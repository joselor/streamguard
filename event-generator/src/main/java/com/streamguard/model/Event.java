package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all security events in StreamGuard.
 * Uses Jackson polymorphic deserialization to handle different event types.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event_type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AuthEvent.class, name = "auth_attempt"),
    @JsonSubTypes.Type(value = NetworkEvent.class, name = "network_connection"),
    @JsonSubTypes.Type(value = FileEvent.class, name = "file_access"),
    @JsonSubTypes.Type(value = ProcessEvent.class, name = "process_execution"),
    @JsonSubTypes.Type(value = DnsEvent.class, name = "dns_query")
})
public abstract class Event {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("source_ip")
    private String sourceIp;

    @JsonProperty("destination_ip")
    private String destinationIp;

    @JsonProperty("user")
    private String user;

    @JsonProperty("threat_score")
    private double threatScore;

    @JsonProperty("status")
    private String status = "success";

    @JsonProperty("metadata")
    private EventMetadata metadata;

    // Constructors
    public Event() {
        this.eventId = generateEventId();
        this.timestamp = System.currentTimeMillis();
        this.metadata = new EventMetadata();
        this.status = "success";
    }

    public Event(String eventType, String sourceIp, String destinationIp,
                 String user, double threatScore) {
        this();
        this.eventType = eventType;
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.user = user;
        this.threatScore = Math.max(0.0, Math.min(1.0, threatScore));
    }

    // Generate unique event ID
    private String generateEventId() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "");
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public double getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(double threatScore) {
        this.threatScore = Math.max(0.0, Math.min(1.0, threatScore));
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventId, event.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", user='" + user + '\'' +
                ", threatScore=" + threatScore +
                '}';
    }

    // Nested class for Metadata
    public static class EventMetadata {
        @JsonProperty("hostname")
        private String hostname;

        @JsonProperty("geo_location")
        private String geoLocation;

        @JsonProperty("user_agent")
        private String userAgent;

        @JsonProperty("session_id")
        private String sessionId;

        public EventMetadata() {}

        public EventMetadata(String hostname, String geoLocation) {
            this.hostname = hostname;
            this.geoLocation = geoLocation;
        }

        // Getters and Setters
        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getGeoLocation() {
            return geoLocation;
        }

        public void setGeoLocation(String geoLocation) {
            this.geoLocation = geoLocation;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public String toString() {
            return "EventMetadata{" +
                    "hostname='" + hostname + '\'' +
                    ", geoLocation='" + geoLocation + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    '}';
        }
    }
}