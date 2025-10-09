package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Event metadata containing event-specific details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class EventMetadata {

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("geo_location")
    private String geoLocation;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("process_name")
    private String processName;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("bytes_transferred")
    private Long bytesTransferred;

    // Extensibility: custom fields
    private Map<String, String> customFields = new HashMap<>();

    // Constructors
    public EventMetadata() {}

    // Getters and Setters
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getBytesTransferred() {
        return bytesTransferred;
    }

    public void setBytesTransferred(Long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }

    public void addCustomField(String key, String value) {
        this.customFields.put(key, value);
    }

    @Override
    public String toString() {
        return "EventMetadata{" +
                "userAgent='" + userAgent + '\'' +
                ", geoLocation='" + geoLocation + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", filePath='" + filePath + '\'' +
                ", processName='" + processName + '\'' +
                ", domain='" + domain + '\'' +
                ", bytesTransferred=" + bytesTransferred +
                ", customFields=" + customFields +
                '}';
    }
}