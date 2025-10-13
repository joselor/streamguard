"""
Kafka Reader for StreamGuard Spark ML Pipeline

Reads historical security events from Kafka for batch processing.
"""

from pyspark.sql import SparkSession, DataFrame
from pyspark.sql import functions as F
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, LongType, TimestampType
from loguru import logger
from typing import Optional
import yaml


class KafkaEventReader:
    """Read security events from Kafka into Spark DataFrames."""

    # Event schema matching StreamGuard event format
    EVENT_SCHEMA = StructType([
        StructField("event_id", StringType(), True),
        StructField("timestamp", TimestampType(), True),
        StructField("event_type", StringType(), True),
        StructField("user", StringType(), True),
        StructField("source_ip", StringType(), True),
        StructField("destination_ip", StringType(), True),
        StructField("location", StringType(), True),
        StructField("status", StringType(), True),
        StructField("threat_score", DoubleType(), True),
        StructField("details", StringType(), True),
    ])

    def __init__(self, spark: SparkSession, config_path: str = "config/spark_config.yaml"):
        self.spark = spark

        # Load configuration
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.kafka_config = self.config['kafka']
        logger.info("KafkaEventReader initialized with broker: {}",
                   self.kafka_config['bootstrap_servers'])

    def read_batch(
        self,
        start_offset: str = "earliest",
        end_offset: Optional[str] = None,
        max_events: Optional[int] = None
    ) -> DataFrame:
        """
        Read a batch of events from Kafka.

        Args:
            start_offset: Starting offset ("earliest", "latest", or specific offset)
            end_offset: Ending offset (optional)
            max_events: Maximum number of events to read (optional)

        Returns:
            DataFrame with parsed security events
        """
        logger.info("Reading batch from Kafka topic: {}", self.kafka_config['topic'])

        # Build Kafka read options
        kafka_options = {
            "kafka.bootstrap.servers": self.kafka_config['bootstrap_servers'],
            "subscribe": self.kafka_config['topic'],
            "startingOffsets": start_offset,
        }

        if end_offset:
            kafka_options["endingOffsets"] = end_offset

        # Read from Kafka
        df = self.spark.read \
            .format("kafka") \
            .options(**kafka_options) \
            .load()

        logger.info("Read {} Kafka records", df.count())

        # Parse JSON value
        df_parsed = df.select(
            F.col("key").cast("string").alias("key"),
            F.col("value").cast("string").alias("value_string"),
            F.col("topic"),
            F.col("partition"),
            F.col("offset"),
            F.col("timestamp").alias("kafka_timestamp")
        )

        # Extract JSON fields
        df_events = df_parsed.select(
            F.get_json_object("value_string", "$.event_id").alias("event_id"),
            F.from_unixtime(
                F.get_json_object("value_string", "$.timestamp").cast("long") / 1000
            ).cast("timestamp").alias("timestamp"),
            F.get_json_object("value_string", "$.event_type").alias("event_type"),
            F.get_json_object("value_string", "$.user").alias("user"),
            F.get_json_object("value_string", "$.source_ip").alias("source_ip"),
            F.get_json_object("value_string", "$.destination_ip").alias("destination_ip"),
            F.get_json_object("value_string", "$.location").alias("location"),
            F.get_json_object("value_string", "$.status").alias("status"),
            F.get_json_object("value_string", "$.threat_score").cast("double").alias("threat_score"),
            F.get_json_object("value_string", "$.details").alias("details"),
            F.col("partition"),
            F.col("offset")
        )

        # Filter out invalid records
        df_valid = df_events.filter(F.col("event_id").isNotNull())

        # Apply max events limit if specified
        if max_events:
            df_valid = df_valid.limit(max_events)

        event_count = df_valid.count()
        logger.info("Parsed {} valid security events", event_count)

        return df_valid

    def read_streaming(self, checkpoint_location: str = "./checkpoints") -> DataFrame:
        """
        Read events as a streaming DataFrame (for micro-batch processing).

        Args:
            checkpoint_location: Directory for streaming checkpoints

        Returns:
            Streaming DataFrame with parsed security events
        """
        logger.info("Starting streaming read from Kafka topic: {}",
                   self.kafka_config['topic'])

        # Read streaming data from Kafka
        df = self.spark.readStream \
            .format("kafka") \
            .option("kafka.bootstrap.servers", self.kafka_config['bootstrap_servers']) \
            .option("subscribe", self.kafka_config['topic']) \
            .option("startingOffsets", self.kafka_config['auto_offset_reset']) \
            .load()

        # Parse JSON value (same as batch)
        df_events = df.select(
            F.get_json_object(F.col("value").cast("string"), "$.event_id").alias("event_id"),
            F.from_unixtime(
                F.get_json_object(F.col("value").cast("string"), "$.timestamp").cast("long") / 1000
            ).cast("timestamp").alias("timestamp"),
            F.get_json_object(F.col("value").cast("string"), "$.event_type").alias("event_type"),
            F.get_json_object(F.col("value").cast("string"), "$.user").alias("user"),
            F.get_json_object(F.col("value").cast("string"), "$.source_ip").alias("source_ip"),
            F.get_json_object(F.col("value").cast("string"), "$.destination_ip").alias("destination_ip"),
            F.get_json_object(F.col("value").cast("string"), "$.location").alias("location"),
            F.get_json_object(F.col("value").cast("string"), "$.status").alias("status"),
            F.get_json_object(F.col("value").cast("string"), "$.threat_score").cast("double").alias("threat_score"),
            F.get_json_object(F.col("value").cast("string"), "$.details").alias("details")
        ).filter(F.col("event_id").isNotNull())

        logger.info("Streaming DataFrame created")
        return df_events

    def read_time_range(
        self,
        start_time: str,
        end_time: Optional[str] = None
    ) -> DataFrame:
        """
        Read events within a specific time range.

        Args:
            start_time: Start timestamp (ISO format: "2024-01-01T00:00:00")
            end_time: End timestamp (optional)

        Returns:
            DataFrame with events in the specified time range
        """
        logger.info("Reading events from {} to {}", start_time, end_time or "now")

        # Read all events
        df = self.read_batch(start_offset="earliest")

        # Filter by timestamp
        df_filtered = df.filter(F.col("timestamp") >= start_time)

        if end_time:
            df_filtered = df_filtered.filter(F.col("timestamp") <= end_time)

        event_count = df_filtered.count()
        logger.info("Found {} events in time range", event_count)

        return df_filtered

    def get_summary_statistics(self, df: DataFrame) -> dict:
        """Get summary statistics for the loaded events."""
        stats = {
            "total_events": df.count(),
            "unique_users": df.select("user").distinct().count(),
            "unique_ips": df.select("source_ip").distinct().count(),
            "event_type_distribution": df.groupBy("event_type").count().collect(),
            "time_range": {
                "min": df.agg(F.min("timestamp")).collect()[0][0],
                "max": df.agg(F.max("timestamp")).collect()[0][0]
            },
            "avg_threat_score": df.agg(F.avg("threat_score")).collect()[0][0]
        }

        logger.info("Summary statistics: {} events, {} users, {} IPs",
                   stats["total_events"], stats["unique_users"], stats["unique_ips"])

        return stats
