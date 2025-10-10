package com.streamguard.queryapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * AI Threat Analysis model
 * Mirrors the C++ ThreatAnalysis struct from stream-processor
 */
@Data
public class ThreatAnalysis {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("severity")
    private String severity;  // LOW, MEDIUM, HIGH, CRITICAL

    @JsonProperty("attack_type")
    private String attackType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("recommendations")
    private List<String> recommendations;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("timestamp")
    private Long timestamp;
}
