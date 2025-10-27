package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.model.AnomalyReport;
import com.streamguard.queryapi.service.QueryService;
import com.streamguard.queryapi.service.TrainingDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prometheus metrics endpoint for StreamGuard.
 *
 * Exposes metrics in Prometheus text format for Grafana dashboards.
 * Sprint 12: Added batch ML training data metrics.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Metrics", description = "Prometheus metrics for monitoring")
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private final QueryService queryService;
    private final TrainingDataService trainingDataService;

    public MetricsController(QueryService queryService, TrainingDataService trainingDataService) {
        this.queryService = queryService;
        this.trainingDataService = trainingDataService;
    }

    @GetMapping(value = "/metrics", produces = "text/plain")
    @Operation(summary = "Get Prometheus metrics")
    public String getMetrics() {
        StringBuilder metrics = new StringBuilder();

        try {
            // Speed layer metrics (real-time processing)
            QueryService.StatsSummary summary = queryService.getStatsSummary();

            metrics.append("# HELP streamguard_events_total Total events processed by speed layer\n");
            metrics.append("# TYPE streamguard_events_total gauge\n");
            metrics.append("streamguard_events_total ")
                  .append(summary.getTotalEvents())
                  .append("\n\n");

            metrics.append("# HELP streamguard_anomalies_total Total anomalies detected by speed layer\n");
            metrics.append("# TYPE streamguard_anomalies_total gauge\n");
            metrics.append("streamguard_anomalies_total ")
                  .append(summary.getTotalAnomalies())
                  .append("\n\n");

            metrics.append("# HELP streamguard_ai_analyses_total Total AI threat analyses\n");
            metrics.append("# TYPE streamguard_ai_analyses_total gauge\n");
            metrics.append("streamguard_ai_analyses_total ")
                  .append(summary.getTotalAnalyses())
                  .append("\n\n");

            // Batch layer metrics (Spark ML pipeline - Sprint 12)
            if (trainingDataService.isTrainingDataAvailable()) {
                AnomalyReport report = trainingDataService.getAnomalyReport();

                if (report != null) {
                    metrics.append("# HELP training_data_users_total Total users analyzed in batch ML pipeline\n");
                    metrics.append("# TYPE training_data_users_total gauge\n");
                    metrics.append("training_data_users_total ")
                          .append(report.getTotalUsers() != null ? report.getTotalUsers() : 0)
                          .append("\n\n");

                    metrics.append("# HELP training_data_anomalies_total Anomalous users detected by batch ML\n");
                    metrics.append("# TYPE training_data_anomalies_total gauge\n");
                    metrics.append("training_data_anomalies_total ")
                          .append(report.getAnomalousUsers() != null ? report.getAnomalousUsers() : 0)
                          .append("\n\n");

                    metrics.append("# HELP training_data_anomaly_rate Batch ML anomaly detection rate (0.0-1.0)\n");
                    metrics.append("# TYPE training_data_anomaly_rate gauge\n");
                    metrics.append("training_data_anomaly_rate ")
                          .append(report.getAnomalyRate() != null ? report.getAnomalyRate() : 0.0)
                          .append("\n\n");

                    metrics.append("# HELP training_data_available Training data availability status (1=available, 0=unavailable)\n");
                    metrics.append("# TYPE training_data_available gauge\n");
                    metrics.append("training_data_available 1\n\n");
                } else {
                    metrics.append("# HELP training_data_available Training data availability status (1=available, 0=unavailable)\n");
                    metrics.append("# TYPE training_data_available gauge\n");
                    metrics.append("training_data_available 0\n\n");
                }
            } else {
                metrics.append("# HELP training_data_available Training data availability status (1=available, 0=unavailable)\n");
                metrics.append("# TYPE training_data_available gauge\n");
                metrics.append("training_data_available 0\n\n");
            }

        } catch (Exception e) {
            logger.error("Failed to generate metrics", e);
            metrics.append("# ERROR: Failed to generate metrics\n");
        }

        logger.debug("Generated Prometheus metrics ({} bytes)", metrics.length());
        return metrics.toString();
    }
}
