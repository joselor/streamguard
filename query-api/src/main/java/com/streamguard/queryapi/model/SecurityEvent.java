package com.streamguard.queryapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Security Event model
 * Mirrors the C++ Event struct from stream-processor
 */
@Data
public class SecurityEvent {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("source_ip")
    private String sourceIp;

    @JsonProperty("destination_ip")
    private String destinationIp;

    @JsonProperty("user")
    private String user;

    @JsonProperty("status")
    private String status;

    @JsonProperty("threat_score")
    private Double threatScore;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
