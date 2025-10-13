#!/usr/bin/env python3
"""
StreamGuard ML Training Data Generator

Main application that orchestrates the ML training pipeline:
1. Read events from Kafka (batch)
2. Extract behavioral features
3. Detect anomalies using ML
4. Generate labeled training dataset
5. Export to Parquet format

Usage:
    python training_data_generator.py [--config config/spark_config.yaml]
"""

import argparse
import sys
from pathlib import Path
from loguru import logger
from pyspark.sql import SparkSession
from datetime import datetime
import json

# Import pipeline components
from kafka_reader import KafkaEventReader
from feature_extractor import FeatureExtractor
from anomaly_detector import MLAnomalyDetector


class TrainingDataGenerator:
    """Main orchestrator for ML training data generation."""

    def __init__(self, config_path: str = "config/spark_config.yaml"):
        self.config_path = config_path
        self.spark = None
        self.kafka_reader = None
        self.feature_extractor = None
        self.anomaly_detector = None

    def initialize_spark(self):
        """Initialize Spark session with appropriate configuration."""
        logger.info("Initializing Spark session")

        import yaml
        with open(self.config_path, 'r') as f:
            config = yaml.safe_load(f)

        spark_config = config['spark']

        self.spark = SparkSession.builder \
            .appName(spark_config['app_name']) \
            .master(spark_config['master']) \
            .config("spark.executor.memory", spark_config['executor_memory']) \
            .config("spark.driver.memory", spark_config['driver_memory']) \
            .config("spark.sql.shuffle.partitions", spark_config['shuffle_partitions']) \
            .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0") \
            .getOrCreate()

        # Set log level
        self.spark.sparkContext.setLogLevel("WARN")

        logger.info("Spark session initialized: {}", self.spark.version)
        logger.info("Spark UI available at: {}", self.spark.sparkContext.uiWebUrl)

    def initialize_components(self):
        """Initialize pipeline components."""
        logger.info("Initializing pipeline components")

        self.kafka_reader = KafkaEventReader(self.spark, self.config_path)
        self.feature_extractor = FeatureExtractor(self.spark, self.config_path)
        self.anomaly_detector = MLAnomalyDetector(self.spark, self.config_path)

        logger.info("All components initialized")

    def run_pipeline(
        self,
        start_offset: str = "earliest",
        max_events: int = None,
        output_path: str = None
    ):
        """
        Run the complete ML training data generation pipeline.

        Args:
            start_offset: Kafka starting offset
            max_events: Maximum events to process (None = all)
            output_path: Output directory for Parquet files
        """
        logger.info("=" * 80)
        logger.info("Starting ML Training Data Generation Pipeline")
        logger.info("=" * 80)

        pipeline_start = datetime.now()

        # Step 1: Read events from Kafka
        logger.info("\n[Step 1/5] Reading events from Kafka")
        df_events = self.kafka_reader.read_batch(
            start_offset=start_offset,
            max_events=max_events
        )

        # Get summary statistics
        stats = self.kafka_reader.get_summary_statistics(df_events)
        logger.info("Events loaded: {}", stats['total_events'])
        logger.info("Unique users: {}", stats['unique_users'])
        logger.info("Time range: {} to {}", stats['time_range']['min'], stats['time_range']['max'])

        # Step 2: Extract features
        logger.info("\n[Step 2/5] Extracting behavioral features")
        df_features = self.feature_extractor.extract_all_features(df_events)

        # Show sample features
        logger.info("Sample features:")
        df_features.select("user", "total_events", "avg_threat_score", "unique_ips").show(5)

        # Step 3: Get feature columns for ML
        logger.info("\n[Step 3/5] Preparing feature vectors")
        feature_cols = self.feature_extractor.get_feature_vector_columns(df_features)
        logger.info("Selected {} numeric features for ML", len(feature_cols))

        # Step 4: Detect anomalies
        logger.info("\n[Step 4/5] Detecting anomalies with ML")
        df_with_anomalies = self.anomaly_detector.detect_anomalies(df_features, feature_cols)

        # Generate anomaly report
        report = self.anomaly_detector.generate_anomaly_report(df_with_anomalies)
        logger.info("Anomaly detection complete:")
        logger.info("  - Total users: {}", report['total_users'])
        logger.info("  - Anomalous users: {} ({:.2f}%)",
                   report['anomalous_users'],
                   100 * report['anomaly_rate'])

        # Show top anomalies
        logger.info("\nTop anomalous users:")
        for i, anomaly in enumerate(report['top_anomalies'][:5], 1):
            logger.info("  {}. {} (score: {:.3f}, events: {}, threat: {:.3f})",
                       i,
                       anomaly['user'],
                       anomaly['anomaly_score'],
                       anomaly['total_events'],
                       anomaly['avg_threat_score'])

        # Step 5: Export training data
        logger.info("\n[Step 5/5] Exporting training data to Parquet")

        if output_path is None:
            import yaml
            with open(self.config_path, 'r') as f:
                config = yaml.safe_load(f)
            output_path = config['output']['path']

        # Ensure output directory exists
        Path(output_path).mkdir(parents=True, exist_ok=True)

        # Write to Parquet with partitioning
        df_with_anomalies.write \
            .mode("overwrite") \
            .partitionBy("is_anomaly") \
            .parquet(output_path)

        logger.info("Training data exported to: {}", output_path)

        # Also export summary report as JSON
        report_path = Path(output_path) / "anomaly_report.json"
        with open(report_path, 'w') as f:
            json.dump(report, f, indent=2)

        logger.info("Anomaly report saved to: {}", report_path)

        # Pipeline complete
        pipeline_duration = (datetime.now() - pipeline_start).total_seconds()

        logger.info("\n" + "=" * 80)
        logger.info("Pipeline Complete!")
        logger.info("=" * 80)
        logger.info("Total duration: {:.2f} seconds", pipeline_duration)
        logger.info("Events processed: {}", stats['total_events'])
        logger.info("Users analyzed: {}", stats['unique_users'])
        logger.info("Anomalies detected: {} ({:.2f}%)",
                   report['anomalous_users'],
                   100 * report['anomaly_rate'])
        logger.info("Output location: {}", output_path)
        logger.info("=" * 80)

        return df_with_anomalies, report

    def stop(self):
        """Stop Spark session and cleanup."""
        if self.spark:
            logger.info("Stopping Spark session")
            self.spark.stop()


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="StreamGuard ML Training Data Generator"
    )
    parser.add_argument(
        "--config",
        default="config/spark_config.yaml",
        help="Path to configuration file"
    )
    parser.add_argument(
        "--max-events",
        type=int,
        default=None,
        help="Maximum number of events to process"
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output directory for training data"
    )
    parser.add_argument(
        "--start-offset",
        default="earliest",
        help="Kafka starting offset (earliest/latest)"
    )

    args = parser.parse_args()

    # Configure logging
    logger.remove()
    logger.add(
        sys.stderr,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>",
        level="INFO"
    )

    # Run pipeline
    generator = TrainingDataGenerator(config_path=args.config)

    try:
        generator.initialize_spark()
        generator.initialize_components()

        df_result, report = generator.run_pipeline(
            start_offset=args.start_offset,
            max_events=args.max_events,
            output_path=args.output
        )

        logger.success("Training data generation successful!")
        return 0

    except Exception as e:
        logger.exception("Pipeline failed with error: {}", e)
        return 1

    finally:
        generator.stop()


if __name__ == "__main__":
    sys.exit(main())