package com.streamguard.queryapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.queryapi.model.AnomalyResult;
import com.streamguard.queryapi.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnomalyController.class)
class AnomalyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueryService queryService;

    @Test
    void testGetHighScoreAnomalies_Success() throws Exception {
        // Arrange
        List<AnomalyResult> anomalies = Arrays.asList(
            createAnomaly("evt_001", "alice", 0.85),
            createAnomaly("evt_002", "bob", 0.72)
        );

        when(queryService.getHighScoreAnomalies(0.7, 100)).thenReturn(anomalies);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("threshold", "0.7")
                .param("limit", "100"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].eventId").value("evt_001"))
            .andExpect(jsonPath("$[0].user").value("alice"))
            .andExpect(jsonPath("$[0].anomalyScore").value(0.85))
            .andExpect(jsonPath("$[1].eventId").value("evt_002"))
            .andExpect(jsonPath("$[1].anomalyScore").value(0.72));
    }

    @Test
    void testGetHighScoreAnomalies_DefaultParameters() throws Exception {
        // Arrange - default threshold 0.5, limit 100
        List<AnomalyResult> anomalies = Collections.singletonList(
            createAnomaly("evt_001", "alice", 0.85)
        );

        when(queryService.getHighScoreAnomalies(0.5, 100)).thenReturn(anomalies);

        // Act & Assert - no parameters provided, should use defaults
        mockMvc.perform(get("/api/anomalies/high-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetHighScoreAnomalies_CustomLimit() throws Exception {
        // Arrange
        List<AnomalyResult> anomalies = Arrays.asList(
            createAnomaly("evt_001", "alice", 0.85),
            createAnomaly("evt_002", "bob", 0.75),
            createAnomaly("evt_003", "charlie", 0.92)
        );

        when(queryService.getHighScoreAnomalies(0.7, 10)).thenReturn(anomalies);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("threshold", "0.7")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void testGetHighScoreAnomalies_EmptyResult() throws Exception {
        // Arrange
        when(queryService.getHighScoreAnomalies(anyDouble(), anyInt()))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("threshold", "0.9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetHighScoreAnomalies_InvalidThreshold() throws Exception {
        // Act & Assert - threshold must be 0.0-1.0
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("threshold", "1.5")) // Invalid: > 1.0
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetHighScoreAnomalies_NegativeLimit() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("limit", "-10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAnomaliesByUser_Success() throws Exception {
        // Arrange
        String user = "alice";
        List<AnomalyResult> anomalies = Arrays.asList(
            createAnomaly("evt_001", user, 0.85),
            createAnomaly("evt_002", user, 0.72)
        );

        when(queryService.getAnomaliesByUser(eq(user), anyInt())).thenReturn(anomalies);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/user/{user}", user)
                .param("limit", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].user").value(user))
            .andExpect(jsonPath("$[1].user").value(user));
    }

    @Test
    void testGetAnomaliesByUser_NoAnomaliesFound() throws Exception {
        // Arrange
        when(queryService.getAnomaliesByUser(eq("nonexistent_user"), anyInt()))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/user/{user}", "nonexistent_user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetLatestAnomalies_Success() throws Exception {
        // Arrange
        List<AnomalyResult> anomalies = Arrays.asList(
            createAnomaly("evt_003", "charlie", 0.92),
            createAnomaly("evt_002", "bob", 0.72),
            createAnomaly("evt_001", "alice", 0.85)
        );

        when(queryService.getLatestAnomalies(anyInt())).thenReturn(anomalies);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void testGetLatestAnomalies_DefaultLimit() throws Exception {
        // Arrange
        List<AnomalyResult> anomalies = Collections.singletonList(
            createAnomaly("evt_001", "alice", 0.85)
        );

        when(queryService.getLatestAnomalies(100)).thenReturn(anomalies);

        // Act & Assert - no limit parameter, should use default 100
        mockMvc.perform(get("/api/anomalies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAnomalyCount_Success() throws Exception {
        // Arrange
        when(queryService.getAnomalyCount()).thenReturn(1234L);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/count"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(1234));
    }

    @Test
    void testGetAnomalyCount_ZeroAnomalies() throws Exception {
        // Arrange
        when(queryService.getAnomalyCount()).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void testGetAnomalyStats_Success() throws Exception {
        // Arrange
        when(queryService.getAnomalyCount()).thenReturn(150L);
        when(queryService.getHighScoreAnomalies(0.9, 1000))
            .thenReturn(Collections.nCopies(10, createAnomaly("evt", "user", 0.95)));
        when(queryService.getHighScoreAnomalies(0.7, 1000))
            .thenReturn(Collections.nCopies(35, createAnomaly("evt", "user", 0.75)));

        // Act & Assert
        mockMvc.perform(get("/api/anomalies/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAnomalies").value(150))
            .andExpect(jsonPath("$.criticalCount").value(10))
            .andExpect(jsonPath("$.highCount").value(35));
    }

    @Test
    void testVerifyAnomalyScoresInRange() throws Exception {
        // Arrange
        List<AnomalyResult> anomalies = Arrays.asList(
            createAnomaly("evt_001", "alice", 0.85),
            createAnomaly("evt_002", "bob", 0.52)
        );

        when(queryService.getHighScoreAnomalies(0.5, 100)).thenReturn(anomalies);

        // Act & Assert - verify all returned scores are >= threshold
        mockMvc.perform(get("/api/anomalies/high-score")
                .param("threshold", "0.5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].anomalyScore").value(0.85))
            .andExpect(jsonPath("$[1].anomalyScore").value(0.52));
    }

    @Test
    void testAnomalyResultContainsAllFields() throws Exception {
        // Arrange
        AnomalyResult anomaly = createDetailedAnomaly();
        when(queryService.getHighScoreAnomalies(anyDouble(), anyInt()))
            .thenReturn(Collections.singletonList(anomaly));

        // Act & Assert - verify all fields are serialized
        mockMvc.perform(get("/api/anomalies/high-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventId").value("evt_detailed"))
            .andExpect(jsonPath("$[0].user").value("test_user"))
            .andExpect(jsonPath("$[0].timestamp").exists())
            .andExpect(jsonPath("$[0].anomalyScore").value(0.82))
            .andExpect(jsonPath("$[0].timeAnomaly").value(0.95))
            .andExpect(jsonPath("$[0].ipAnomaly").value(0.99))
            .andExpect(jsonPath("$[0].locationAnomaly").value(0.88))
            .andExpect(jsonPath("$[0].typeAnomaly").value(0.45))
            .andExpect(jsonPath("$[0].failureAnomaly").value(0.30))
            .andExpect(jsonPath("$[0].reasons").isArray())
            .andExpect(jsonPath("$[0].reasons.length()").value(3));
    }

    // Helper methods
    private AnomalyResult createAnomaly(String eventId, String user, double score) {
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

    private AnomalyResult createDetailedAnomaly() {
        AnomalyResult anomaly = new AnomalyResult();
        anomaly.setEventId("evt_detailed");
        anomaly.setUser("test_user");
        anomaly.setTimestamp(System.currentTimeMillis());
        anomaly.setAnomalyScore(0.82);
        anomaly.setTimeAnomaly(0.95);
        anomaly.setIpAnomaly(0.99);
        anomaly.setLocationAnomaly(0.88);
        anomaly.setTypeAnomaly(0.45);
        anomaly.setFailureAnomaly(0.30);
        anomaly.setReasons(Arrays.asList(
            "Unusual login time (3 AM)",
            "New IP address: 45.142.120.10",
            "New location: Moscow, Russia"
        ));
        return anomaly;
    }
}
