package com.streamguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventFactory.
 *
 * Tests event generation for all 5 event types to ensure:
 * - Events are created with valid data
 * - Events can be serialized to JSON
 * - Event types match expectations
 * - Threat scores are within valid ranges
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class EventFactoryTest {

    private EventFactory factory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        factory = new EventFactory(12345L);  // Fixed seed for reproducibility
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGenerateAuthEvent() {
        AuthEvent event = factory.generateAuthEvent(false);

        assertNotNull(event, "AuthEvent should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertEquals("auth_attempt", event.getEventType(), "Event type should be auth_attempt");
        assertNotNull(event.getSourceIp(), "Source IP should not be null");
        assertNotNull(event.getUser(), "User should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
        assertNotNull(event.getAuthDetails(), "Auth details should not be null");
        assertNotNull(event.getAuthDetails().getAuthType(), "Auth type should not be null");
        assertNotNull(event.getAuthDetails().getStatus(), "Status should not be null");
    }

    @Test
    void testGenerateNetworkEvent() {
        NetworkEvent event = factory.generateNetworkEvent(false);

        assertNotNull(event, "NetworkEvent should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertEquals("network_connection", event.getEventType(), "Event type should be network_connection");
        assertNotNull(event.getSourceIp(), "Source IP should not be null");
        assertNotNull(event.getDestinationIp(), "Destination IP should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
        assertNotNull(event.getNetworkDetails(), "Network details should not be null");
        assertNotNull(event.getNetworkDetails().getProtocol(), "Protocol should not be null");
        assertTrue(event.getNetworkDetails().getDestinationPort() > 0,
                  "Destination port should be positive");
    }

    @Test
    void testGenerateFileEvent() {
        FileEvent event = factory.generateFileEvent(false);

        assertNotNull(event, "FileEvent should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertEquals("file_access", event.getEventType(), "Event type should be file_access");
        assertNotNull(event.getUser(), "User should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
        assertNotNull(event.getFileDetails(), "File details should not be null");
        assertNotNull(event.getFileDetails().getFilePath(), "File path should not be null");
        assertNotNull(event.getFileDetails().getOperation(), "Operation should not be null");
    }

    @Test
    void testGenerateProcessEvent() {
        ProcessEvent event = factory.generateProcessEvent(false);

        assertNotNull(event, "ProcessEvent should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertEquals("process_execution", event.getEventType(), "Event type should be process_execution");
        assertNotNull(event.getUser(), "User should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
        assertNotNull(event.getProcessDetails(), "Process details should not be null");
        assertNotNull(event.getProcessDetails().getProcessName(), "Process name should not be null");
        assertNotNull(event.getProcessDetails().getCommandLine(), "Command line should not be null");
        assertTrue(event.getProcessDetails().getProcessId() > 0, "Process ID should be positive");
    }

    @Test
    void testGenerateDnsEvent() {
        DnsEvent event = factory.generateDnsEvent(false);

        assertNotNull(event, "DnsEvent should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertEquals("dns_query", event.getEventType(), "Event type should be dns_query");
        assertNotNull(event.getUser(), "User should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
        assertNotNull(event.getDnsDetails(), "DNS details should not be null");
        assertNotNull(event.getDnsDetails().getQueryName(), "Query name should not be null");
        assertNotNull(event.getDnsDetails().getQueryType(), "Query type should not be null");
        assertNotNull(event.getDnsDetails().getResponseCode(), "Response code should not be null");
    }

    @Test
    void testGenerateRandomEvent() {
        Event event = factory.generateEvent();

        assertNotNull(event, "Generated event should not be null");
        assertNotNull(event.getEventId(), "Event ID should not be null");
        assertNotNull(event.getEventType(), "Event type should not be null");
        assertTrue(event.getThreatScore() >= 0.0 && event.getThreatScore() <= 1.0,
                  "Threat score should be between 0.0 and 1.0");
    }

    @Test
    void testEventDistribution() {
        // Generate 1000 events and check distribution is reasonable
        int authCount = 0;
        int networkCount = 0;
        int fileCount = 0;
        int processCount = 0;
        int dnsCount = 0;

        for (int i = 0; i < 1000; i++) {
            Event event = factory.generateEvent();
            switch (event.getEventType()) {
                case "auth_attempt":
                    authCount++;
                    break;
                case "network_connection":
                    networkCount++;
                    break;
                case "file_access":
                    fileCount++;
                    break;
                case "process_execution":
                    processCount++;
                    break;
                case "dns_query":
                    dnsCount++;
                    break;
            }
        }

        // Check distribution is roughly correct (with some tolerance)
        // Expected: 40% auth, 25% network, 20% file, 10% process, 5% dns
        assertTrue(authCount > 300 && authCount < 500, "Auth events should be ~40% (got " + authCount + ")");
        assertTrue(networkCount > 150 && networkCount < 350, "Network events should be ~25% (got " + networkCount + ")");
        assertTrue(fileCount > 100 && fileCount < 300, "File events should be ~20% (got " + fileCount + ")");
        assertTrue(processCount > 50 && processCount < 200, "Process events should be ~10% (got " + processCount + ")");
        assertTrue(dnsCount > 0 && dnsCount < 150, "DNS events should be ~5% (got " + dnsCount + ")");
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Test that all event types can be serialized to JSON
        Event[] events = {
            factory.generateAuthEvent(false),
            factory.generateNetworkEvent(false),
            factory.generateFileEvent(false),
            factory.generateProcessEvent(false),
            factory.generateDnsEvent(false)
        };

        for (Event event : events) {
            String json = objectMapper.writeValueAsString(event);
            assertNotNull(json, "JSON should not be null");
            assertTrue(json.length() > 0, "JSON should not be empty");
            assertTrue(json.contains("event_id"), "JSON should contain event_id");
            assertTrue(json.contains("event_type"), "JSON should contain event_type");
            assertTrue(json.contains("threat_score"), "JSON should contain threat_score");

            // Verify we can deserialize back
            Event deserialized = objectMapper.readValue(json, Event.class);
            assertNotNull(deserialized, "Deserialized event should not be null");
            assertEquals(event.getEventId(), deserialized.getEventId(), "Event ID should match");
            assertEquals(event.getEventType(), deserialized.getEventType(), "Event type should match");
        }
    }

    @Test
    void testEventIdFormat() {
        Event event = factory.generateEvent();
        String eventId = event.getEventId();

        assertTrue(eventId.startsWith("evt_"), "Event ID should start with evt_");
        assertTrue(eventId.length() > 4, "Event ID should have content after prefix");
    }

    @Test
    void testTimestampIsRecent() {
        Event event = factory.generateEvent();
        long timestamp = event.getTimestamp();
        long now = System.currentTimeMillis();

        assertTrue(timestamp <= now, "Timestamp should not be in the future");
        assertTrue(timestamp > now - 10000, "Timestamp should be within last 10 seconds");
    }

    @Test
    void testMetadataPopulated() {
        Event event = factory.generateEvent();

        assertNotNull(event.getMetadata(), "Metadata should not be null");
        assertNotNull(event.getMetadata().getHostname(), "Hostname should be populated");
        assertNotNull(event.getMetadata().getGeoLocation(), "Geo location should be populated");
    }

    @Test
    void testReproducibilityWithSeed() {
        // Events with same seed should produce same sequence
        EventFactory factory1 = new EventFactory(99999L);
        EventFactory factory2 = new EventFactory(99999L);

        Event event1 = factory1.generateEvent();
        Event event2 = factory2.generateEvent();

        assertEquals(event1.getEventType(), event2.getEventType(),
                    "Events with same seed should have same type");
    }
}
