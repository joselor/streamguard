package com.streamguard.queryapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.queryapi.model.AnomalyResult;
import com.streamguard.queryapi.model.SecurityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private RocksDB rocksDB;

    @Mock
    private ColumnFamilyHandle defaultColumnFamily;

    @Mock
    private ColumnFamilyHandle anomaliesColumnFamily;

    @Mock
    private ColumnFamilyHandle aiAnalysisColumnFamily;

    @Mock
    private RocksIterator rocksIterator;

    private QueryService queryService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        queryService = new QueryService(
            rocksDB,
            defaultColumnFamily,
            aiAnalysisColumnFamily,
            anomaliesColumnFamily
        );
    }

    @Test
    void testGetEvent_Success() throws Exception {
        // Arrange
        String eventId = "evt_test_001";
        SecurityEvent expectedEvent = new SecurityEvent();
        expectedEvent.setEventId(eventId);
        expectedEvent.setUser("alice");
        expectedEvent.setSourceIp("192.168.1.100");
        expectedEvent.setThreatScore(0.15);

        String json = objectMapper.writeValueAsString(expectedEvent);
        String key = "auth_attempt:1234567890:" + eventId;

        when(rocksDB.newIterator(defaultColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, false);
        when(rocksIterator.key()).thenReturn(key.getBytes());
        when(rocksIterator.value()).thenReturn(json.getBytes());

        // Act
        SecurityEvent result = queryService.getEventById(eventId);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals("alice", result.getUser());
        assertEquals("192.168.1.100", result.getSourceIp());
        assertEquals(0.15, result.getThreatScore(), 0.001);
    }

    @Test
    void testGetEvent_NotFound() throws Exception {
        // Arrange
        String eventId = "evt_nonexistent";
        when(rocksDB.newIterator(defaultColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(false);

        // Act
        SecurityEvent result = queryService.getEventById(eventId);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetEvent_InvalidJson() throws Exception {
        // Arrange
        String eventId = "evt_invalid";
        String key = "auth_attempt:1234567890:" + eventId;
        byte[] invalidData = "not valid json".getBytes(StandardCharsets.UTF_8);

        when(rocksDB.newIterator(defaultColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, false);
        when(rocksIterator.key()).thenReturn(key.getBytes());
        when(rocksIterator.value()).thenReturn(invalidData);

        // Act - should return null on error
        SecurityEvent result = queryService.getEventById(eventId);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetHighScoreAnomalies_Success() throws Exception {
        // Arrange
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, false); // 2 results

        // Create test anomalies
        AnomalyResult anomaly1 = createTestAnomaly("evt_001", "alice", 0.85);
        AnomalyResult anomaly2 = createTestAnomaly("evt_002", "bob", 0.72);

        String json1 = objectMapper.writeValueAsString(anomaly1);
        String json2 = objectMapper.writeValueAsString(anomaly2);

        when(rocksIterator.value())
            .thenReturn(json1.getBytes(StandardCharsets.UTF_8))
            .thenReturn(json2.getBytes(StandardCharsets.UTF_8));

        // Act
        List<AnomalyResult> results = queryService.getHighScoreAnomalies(0.7, 10);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        assertEquals("evt_001", results.get(0).getEventId());
        assertEquals(0.85, results.get(0).getAnomalyScore(), 0.001);

        assertEquals("evt_002", results.get(1).getEventId());
        assertEquals(0.72, results.get(1).getAnomalyScore(), 0.001);

        verify(rocksIterator, times(1)).seekToLast();
        verify(rocksIterator, times(2)).prev();
        verify(rocksIterator, times(1)).close();
    }

    @Test
    void testGetHighScoreAnomalies_FilterByThreshold() throws Exception {
        // Arrange
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, true, false); // 3 results

        AnomalyResult high = createTestAnomaly("evt_high", "alice", 0.85);
        AnomalyResult med = createTestAnomaly("evt_med", "bob", 0.65);
        AnomalyResult low = createTestAnomaly("evt_low", "charlie", 0.45);

        when(rocksIterator.value())
            .thenReturn(objectMapper.writeValueAsString(high).getBytes())
            .thenReturn(objectMapper.writeValueAsString(med).getBytes())
            .thenReturn(objectMapper.writeValueAsString(low).getBytes());

        // Act - threshold 0.7
        List<AnomalyResult> results = queryService.getHighScoreAnomalies(0.7, 10);

        // Assert - should only return high (0.85), not med or low
        assertEquals(1, results.size());
        assertEquals("evt_high", results.get(0).getEventId());
        assertTrue(results.get(0).getAnomalyScore() >= 0.7);
    }

    @Test
    void testGetHighScoreAnomalies_RespectLimit() throws Exception {
        // Arrange
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, true, true, true, false);

        for (int i = 0; i < 5; i++) {
            AnomalyResult anomaly = createTestAnomaly("evt_" + i, "user" + i, 0.8);
            when(rocksIterator.value()).thenReturn(
                objectMapper.writeValueAsString(anomaly).getBytes()
            );
        }

        // Act - limit 3
        List<AnomalyResult> results = queryService.getHighScoreAnomalies(0.5, 3);

        // Assert - should return only 3 even though 5 are available
        assertEquals(3, results.size());
    }

    @Test
    void testGetHighScoreAnomalies_NullColumnFamily() {
        // Arrange
        QueryService serviceWithNullCF = new QueryService(
            rocksDB,
            defaultColumnFamily,
            aiAnalysisColumnFamily,
            null  // null anomalies CF
        );

        // Act
        List<AnomalyResult> results = serviceWithNullCF.getHighScoreAnomalies(0.7, 10);

        // Assert - should return empty list, not throw exception
        assertNotNull(results);
        assertEquals(0, results.size());

        verify(rocksDB, never()).newIterator(any(ColumnFamilyHandle.class));
    }

    @Test
    void testGetHighScoreAnomalies_EmptyDatabase() {
        // Arrange
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(false); // No data

        // Act
        List<AnomalyResult> results = queryService.getHighScoreAnomalies(0.7, 10);

        // Assert
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void testGetHighScoreAnomalies_InvalidJson() throws Exception {
        // Arrange
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, false);
        when(rocksIterator.value()).thenReturn("invalid json".getBytes());

        // Act - should skip invalid entries
        List<AnomalyResult> results = queryService.getHighScoreAnomalies(0.7, 10);

        // Assert - should return empty list (skipped invalid entry)
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void testGetEventCount() throws Exception {
        // Arrange
        when(rocksDB.newIterator(defaultColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, true, false); // 3 events

        // Act
        long count = queryService.getEventCount();

        // Assert
        assertEquals(3, count);
        verify(rocksIterator, times(1)).seekToFirst();
    }

    @Test
    void testGetAnomaliesByUser() throws Exception {
        // Arrange
        String targetUser = "alice";
        when(rocksDB.newIterator(anomaliesColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, true, false);

        AnomalyResult aliceAnomaly1 = createTestAnomaly("evt_001", "alice", 0.85);
        AnomalyResult bobAnomaly = createTestAnomaly("evt_002", "bob", 0.75);
        AnomalyResult aliceAnomaly2 = createTestAnomaly("evt_003", "alice", 0.68);

        when(rocksIterator.value())
            .thenReturn(objectMapper.writeValueAsString(aliceAnomaly1).getBytes())
            .thenReturn(objectMapper.writeValueAsString(bobAnomaly).getBytes())
            .thenReturn(objectMapper.writeValueAsString(aliceAnomaly2).getBytes());

        // Act
        List<AnomalyResult> results = queryService.getAnomaliesByUser(targetUser, 10);

        // Assert - should only return alice's anomalies
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(anomaly -> assertEquals(targetUser, anomaly.getUser()));
    }

    @Test
    void testGetLatestEvents() throws Exception {
        // Arrange
        when(rocksDB.newIterator(defaultColumnFamily)).thenReturn(rocksIterator);
        when(rocksIterator.isValid()).thenReturn(true, true, false);

        SecurityEvent event1 = createTestEvent("evt_001", "alice");
        SecurityEvent event2 = createTestEvent("evt_002", "bob");

        when(rocksIterator.value())
            .thenReturn(objectMapper.writeValueAsString(event1).getBytes())
            .thenReturn(objectMapper.writeValueAsString(event2).getBytes());

        // Act
        List<SecurityEvent> results = queryService.getLatestEvents(10);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(rocksIterator, times(1)).seekToLast(); // Start from most recent
    }

    // Helper methods
    private AnomalyResult createTestAnomaly(String eventId, String user, double score) {
        AnomalyResult anomaly = new AnomalyResult();
        anomaly.setEventId(eventId);
        anomaly.setUser(user);
        anomaly.setTimestamp(System.currentTimeMillis());
        anomaly.setAnomalyScore(score);
        anomaly.setTimeAnomaly(score * 0.3);
        anomaly.setIpAnomaly(score * 0.4);
        anomaly.setLocationAnomaly(score * 0.2);
        anomaly.setTypeAnomaly(score * 0.1);
        anomaly.setFailureAnomaly(0.0);
        return anomaly;
    }

    private SecurityEvent createTestEvent(String eventId, String user) {
        SecurityEvent event = new SecurityEvent();
        event.setEventId(eventId);
        event.setUser(user);
        event.setTimestamp(System.currentTimeMillis());
        event.setSourceIp("192.168.1.100");
        event.setEventType("auth_attempt");
        event.setStatus("success");
        event.setThreatScore(0.15);
        return event;
    }
}
