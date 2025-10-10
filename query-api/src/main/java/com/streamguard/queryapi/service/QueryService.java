package com.streamguard.queryapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.queryapi.model.SecurityEvent;
import com.streamguard.queryapi.model.ThreatAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
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
@RequiredArgsConstructor
public class QueryService {

    private final RocksDB rocksDB;
    private final ColumnFamilyHandle defaultColumnFamily;
    private final ColumnFamilyHandle aiAnalysisColumnFamily;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get latest events (up to limit)
     */
    public List<SecurityEvent> getLatestEvents(int limit) {
        List<SecurityEvent> events = new ArrayList<>();

        try (RocksIterator iterator = rocksDB.newIterator(defaultColumnFamily)) {
            iterator.seekToLast();

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
            log.warn("AI analysis column family not available");
            return analyses;
        }

        try (RocksIterator iterator = rocksDB.newIterator(aiAnalysisColumnFamily)) {
            iterator.seekToLast();

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
}
