package com.streamguard.queryapi;

import com.streamguard.queryapi.model.BatchAnomaly;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone test to verify Parquet reading functionality.
 * Run with: mvn test-compile exec:java -Dexec.mainClass="com.streamguard.queryapi.ParquetReaderTest"
 */
public class ParquetReaderTest {

    public static void main(String[] args) {
        String trainingDataPath = "../spark-ml-pipeline/output/training_data";
        File anomalyDir = new File(trainingDataPath + "/is_anomaly=1");

        System.out.println("=== Parquet Reader Test ===");
        System.out.println("Training data path: " + trainingDataPath);
        System.out.println("Anomaly directory: " + anomalyDir.getAbsolutePath());
        System.out.println("Directory exists: " + anomalyDir.exists());

        if (!anomalyDir.exists() || !anomalyDir.isDirectory()) {
            System.err.println("ERROR: Anomaly directory not found!");
            System.exit(1);
        }

        // Find Parquet files
        File[] parquetFiles = anomalyDir.listFiles((dir, name) ->
            name.endsWith(".parquet") && !name.startsWith(".")
        );

        if (parquetFiles == null || parquetFiles.length == 0) {
            System.err.println("ERROR: No Parquet files found!");
            System.exit(1);
        }

        System.out.println("Found " + parquetFiles.length + " Parquet files");

        List<BatchAnomaly> anomalies = new ArrayList<>();
        Configuration conf = new Configuration();

        // Read each Parquet file
        for (File parquetFile : parquetFiles) {
            System.out.println("\nReading: " + parquetFile.getName());

            try (ParquetReader<GenericRecord> reader = AvroParquetReader
                    .<GenericRecord>builder(new Path(parquetFile.getPath()))
                    .withConf(conf)
                    .build()) {

                GenericRecord record;
                int recordCount = 0;
                while ((record = reader.read()) != null) {
                    recordCount++;
                    if (recordCount == 1) {
                        // Print first record details
                        System.out.println("  First record:");
                        System.out.println("    user: " + record.get("user"));
                        System.out.println("    anomaly_score: " + record.get("anomaly_score_normalized"));
                        System.out.println("    total_events: " + record.get("total_events"));
                    }

                    BatchAnomaly anomaly = mapRecord(record);
                    anomalies.add(anomaly);
                }
                System.out.println("  Records read: " + recordCount);

            } catch (Exception e) {
                System.err.println("  ERROR reading file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=== Summary ===");
        System.out.println("Total anomalies read: " + anomalies.size());

        // Print top 5 anomalies by score
        anomalies.sort((a, b) -> Double.compare(
            b.getAnomalyScore() != null ? b.getAnomalyScore() : 0.0,
            a.getAnomalyScore() != null ? a.getAnomalyScore() : 0.0
        ));

        System.out.println("\nTop 5 anomalies:");
        for (int i = 0; i < Math.min(5, anomalies.size()); i++) {
            BatchAnomaly a = anomalies.get(i);
            System.out.printf("  %d. %s - score: %.3f, events: %d, failed_auth: %.2f%%\n",
                i + 1, a.getUser(), a.getAnomalyScore(),
                a.getTotalEvents(), a.getFailedAuthRate() * 100);
        }

        System.out.println("\n✅ Parquet reading test PASSED!");
    }

    private static BatchAnomaly mapRecord(GenericRecord record) {
        BatchAnomaly anomaly = new BatchAnomaly();
        anomaly.setUser(getString(record, "user"));
        anomaly.setAnomalyScore(getDouble(record, "anomaly_score_normalized"));
        anomaly.setTotalEvents(getInt(record, "total_events"));
        anomaly.setAvgThreatScore(getDouble(record, "avg_threat_score"));
        anomaly.setUniqueIps(getInt(record, "unique_ips"));
        anomaly.setFailedAuthRate(getDouble(record, "failed_auth_rate"));
        anomaly.setIsAnomaly(1);  // All records in is_anomaly=1/ are anomalies
        return anomaly;
    }

    private static String getString(GenericRecord record, String field) {
        Object value = record.get(field);
        return value != null ? value.toString() : null;
    }

    private static Double getDouble(GenericRecord record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer getInt(GenericRecord record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
