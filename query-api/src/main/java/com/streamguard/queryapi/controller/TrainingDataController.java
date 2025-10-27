package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.model.AnomalyReport;
import com.streamguard.queryapi.model.BatchAnomaly;
import com.streamguard.queryapi.service.TrainingDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for accessing batch ML training data.
 *
 * Provides endpoints to query anomaly detection results from the Spark ML batch layer.
 * Part of Lambda Architecture serving layer.
 *
 * Sprint 12: Lambda Architecture Integration
 */
@RestController
@RequestMapping("/api/training-data")
@Tag(name = "Training Data", description = "Batch ML training data and anomaly detection results")
public class TrainingDataController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingDataController.class);
    private final TrainingDataService trainingDataService;

    public TrainingDataController(TrainingDataService trainingDataService) {
        this.trainingDataService = trainingDataService;
        logger.info("TrainingDataController initialized");
    }

    /**
     * GET /api/training-data/report
     * Returns anomaly detection summary report.
     */
    @GetMapping("/report")
    @Operation(summary = "Get anomaly detection summary report")
    public ResponseEntity<AnomalyReport> getReport() {
        try {
            logger.debug("GET /api/training-data/report");

            AnomalyReport report = trainingDataService.getAnomalyReport();

            if (report == null) {
                logger.warn("Anomaly report not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            logger.info("Returned anomaly report: {} users, {} anomalies",
                       report.getTotalUsers(), report.getAnomalousUsers());

            return ResponseEntity.ok(report);

        } catch (IOException e) {
            logger.error("Failed to read anomaly report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/anomalies
     * Returns all batch-detected anomalies.
     */
    @GetMapping("/anomalies")
    @Operation(summary = "Get all batch-detected anomalies")
    public ResponseEntity<List<BatchAnomaly>> getAnomalies() {
        try {
            logger.debug("GET /api/training-data/anomalies");

            List<BatchAnomaly> anomalies = trainingDataService.getAnomalies();

            logger.info("Returned {} batch anomalies", anomalies.size());
            return ResponseEntity.ok(anomalies);

        } catch (IOException e) {
            logger.error("Failed to read batch anomalies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/anomalies/{userId}
     * Returns batch anomaly for specific user.
     */
    @GetMapping("/anomalies/{userId}")
    @Operation(summary = "Get batch anomaly for specific user")
    public ResponseEntity<BatchAnomaly> getAnomalyForUser(@PathVariable String userId) {
        try {
            logger.debug("GET /api/training-data/anomalies/{}", userId);

            BatchAnomaly anomaly = trainingDataService.getAnomalyForUser(userId);

            if (anomaly == null) {
                logger.debug("No batch anomaly found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            logger.info("Returned batch anomaly for user: {} (score: {})",
                       userId, anomaly.getAnomalyScore());

            return ResponseEntity.ok(anomaly);

        } catch (IOException e) {
            logger.error("Failed to get anomaly for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/health
     * Check if training data is available.
     */
    @GetMapping("/health")
    @Operation(summary = "Check training data availability")
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean available = trainingDataService.isTrainingDataAvailable();
        String stats = trainingDataService.getTrainingDataStats();

        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("stats", stats);
        response.put("status", available ? "healthy" : "unavailable");

        logger.debug("Training data health: {}", available ? "healthy" : "unavailable");

        if (available) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * GET /api/training-data/stats
     * Get training data statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get training data statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            AnomalyReport report = trainingDataService.getAnomalyReport();

            if (report == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_users", report.getTotalUsers());
            stats.put("anomalous_users", report.getAnomalousUsers());
            stats.put("anomaly_rate", report.getAnomalyRate());
            stats.put("top_anomalies_count", report.getTopAnomalies() != null ? report.getTopAnomalies().size() : 0);

            return ResponseEntity.ok(stats);

        } catch (IOException e) {
            logger.error("Failed to get training data stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
