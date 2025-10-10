package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.model.AnomalyResult;
import com.streamguard.queryapi.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Anomaly Detection queries
 */
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Tag(name = "Anomalies", description = "Anomaly detection query endpoints")
public class AnomalyController {

    private final QueryService queryService;

    @GetMapping
    @Operation(summary = "Get latest anomalies", description = "Returns the most recent detected anomalies up to the specified limit")
    public ResponseEntity<List<AnomalyResult>> getLatestAnomalies(
            @Parameter(description = "Maximum number of anomalies to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<AnomalyResult> anomalies = queryService.getLatestAnomalies(limit);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get anomaly by event ID", description = "Returns a specific anomaly detection result by its event ID")
    public ResponseEntity<AnomalyResult> getAnomalyByEventId(
            @Parameter(description = "Event ID", example = "evt_1696723200_001")
            @PathVariable String eventId) {
        AnomalyResult anomaly = queryService.getAnomalyByEventId(eventId);
        if (anomaly != null) {
            return ResponseEntity.ok(anomaly);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{user}")
    @Operation(summary = "Get anomalies by user", description = "Returns all anomalies detected for a specific user")
    public ResponseEntity<List<AnomalyResult>> getAnomaliesByUser(
            @Parameter(description = "Username", example = "alice")
            @PathVariable String user,
            @Parameter(description = "Maximum number of anomalies to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<AnomalyResult> anomalies = queryService.getAnomaliesByUser(user, limit);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/high-score")
    @Operation(summary = "Get high-score anomalies", description = "Returns anomalies with score above the specified threshold")
    public ResponseEntity<List<AnomalyResult>> getHighScoreAnomalies(
            @Parameter(description = "Minimum anomaly score (0.0-1.0)", example = "0.7")
            @RequestParam(name = "threshold", defaultValue = "0.7") double threshold,
            @Parameter(description = "Maximum number of anomalies to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<AnomalyResult> anomalies = queryService.getHighScoreAnomalies(threshold, limit);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total anomaly count", description = "Returns the total number of anomalies detected")
    public ResponseEntity<Long> getAnomalyCount() {
        long count = queryService.getAnomalyCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/time-range")
    @Operation(summary = "Get anomalies by time range", description = "Returns anomalies detected within the specified time range")
    public ResponseEntity<List<AnomalyResult>> getAnomaliesByTimeRange(
            @Parameter(description = "Start timestamp (Unix epoch milliseconds)", example = "1696723200000")
            @RequestParam(name = "start_time") long startTime,
            @Parameter(description = "End timestamp (Unix epoch milliseconds)", example = "1696809600000")
            @RequestParam(name = "end_time") long endTime,
            @Parameter(description = "Maximum number of anomalies to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<AnomalyResult> anomalies = queryService.getAnomaliesByTimeRange(startTime, endTime, limit);
        return ResponseEntity.ok(anomalies);
    }
}
