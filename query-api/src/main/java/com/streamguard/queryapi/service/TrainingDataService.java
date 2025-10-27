package com.streamguard.queryapi.service;

import com.streamguard.queryapi.model.AnomalyReport;
import com.streamguard.queryapi.model.BatchAnomaly;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for reading batch ML training data from Spark ML pipeline output.
 *
 * Reads anomaly detection results from:
 * 1. anomaly_report.json - Summary statistics
 * 2. Parquet files (is_anomaly=1/) - Detailed anomaly records
 *
 * Sprint 12: Lambda Architecture Integration
 */
@Service
public class TrainingDataService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingDataService.class);
    private final String trainingDataPath;
    private final ObjectMapper objectMapper;

    public TrainingDataService() {
        this.trainingDataPath = System.getenv()
            .getOrDefault("TRAINING_DATA_PATH",
                         "../spark-ml-pipeline/output/training_data");
        this.objectMapper = new ObjectMapper();

        logger.info("TrainingDataService initialized with path: {}", trainingDataPath);

        // Log availability on startup
        if (isTrainingDataAvailable()) {
            logger.info("Training data is available");
        } else {
            logger.warn("Training data not found at: {}", trainingDataPath);
        }
    }

    /**
     * Read anomaly report JSON summary.
     *
     * @return AnomalyReport or null if not found
     */
    public AnomalyReport getAnomalyReport() throws IOException {
        File reportFile = new File(trainingDataPath + "/anomaly_report.json");

        if (!reportFile.exists()) {
            logger.warn("Anomaly report not found: {}", reportFile.getPath());
            return null;
        }

        logger.debug("Reading anomaly report from: {}", reportFile.getPath());
        AnomalyReport report = objectMapper.readValue(reportFile, AnomalyReport.class);

        logger.info("Loaded anomaly report: {} users, {} anomalies",
                   report.getTotalUsers(), report.getAnomalousUsers());

        return report;
    }

    /**
     * Read all batch anomalies from Parquet files.
     *
     * Reads from is_anomaly=1/ partition containing all flagged users.
     *
     * @return List of all batch anomalies
     */
    public List<BatchAnomaly> getAnomalies() throws IOException {
        return readAnomaliesFromParquet();
    }

    /**
     * Read anomalies directly from Parquet files.
     *
     * @return List of BatchAnomaly objects from Parquet
     */
    private List<BatchAnomaly> readAnomaliesFromParquet() throws IOException {
        File anomalyDir = new File(trainingDataPath + "/is_anomaly=1");

        if (!anomalyDir.exists() || !anomalyDir.isDirectory()) {
            logger.warn("Anomaly Parquet directory not found: {}", anomalyDir.getPath());
            return new ArrayList<>();
        }

        List<BatchAnomaly> anomalies = new ArrayList<>();
        Configuration conf = new Configuration();

        // Read all Parquet files in is_anomaly=1/ directory
        File[] parquetFiles = anomalyDir.listFiles((dir, name) ->
            name.endsWith(".parquet") && !name.startsWith(".")
        );

        if (parquetFiles == null || parquetFiles.length == 0) {
            logger.warn("No Parquet files found in: {}", anomalyDir.getPath());
            return new ArrayList<>();
        }

        logger.debug("Found {} Parquet files to read", parquetFiles.length);

        // Read each Parquet file
        for (File parquetFile : parquetFiles) {
            try (ParquetReader<GenericRecord> reader = AvroParquetReader
                    .<GenericRecord>builder(new Path(parquetFile.getPath()))
                    .withConf(conf)
                    .build()) {

                GenericRecord record;
                while ((record = reader.read()) != null) {
                    BatchAnomaly anomaly = mapRecordToAnomaly(record);
                    anomalies.add(anomaly);
                }
            } catch (Exception e) {
                logger.error("Failed to read Parquet file: {}", parquetFile.getName(), e);
                // Continue with other files
            }
        }

        logger.info("Read {} anomalies from Parquet files", anomalies.size());
        return anomalies;
    }

    /**
     * Map Avro GenericRecord to BatchAnomaly object.
     *
     * @param record Avro record from Parquet file
     * @return BatchAnomaly object
     */
    private BatchAnomaly mapRecordToAnomaly(GenericRecord record) {
        BatchAnomaly anomaly = new BatchAnomaly();

        // Map fields from Avro record
        anomaly.setUser(getStringField(record, "user"));
        anomaly.setAnomalyScore(getDoubleField(record, "anomaly_score_normalized"));
        anomaly.setTotalEvents(getIntField(record, "total_events"));
        anomaly.setAvgThreatScore(getDoubleField(record, "avg_threat_score"));
        anomaly.setUniqueIps(getIntField(record, "unique_ips"));
        anomaly.setFailedAuthRate(getDoubleField(record, "failed_auth_rate"));

        // Note: is_anomaly is in the partition folder name (is_anomaly=1), not in the Parquet schema
        // All records in is_anomaly=1/ are anomalies, so we set it to 1
        anomaly.setIsAnomaly(1);

        return anomaly;
    }

    /**
     * Safely extract string field from Avro record.
     */
    private String getStringField(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Safely extract double field from Avro record.
     */
    private Double getDoubleField(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse double field '{}': {}", fieldName, value);
            return null;
        }
    }

    /**
     * Safely extract integer field from Avro record.
     */
    private Integer getIntField(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer field '{}': {}", fieldName, value);
            return null;
        }
    }

    /**
     * Get batch anomaly for specific user.
     *
     * @param userId User ID to lookup
     * @return BatchAnomaly or null if not found
     */
    public BatchAnomaly getAnomalyForUser(String userId) throws IOException {
        List<BatchAnomaly> anomalies = getAnomalies();

        BatchAnomaly result = anomalies.stream()
            .filter(a -> userId.equals(a.getUser()))
            .findFirst()
            .orElse(null);

        if (result != null) {
            logger.debug("Found batch anomaly for user: {} (score: {})",
                        userId, result.getAnomalyScore());
        } else {
            logger.debug("No batch anomaly found for user: {}", userId);
        }

        return result;
    }

    /**
     * Check if training data is available.
     *
     * @return true if anomaly_report.json exists
     */
    public boolean isTrainingDataAvailable() {
        File reportFile = new File(trainingDataPath + "/anomaly_report.json");
        boolean available = reportFile.exists();

        logger.debug("Training data available: {}", available);
        return available;
    }

    /**
     * Get training data statistics.
     *
     * @return Summary of available training data
     */
    public String getTrainingDataStats() {
        try {
            AnomalyReport report = getAnomalyReport();

            if (report == null) {
                return "No training data available";
            }

            return String.format("Training data: %d users analyzed, %d anomalies (%.1f%% rate)",
                               report.getTotalUsers(),
                               report.getAnomalousUsers(),
                               report.getAnomalyRate() * 100);

        } catch (IOException e) {
            logger.error("Failed to get training data stats", e);
            return "Error reading training data";
        }
    }
}
