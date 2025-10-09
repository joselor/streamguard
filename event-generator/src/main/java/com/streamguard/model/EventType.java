
package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Event type enumeration
 */
public enum EventType {
    @JsonProperty("auth_attempt")
    AUTH_ATTEMPT("auth_attempt"),
    
    @JsonProperty("network_connection")
    NETWORK_CONNECTION("network_connection"),
    
    @JsonProperty("file_access")
    FILE_ACCESS("file_access"),
    
    @JsonProperty("process_execution")
    PROCESS_EXECUTION("process_execution"),
    
    @JsonProperty("dns_query")
    DNS_QUERY("dns_query");
    
    private final String value;
    
    EventType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public static EventType fromString(String value) {
        for (EventType type : EventType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + value);
    }
}