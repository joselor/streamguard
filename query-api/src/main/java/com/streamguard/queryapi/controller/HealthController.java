package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for System Health and Status
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "System health and status endpoints")
public class HealthController {

    private final QueryService queryService;

    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns OK if the API is running")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    @Operation(summary = "Detailed system status", description = "Returns detailed status including database connectivity and feature availability")
    public ResponseEntity<Map<String, Object>> systemStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());

        // Check database connectivity
        Map<String, Object> database = new HashMap<>();
        database.put("connected", true);

        // Check data availability
        long eventCount = queryService.getEventCount();
        long anomalyCount = queryService.getAnomalyCount();
        long analysisCount = queryService.getAnalysisCount();

        database.put("eventCount", eventCount);
        database.put("anomalyCount", anomalyCount);
        database.put("analysisCount", analysisCount);

        status.put("database", database);

        // Check feature availability
        Map<String, Object> features = new HashMap<>();
        features.put("eventsAvailable", eventCount > 0);
        features.put("anomalyDetectionAvailable", anomalyCount >= 0); // >= 0 means CF exists
        features.put("aiAnalysisAvailable", analysisCount >= 0); // >= 0 means CF exists

        status.put("features", features);

        // Provide warnings
        Map<String, String> warnings = new HashMap<>();
        if (eventCount == 0) {
            warnings.put("events", "No events found - stream-processor may not be running or connected");
        }
        if (analysisCount == 0) {
            warnings.put("aiAnalysis", "No AI analyses found - check OPENAI_API_KEY configuration");
        }
        if (anomalyCount == 0) {
            warnings.put("anomalies", "No anomalies detected - may need more events or lower threshold");
        }

        if (!warnings.isEmpty()) {
            status.put("warnings", warnings);
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Returns 200 if the system is ready to serve requests, 503 otherwise")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();

        long eventCount = queryService.getEventCount();

        if (eventCount > 0) {
            readiness.put("ready", true);
            readiness.put("message", "System is ready and has data");
            return ResponseEntity.ok(readiness);
        } else {
            readiness.put("ready", false);
            readiness.put("message", "System is not ready - no events in database");
            return ResponseEntity.status(503).body(readiness);
        }
    }
}
