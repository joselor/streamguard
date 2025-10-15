package com.streamguard.queryapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.queryapi.model.AnomalyResult;
import com.streamguard.queryapi.model.SecurityEvent;
import com.streamguard.queryapi.model.ThreatAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Query Service for RocksDB operations
 *
 * Provides read-only access to security events and AI analyses
 */
@Slf4j
@Service
public class QueryService {

    private final RocksDB rocksDB;
    private final ColumnFamilyHandle defaultColumnFamily;
    private final ColumnFamilyHandle aiAnalysisColumnFamily;
    private final ColumnFamilyHandle anomaliesColumnFamily;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public QueryService(
            RocksDB rocksDB,
            ColumnFamilyHandle defaultColumnFamily,
            @Nullable @Qualifier("aiAnalysisColumnFamily") ColumnFamilyHandle aiAnalysisColumnFamily,
            @Nullable @Qualifier("anomaliesColumnFamily") ColumnFamilyHandle anomaliesColumnFamily) {
        this.rocksDB = rocksDB;
        this.defaultColumnFamily = defaultColumnFamily;
        this.aiAnalysisColumnFamily = aiAnalysisColumnFamily;
        this.anomaliesColumnFamily = anomaliesColumnFamily;
    }

    /**
     * Get latest events (up to limit)
     */
    public List<SecurityEvent> getLatestEvents(int limit) {
        List<SecurityEvent> events = new ArrayList<>();

        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToLast();

            if (!iterator.isValid()) {
                log.warn("No events found in database - database may be empty or stream-processor not running");
                return events;
            }

            int count = 0;
            while (iterator.isValid() && count < limit) {
                try {
                    String json = new String(iterator.value());
                    SecurityEvent event = objectMapper.readValue(json, SecurityEvent.class);
                    events.add(event);
                    count++;
                } catch (Exception e) {
                    log.error("Error parsing event JSON", e);
                }
                iterator.prev();
            }

            log.info("Retrieved {} events (limit: {})", events.size(), limit);
        }

        return events;
    }

    /**
     * Get event by ID
     */
    public SecurityEvent getEventById(String eventId) {
        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid()) {
                String key = new String(iterator.key());
                if (key.endsWith(":" + eventId)) {
                    String json = new String(iterator.value());
                    return objectMapper.readValue(json, SecurityEvent.class);
                }
                iterator.next();
            }
        } catch (Exception e) {
            log.error("Error getting event by ID: {}", eventId, e);
        }

        return null;
    }

    /**
     * Get threat analysis by event ID
     */
    public ThreatAnalysis getAnalysisByEventId(String eventId) {
        if (aiAnalysisColumnFamily == null) {
            log.warn("AI analysis column family not available");
            return null;
        }

        try {
            byte[] value = rocksDB.get(aiAnalysisColumnFamily, eventId.getBytes());
            if (value != null) {
                return objectMapper.readValue(value, ThreatAnalysis.class);
            }
        } catch (RocksDBException e) {
            log.error("RocksDB error getting analysis for event: {}", eventId, e);
        } catch (Exception e) {
            log.error("Error parsing analysis JSON for event: {}", eventId, e);
        }

        return null;
    }

    /**
     * Get all AI analyses (up to limit)
     */
    public List<ThreatAnalysis> getLatestAnalyses(int limit) {
        List<ThreatAnalysis> analyses = new ArrayList<>();

        if (aiAnalysisColumnFamily == null) {
            log.warn("AI analysis column family not available - this means:");
            log.warn("  1. Database was created before AI analysis feature was added, OR");
            log.warn("  2. Stream-processor has not written any AI analyses yet, OR");
            log.warn("  3. OPENAI_API_KEY is not configured in stream-processor");
            log.warn("  Solution: Ensure OPENAI_API_KEY is set and restart stream-processor");
            return analyses;
        }

        try (RocksIterator iterator = rocksDB.newIterator(aiAnalysisColumnFamily)) {
            iterator.seekToLast();

            if (!iterator.isValid()) {
                log.warn("AI analysis column family exists but is empty");
                log.warn("  Check if stream-processor is generating AI analyses");
                log.warn("  Verify OPENAI_API_KEY is valid and API calls are succeeding");
                return analyses;
            }

            int count = 0;
            while (iterator.isValid() && count < limit) {
                try {
                    String json = new String(iterator.value());
                    ThreatAnalysis analysis = objectMapper.readValue(json, ThreatAnalysis.class);
                    analyses.add(analysis);
                    count++;
                } catch (Exception e) {
                    log.error("Error parsing analysis JSON", e);
                }
                iterator.prev();
            }

            log.info("Retrieved {} AI analyses (limit: {})", analyses.size(), limit);
        }

        return analyses;
    }

    /**
     * Get analyses by severity
     */
    public List<ThreatAnalysis> getAnalysesBySeverity(String severity, int limit) {
        List<ThreatAnalysis> analyses = new ArrayList<>();

        if (aiAnalysisColumnFamily == null) {
            log.warn("AI analysis column family not available");
            return analyses;
        }

        try (RocksIterator iterator = rocksDB.newIterator(aiAnalysisColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid() && analyses.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    ThreatAnalysis analysis = objectMapper.readValue(json, ThreatAnalysis.class);

                    if (severity.equalsIgnoreCase(analysis.getSeverity())) {
                        analyses.add(analysis);
                    }
                } catch (Exception e) {
                    log.error("Error parsing analysis JSON", e);
                }
                iterator.next();
            }
        }

        return analyses;
    }

    /**
     * Get total event count
     */
    public long getEventCount() {
        long count = 0;
        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                count++;
                iterator.next();
            }
        }
        return count;
    }

    /**
     * Get total analysis count
     */
    public long getAnalysisCount() {
        if (aiAnalysisColumnFamily == null) {
            return 0;
        }

        long count = 0;
        try (RocksIterator iterator = rocksDB.newIterator(aiAnalysisColumnFamily)) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                count++;
                iterator.next();
            }
        }
        return count;
    }

    /**
     * Get events with threat score above threshold
     */
    public List<SecurityEvent> getEventsByThreatScore(double minScore, int limit) {
        List<SecurityEvent> events = new ArrayList<>();

        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToLast();

            while (iterator.isValid() && events.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    SecurityEvent event = objectMapper.readValue(json, SecurityEvent.class);

                    if (event.getThreatScore() != null && event.getThreatScore() >= minScore) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    log.error("Error parsing event JSON", e);
                }
                iterator.prev();
            }
        }

        return events;
    }

    /**
     * Get summary statistics
     */
    public StatsSummary getStatsSummary() {
        StatsSummary summary = new StatsSummary();

        long totalEvents = 0;
        long highThreatEvents = 0;
        double totalThreatScore = 0.0;

        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid()) {
                try {
                    String json = new String(iterator.value());
                    SecurityEvent event = objectMapper.readValue(json, SecurityEvent.class);

                    totalEvents++;
                    if (event.getThreatScore() != null) {
                        totalThreatScore += event.getThreatScore();
                        if (event.getThreatScore() >= 0.7) {
                            highThreatEvents++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error parsing event JSON", e);
                }
                iterator.next();
            }
        }

        summary.setTotalEvents(totalEvents);
        summary.setHighThreatEvents(highThreatEvents);
        summary.setAverageThreatScore(totalEvents > 0 ? totalThreatScore / totalEvents : 0.0);
        summary.setTotalAnalyses(getAnalysisCount());
        summary.setTotalAnomalies(getAnomalyCount());

        return summary;
    }

    /**
     * Get latest anomalies (up to limit)
     */
    public List<AnomalyResult> getLatestAnomalies(int limit) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (anomaliesColumnFamily == null) {
            log.warn("Anomalies column family not available - this means:");
            log.warn("  1. Database was created before anomaly detection feature was added, OR");
            log.warn("  2. Stream-processor has not detected any anomalies yet");
            log.warn("  Solution: Ensure stream-processor is running and processing events");
            return anomalies;
        }

        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToLast();

            if (!iterator.isValid()) {
                log.warn("Anomalies column family exists but is empty");
                log.warn("  Possible reasons:");
                log.warn("    - Not enough events processed to establish baseline (need {} events)", 100);
                log.warn("    - No anomalous behavior detected yet (threshold: 0.5)");
                log.warn("    - Anomaly detection may be disabled in stream-processor");
                log.warn("  Tip: Lower the threshold or wait for more events to be processed");
                return anomalies;
            }

            int count = 0;
            while (iterator.isValid() && count < limit) {
                try {
                    String json = new String(iterator.value());
                    AnomalyResult anomaly = objectMapper.readValue(json, AnomalyResult.class);
                    anomalies.add(anomaly);
                    count++;
                } catch (Exception e) {
                    log.error("Error parsing anomaly JSON", e);
                }
                iterator.prev();
            }

            log.info("Retrieved {} anomalies (limit: {})", anomalies.size(), limit);
        }

        return anomalies;
    }

    /**
     * Get anomaly by event ID
     */
    public AnomalyResult getAnomalyByEventId(String eventId) {
        if (anomaliesColumnFamily == null) {
            log.warn("anomalies column family not available");
            return null;
        }

        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid()) {
                String key = new String(iterator.key());
                if (key.endsWith(":" + eventId)) {
                    String json = new String(iterator.value());
                    return objectMapper.readValue(json, AnomalyResult.class);
                }
                iterator.next();
            }
        } catch (Exception e) {
            log.error("Error getting anomaly by event ID: {}", eventId, e);
        }

        return null;
    }

    /**
     * Get anomalies by user
     */
    public List<AnomalyResult> getAnomaliesByUser(String user, int limit) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (anomaliesColumnFamily == null) {
            log.warn("anomalies column family not available");
            return anomalies;
        }

        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid() && anomalies.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    AnomalyResult anomaly = objectMapper.readValue(json, AnomalyResult.class);

                    if (user.equals(anomaly.getUser())) {
                        anomalies.add(anomaly);
                    }
                } catch (Exception e) {
                    log.error("Error parsing anomaly JSON", e);
                }
                iterator.next();
            }
        }

        return anomalies;
    }

    /**
     * Get high-score anomalies (above threshold)
     */
    public List<AnomalyResult> getHighScoreAnomalies(double threshold, int limit) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (anomaliesColumnFamily == null) {
            log.warn("anomalies column family not available");
            return anomalies;
        }

        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToLast();

            while (iterator.isValid() && anomalies.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    AnomalyResult anomaly = objectMapper.readValue(json, AnomalyResult.class);

                    if (anomaly.getAnomalyScore() != null && anomaly.getAnomalyScore() >= threshold) {
                        anomalies.add(anomaly);
                    }
                } catch (Exception e) {
                    log.error("Error parsing anomaly JSON", e);
                }
                iterator.prev();
            }
        }

        return anomalies;
    }

    /**
     * Get anomalies by time range
     */
    public List<AnomalyResult> getAnomaliesByTimeRange(long startTime, long endTime, int limit) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (anomaliesColumnFamily == null) {
            log.warn("anomalies column family not available");
            return anomalies;
        }

        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToFirst();

            while (iterator.isValid() && anomalies.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    AnomalyResult anomaly = objectMapper.readValue(json, AnomalyResult.class);

                    if (anomaly.getTimestamp() != null
                            && anomaly.getTimestamp() >= startTime
                            && anomaly.getTimestamp() <= endTime) {
                        anomalies.add(anomaly);
                    }
                } catch (Exception e) {
                    log.error("Error parsing anomaly JSON", e);
                }
                iterator.next();
            }
        }

        return anomalies;
    }

    /**
     * Get total anomaly count
     */
    public long getAnomalyCount() {
        if (anomaliesColumnFamily == null) {
            return 0;
        }

        long count = 0;
        try (RocksIterator iterator = rocksDB.newIterator(anomaliesColumnFamily)) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                count++;
                iterator.next();
            }
        }
        return count;
    }

    /**
     * Statistics summary model
     */
    @lombok.Data
    public static class StatsSummary {
        private long totalEvents;
        private long highThreatEvents;
        private double averageThreatScore;
        private long totalAnalyses;
        private long totalAnomalies;
    }
}
