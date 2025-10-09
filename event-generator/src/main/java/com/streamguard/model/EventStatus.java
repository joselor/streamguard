package com.streamguard.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Event status enumeration
 */
public enum EventStatus {
    @JsonProperty("success")
    SUCCESS("success"),

    @JsonProperty("failed")
    FAILED("failed"),

    @JsonProperty("blocked")
    BLOCKED("blocked"),

    @JsonProperty("pending")
    PENDING("pending");

    private final String value;

    EventStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static EventStatus fromString(String value) {
        for (EventStatus status : EventStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown event status: " + value);
    }
}