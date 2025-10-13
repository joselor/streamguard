"""
Feature Extractor for StreamGuard ML Pipeline

Extracts behavioral features from security events for ML training.
"""

from pyspark.sql import DataFrame, SparkSession
from pyspark.sql import functions as F
from pyspark.sql.window import Window
from typing import List, Dict
from loguru import logger
import yaml


class FeatureExtractor:
    """Extract ML features from security events."""

    def __init__(self, spark: SparkSession, config_path: str = "config/spark_config.yaml"):
        self.spark = spark

        # Load configuration
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.feature_config = self.config['features']
        logger.info("FeatureExtractor initialized with config from {}", config_path)

    def extract_user_features(self, df: DataFrame) -> DataFrame:
        """
        Extract user-level behavioral features.

        Features:
        - Total events per user
        - Unique IPs per user
        - Unique event types per user
        - Average threat score
        - Failed authentication rate
        - Unusual hour activity rate (0-6 AM)
        """
        logger.info("Extracting user-level features")

        user_features = df.groupBy("user").agg(
            # Event counts
            F.count("*").alias("total_events"),
            F.countDistinct("source_ip").alias("unique_ips"),
            F.countDistinct("event_type").alias("unique_event_types"),

            # Threat indicators
            F.avg("threat_score").alias("avg_threat_score"),
            F.max("threat_score").alias("max_threat_score"),

            # Failed attempts
            F.sum(
                F.when(F.col("status") == "failed", 1).otherwise(0)
            ).alias("failed_events"),

            # Unusual hours (0-6 AM)
            F.sum(
                F.when(F.hour("timestamp").between(0, 6), 1).otherwise(0)
            ).alias("unusual_hour_events"),

            # Geographic diversity
            F.countDistinct("location").alias("unique_locations"),

            # Time range
            F.min("timestamp").alias("first_seen"),
            F.max("timestamp").alias("last_seen")
        )

        # Calculate derived features
        user_features = user_features.withColumn(
            "failed_auth_rate",
            F.col("failed_events") / F.col("total_events")
        ).withColumn(
            "unusual_hour_rate",
            F.col("unusual_hour_events") / F.col("total_events")
        ).withColumn(
            "ip_diversity_score",
            F.col("unique_ips") / F.col("total_events")
        ).withColumn(
            "activity_days",
            F.datediff(F.col("last_seen"), F.col("first_seen")) + 1
        ).withColumn(
            "events_per_day",
            F.col("total_events") / F.col("activity_days")
        )

        logger.info("Extracted {} user feature columns", len(user_features.columns))
        return user_features

    def extract_event_type_features(self, df: DataFrame) -> DataFrame:
        """Extract event type distribution features per user."""
        logger.info("Extracting event type distribution features")

        # Pivot to get event type counts per user
        event_type_features = df.groupBy("user", "event_type").count() \
            .groupBy("user").pivot("event_type").sum("count").na.fill(0)

        return event_type_features

    def extract_temporal_features(self, df: DataFrame) -> DataFrame:
        """Extract time-based features using sliding windows."""
        logger.info("Extracting temporal features")

        # Add time-based columns
        df_with_time = df.withColumn("hour", F.hour("timestamp")) \
            .withColumn("day_of_week", F.dayofweek("timestamp")) \
            .withColumn("is_weekend", F.dayofweek("timestamp").isin([1, 7]).cast("int"))

        # Calculate hourly distribution per user
        hourly_features = df_with_time.groupBy("user", "hour").count() \
            .groupBy("user").agg(
                F.stddev("count").alias("hourly_stddev"),
                F.variance("count").alias("hourly_variance")
            )

        return hourly_features

    def extract_ip_features(self, df: DataFrame) -> DataFrame:
        """Extract IP-based behavioral features."""
        logger.info("Extracting IP-based features")

        # Window for IP frequency per user
        window_spec = Window.partitionBy("user", "source_ip")

        df_with_ip_freq = df.withColumn(
            "ip_frequency",
            F.count("*").over(window_spec)
        )

        # Aggregate IP features per user
        ip_features = df_with_ip_freq.groupBy("user").agg(
            # IP usage patterns
            F.avg("ip_frequency").alias("avg_ip_frequency"),
            F.stddev("ip_frequency").alias("ip_frequency_stddev"),

            # Rare IP detection (IPs used only once)
            F.sum(
                F.when(F.col("ip_frequency") == 1, 1).otherwise(0)
            ).alias("rare_ip_count")
        ).withColumn(
            "rare_ip_ratio",
            F.col("rare_ip_count") / F.col("avg_ip_frequency")
        )

        return ip_features

    def extract_sequence_features(self, df: DataFrame) -> DataFrame:
        """Extract sequential pattern features (e.g., burst detection)."""
        logger.info("Extracting sequence features")

        # Sort by user and timestamp
        window_spec = Window.partitionBy("user").orderBy("timestamp")

        # Calculate time difference between consecutive events
        df_with_intervals = df.withColumn(
            "time_since_last_event",
            F.unix_timestamp("timestamp") - F.lag("timestamp").over(window_spec).cast("long")
        )

        # Aggregate sequence features
        sequence_features = df_with_intervals.groupBy("user").agg(
            F.avg("time_since_last_event").alias("avg_event_interval"),
            F.stddev("time_since_last_event").alias("event_interval_stddev"),
            F.min("time_since_last_event").alias("min_event_interval"),

            # Burst detection (events within 1 second)
            F.sum(
                F.when(F.col("time_since_last_event") < 1, 1).otherwise(0)
            ).alias("burst_events")
        )

        return sequence_features

    def extract_all_features(self, df: DataFrame) -> DataFrame:
        """
        Extract all features and join them into a single feature DataFrame.

        Args:
            df: Input DataFrame with security events

        Returns:
            DataFrame with all extracted features per user
        """
        logger.info("Extracting all features from {} events", df.count())

        # Extract different feature groups
        user_features = self.extract_user_features(df)
        event_type_features = self.extract_event_type_features(df)
        temporal_features = self.extract_temporal_features(df)
        ip_features = self.extract_ip_features(df)
        sequence_features = self.extract_sequence_features(df)

        # Join all features
        features = user_features \
            .join(event_type_features, on="user", how="left") \
            .join(temporal_features, on="user", how="left") \
            .join(ip_features, on="user", how="left") \
            .join(sequence_features, on="user", how="left")

        # Fill nulls with 0
        features = features.na.fill(0)

        logger.info("Total features extracted: {} columns for {} users",
                   len(features.columns), features.count())

        return features

    def get_feature_vector_columns(self, df: DataFrame) -> List[str]:
        """Get list of numeric feature columns suitable for ML."""
        # Exclude non-feature columns
        exclude_cols = {"user", "first_seen", "last_seen"}

        feature_cols = [
            col for col in df.columns
            if col not in exclude_cols and df.schema[col].dataType.simpleString() in ['double', 'bigint', 'int']
        ]

        logger.info("Feature vector columns: {}", feature_cols)
        return feature_cols
