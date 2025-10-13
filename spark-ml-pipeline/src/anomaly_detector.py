"""
ML-based Anomaly Detector for StreamGuard

Uses Isolation Forest and K-Means clustering for anomaly detection.
"""

from pyspark.sql import DataFrame, SparkSession
from pyspark.sql import functions as F
from pyspark.ml.feature import VectorAssembler, StandardScaler
from pyspark.ml.clustering import KMeans
from sklearn.ensemble import IsolationForest
import pandas as pd
import numpy as np
from loguru import logger
from typing import List, Tuple
import yaml


class MLAnomalyDetector:
    """Machine Learning-based anomaly detection."""

    def __init__(self, spark: SparkSession, config_path: str = "config/spark_config.yaml"):
        self.spark = spark

        # Load configuration
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.anomaly_config = self.config['anomaly_detection']
        self.algorithm = self.anomaly_config['algorithm']

        logger.info("MLAnomalyDetector initialized with algorithm: {}", self.algorithm)

    def prepare_features(self, df: DataFrame, feature_cols: List[str]) -> Tuple[DataFrame, str]:
        """
        Prepare feature vectors for ML algorithms.

        Args:
            df: Input DataFrame with extracted features
            feature_cols: List of feature column names

        Returns:
            Tuple of (DataFrame with feature vectors, feature column name)
        """
        logger.info("Preparing feature vectors from {} columns", len(feature_cols))

        # Assemble features into a single vector column
        assembler = VectorAssembler(
            inputCols=feature_cols,
            outputCol="features_raw",
            handleInvalid="skip"
        )

        df_assembled = assembler.transform(df)

        # Standardize features (mean=0, std=1)
        scaler = StandardScaler(
            inputCol="features_raw",
            outputCol="features",
            withStd=True,
            withMean=True
        )

        scaler_model = scaler.fit(df_assembled)
        df_scaled = scaler_model.transform(df_assembled)

        logger.info("Features prepared and standardized")
        return df_scaled, "features"

    def detect_with_isolation_forest(
        self,
        df: DataFrame,
        feature_cols: List[str]
    ) -> DataFrame:
        """
        Detect anomalies using Isolation Forest (scikit-learn).

        Note: This converts to Pandas for sklearn, suitable for smaller datasets.
        For large-scale, consider distributed alternatives.

        Args:
            df: DataFrame with extracted features
            feature_cols: List of feature column names

        Returns:
            DataFrame with anomaly predictions
        """
        logger.info("Running Isolation Forest anomaly detection")

        # Prepare features
        df_prepared, feature_col = self.prepare_features(df, feature_cols)

        # Convert to Pandas (for sklearn)
        # NOTE: This is fine for training data generation, but not for large-scale inference
        df_pandas = df_prepared.select("user", *feature_cols).toPandas()

        # Extract feature matrix
        X = df_pandas[feature_cols].values

        # Train Isolation Forest
        clf = IsolationForest(
            contamination=self.anomaly_config['contamination'],
            n_estimators=self.anomaly_config['n_estimators'],
            random_state=self.anomaly_config['random_state'],
            n_jobs=-1
        )

        logger.info("Training Isolation Forest with contamination={}, n_estimators={}",
                   self.anomaly_config['contamination'],
                   self.anomaly_config['n_estimators'])

        # Fit and predict
        predictions = clf.fit_predict(X)
        anomaly_scores = clf.score_samples(X)

        # Add predictions to DataFrame
        # -1 = anomaly, 1 = normal
        df_pandas['is_anomaly'] = (predictions == -1).astype(int)
        df_pandas['anomaly_score'] = -anomaly_scores  # Invert so higher = more anomalous

        # Normalize anomaly scores to 0-1 range
        min_score = df_pandas['anomaly_score'].min()
        max_score = df_pandas['anomaly_score'].max()
        df_pandas['anomaly_score_normalized'] = (
            (df_pandas['anomaly_score'] - min_score) / (max_score - min_score)
        )

        # Convert back to Spark DataFrame
        df_result = self.spark.createDataFrame(df_pandas)

        anomaly_count = df_pandas['is_anomaly'].sum()
        logger.info("Detected {} anomalies ({:.2f}% of users)",
                   anomaly_count,
                   100 * anomaly_count / len(df_pandas))

        return df_result

    def detect_with_kmeans(
        self,
        df: DataFrame,
        feature_cols: List[str],
        k: int = 5
    ) -> DataFrame:
        """
        Detect anomalies using K-Means clustering (Spark MLlib).

        Users far from cluster centers are considered anomalous.

        Args:
            df: DataFrame with extracted features
            feature_cols: List of feature column names
            k: Number of clusters

        Returns:
            DataFrame with cluster assignments and distance to centroid
        """
        logger.info("Running K-Means clustering with k={}", k)

        # Prepare features
        df_prepared, feature_col = self.prepare_features(df, feature_cols)

        # Train K-Means
        kmeans = KMeans(k=k, seed=self.anomaly_config['random_state'])
        model = kmeans.fit(df_prepared)

        # Make predictions
        df_predictions = model.transform(df_prepared)

        # Calculate distance to nearest centroid (anomaly score)
        # Users far from their cluster center are anomalous
        centers = model.clusterCenters()

        def distance_to_centroid(features, prediction):
            """Calculate Euclidean distance to assigned cluster center."""
            center = centers[prediction]
            return float(np.linalg.norm(features.toArray() - center))

        distance_udf = F.udf(distance_to_centroid, F.DoubleType())

        df_with_distance = df_predictions.withColumn(
            "distance_to_center",
            distance_udf(F.col("features"), F.col("prediction"))
        )

        # Normalize distances to 0-1 range (anomaly score)
        max_distance = df_with_distance.agg(F.max("distance_to_center")).collect()[0][0]
        min_distance = df_with_distance.agg(F.min("distance_to_center")).collect()[0][0]

        df_result = df_with_distance.withColumn(
            "anomaly_score_normalized",
            (F.col("distance_to_center") - min_distance) / (max_distance - min_distance)
        ).withColumn(
            "is_anomaly",
            (F.col("anomaly_score_normalized") > 0.8).cast("int")  # Top 20% are anomalies
        )

        anomaly_count = df_result.filter(F.col("is_anomaly") == 1).count()
        logger.info("Detected {} anomalies using K-Means", anomaly_count)

        return df_result

    def detect_anomalies(self, df: DataFrame, feature_cols: List[str]) -> DataFrame:
        """
        Detect anomalies using the configured algorithm.

        Args:
            df: DataFrame with extracted features
            feature_cols: List of feature column names

        Returns:
            DataFrame with anomaly predictions
        """
        if self.algorithm == "isolation_forest":
            return self.detect_with_isolation_forest(df, feature_cols)
        elif self.algorithm == "kmeans":
            return self.detect_with_kmeans(df, feature_cols)
        else:
            raise ValueError(f"Unknown algorithm: {self.algorithm}")

    def generate_anomaly_report(self, df: DataFrame) -> dict:
        """Generate summary report of anomaly detection results."""
        total_users = df.count()
        anomaly_users = df.filter(F.col("is_anomaly") == 1).count()

        # Top anomalous users
        top_anomalies = df.filter(F.col("is_anomaly") == 1) \
            .orderBy(F.desc("anomaly_score_normalized")) \
            .select("user", "anomaly_score_normalized", "total_events", "avg_threat_score") \
            .limit(10) \
            .collect()

        report = {
            "total_users": total_users,
            "anomalous_users": anomaly_users,
            "anomaly_rate": anomaly_users / total_users if total_users > 0 else 0,
            "top_anomalies": [
                {
                    "user": row["user"],
                    "anomaly_score": float(row["anomaly_score_normalized"]),
                    "total_events": int(row["total_events"]),
                    "avg_threat_score": float(row["avg_threat_score"]) if row["avg_threat_score"] else 0
                }
                for row in top_anomalies
            ]
        }

        logger.info("Anomaly Report: {}/{} users anomalous ({:.2f}%)",
                   anomaly_users, total_users, 100 * report["anomaly_rate"])

        return report
