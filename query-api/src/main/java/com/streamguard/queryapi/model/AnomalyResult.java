package com.streamguard.queryapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Anomaly Detection Result model
 * Mirrors the C++ AnomalyResult struct from stream-processor
 */
@Data
public class AnomalyResult {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("user")
    private String user;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("anomaly_score")
    private Double anomalyScore;  // 0.0-1.0

    @JsonProperty("time_anomaly")
    private Double timeAnomaly;

    @JsonProperty("ip_anomaly")
    private Double ipAnomaly;

    @JsonProperty("location_anomaly")
    private Double locationAnomaly;

    @JsonProperty("type_anomaly")
    private Double typeAnomaly;

    @JsonProperty("failure_anomaly")
    private Double failureAnomaly;

    @JsonProperty("reasons")
    private List<String> reasons;
}
