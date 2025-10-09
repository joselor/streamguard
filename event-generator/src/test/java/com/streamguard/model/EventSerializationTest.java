package com.streamguard.model;

import com.streamguard.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Event serialization and deserialization.
 *
 * <p>Verifies that all event types can be correctly serialized to JSON
 * and deserialized back to the correct subtype with all data intact.
 *
 * <p>These tests are critical for ensuring data integrity as events
 * flow through the system: Generator → Kafka → Processor → Storage.
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class EventSerializationTest {

    @Test
    @DisplayName("Serialize and deserialize AuthEvent")
    void testAuthEventSerialization() throws Exception {
        // Create an AuthEvent
        AuthEvent original = new AuthEvent(
            "192.168.1.100",
            "10.0.0.5",
            "john.doe",
            0.05,
            "password",
            "success"
        );

        original.getMetadata().setHostname("laptop-jdoe");
        original.getMetadata().setGeoLocation("US-MN-Minneapolis");
        original.getAuthDetails().setAttemptsCount(1);

        // Serialize to JSON
        String json = JsonUtil.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("auth_attempt"));
        assertTrue(json.contains("john.doe"));

        // Deserialize back
        Event deserialized = JsonUtil.fromJson(json);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof AuthEvent);

        AuthEvent authEvent = (AuthEvent) deserialized;
        assertEquals(original.getUser(), authEvent.getUser());
        assertEquals(original.getSourceIp(), authEvent.getSourceIp());
        assertEquals(original.getAuthDetails().getStatus(),
                    authEvent.getAuthDetails().getStatus());
    }

    @Test
    @DisplayName("Serialize and deserialize NetworkEvent")
    void testNetworkEventSerialization() throws Exception {
        // Create a NetworkEvent
        NetworkEvent original = new NetworkEvent(
            "192.168.1.100",
            "93.184.216.34",
            "jane.smith",
            0.10,
            "tcp",
            54321,
            443
        );

        original.getMetadata().setHostname("workstation-02");
        original.getNetworkDetails().setBytesSent(1024);
        original.getNetworkDetails().setBytesReceived(4096);
        original.getNetworkDetails().setConnectionState("established");

        // Serialize to JSON
        String json = JsonUtil.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("network_connection"));

        // Deserialize back
        Event deserialized = JsonUtil.fromJson(json);
        assertTrue(deserialized instanceof NetworkEvent);

        NetworkEvent networkEvent = (NetworkEvent) deserialized;
        assertEquals(443, networkEvent.getNetworkDetails().getDestinationPort());
        assertEquals("tcp", networkEvent.getNetworkDetails().getProtocol());
    }

    @Test
    @DisplayName("Serialize and deserialize FileEvent")
    void testFileEventSerialization() throws Exception {
        FileEvent original = new FileEvent(
            "192.168.1.105",
            "10.0.0.20",
            "contractor_temp",
            0.65,
            "/var/data/confidential/payroll_2024.xlsx",
            "read"
        );

        original.getFileDetails().setFileSizeBytes(524288);
        original.getFileDetails().setEncrypted(false);

        String json = JsonUtil.toJson(original);
        Event deserialized = JsonUtil.fromJson(json);

        assertTrue(deserialized instanceof FileEvent);
        FileEvent fileEvent = (FileEvent) deserialized;
        assertEquals(original.getFileDetails().getFilePath(),
                    fileEvent.getFileDetails().getFilePath());
    }

    @Test
    @DisplayName("Serialize and deserialize ProcessEvent")
    void testProcessEventSerialization() throws Exception {
        ProcessEvent original = new ProcessEvent(
            "192.168.1.110",
            "192.168.1.110",
            "regular_user",
            0.78,
            "powershell.exe",
            "powershell.exe -ExecutionPolicy Bypass"
        );

        original.getProcessDetails().setProcessId(8745);
        original.getProcessDetails().setUserPrivileges("standard");
        original.getProcessDetails().setSigned(true);

        String json = JsonUtil.toJson(original);
        Event deserialized = JsonUtil.fromJson(json);

        assertTrue(deserialized instanceof ProcessEvent);
        ProcessEvent processEvent = (ProcessEvent) deserialized;
        assertEquals(8745, processEvent.getProcessDetails().getProcessId());
    }

    @Test
    @DisplayName("Serialize and deserialize DnsEvent")
    void testDnsEventSerialization() throws Exception {
        DnsEvent original = new DnsEvent(
            "192.168.1.115",
            "8.8.8.8",
            "user123",
            0.20,
            "www.example.com",
            "A"
        );

        original.getDnsDetails().setResponseCode("NOERROR");
        original.getDnsDetails().getResolvedIps().add("93.184.216.34");
        original.getDnsDetails().setTtl(300);

        String json = JsonUtil.toJson(original);
        Event deserialized = JsonUtil.fromJson(json);

        assertTrue(deserialized instanceof DnsEvent);
        DnsEvent dnsEvent = (DnsEvent) deserialized;
        assertEquals("www.example.com", dnsEvent.getDnsDetails().getQueryName());
    }

    @Test
    @DisplayName("Serialize to bytes for Kafka")
    void testSerializeToBytes() throws Exception {
        AuthEvent event = new AuthEvent(
            "192.168.1.100",
            "10.0.0.5",
            "test_user",
            0.5,
            "password",
            "success"
        );

        byte[] bytes = JsonUtil.toJsonBytes(event);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Deserialize from bytes
        Event deserialized = JsonUtil.fromJsonBytes(bytes);
        assertNotNull(deserialized);
        assertEquals(event.getUser(), deserialized.getUser());
    }

    @Test
    @DisplayName("Handle invalid JSON gracefully")
    void testInvalidJsonHandling() {
        String invalidJson = "{invalid json}";
        assertFalse(JsonUtil.isValidJson(invalidJson));
        assertThrows(Exception.class, () -> JsonUtil.fromJson(invalidJson));
    }

    @Test
    @DisplayName("Event ID is generated automatically")
    void testEventIdGeneration() {
        AuthEvent event1 = new AuthEvent();
        AuthEvent event2 = new AuthEvent();

        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
        assertTrue(event1.getEventId().startsWith("evt_"));
    }

    @Test
    @DisplayName("Threat score is clamped to 0.0-1.0")
    void testThreatScoreClamping() {
        AuthEvent event = new AuthEvent();

        // Test upper bound
        event.setThreatScore(1.5);
        assertEquals(1.0, event.getThreatScore());

        // Test lower bound
        event.setThreatScore(-0.5);
        assertEquals(0.0, event.getThreatScore());

        // Test valid range
        event.setThreatScore(0.75);
        assertEquals(0.75, event.getThreatScore());
    }

    @Test
    @DisplayName("Pretty print JSON for debugging")
    void testPrettyPrintJson() throws Exception {
        AuthEvent event = new AuthEvent(
            "192.168.1.100",
            "10.0.0.5",
            "john.doe",
            0.5,
            "password",
            "success"
        );

        String prettyJson = JsonUtil.toPrettyJson(event);
        assertNotNull(prettyJson);
        assertTrue(prettyJson.contains("\n")); // Has newlines
        assertTrue(prettyJson.contains("  ")); // Has indentation

        // Can still deserialize pretty-printed JSON
        Event deserialized = JsonUtil.fromJson(prettyJson);
        assertEquals(event.getUser(), deserialized.getUser());
    }

    @Test
    @DisplayName("All event types have correct event_type discriminator")
    void testEventTypeDiscriminators() throws Exception {
        AuthEvent authEvent = new AuthEvent();
        NetworkEvent networkEvent = new NetworkEvent();
        FileEvent fileEvent = new FileEvent();
        ProcessEvent processEvent = new ProcessEvent();
        DnsEvent dnsEvent = new DnsEvent();

        assertEquals("auth_attempt", authEvent.getEventType());
        assertEquals("network_connection", networkEvent.getEventType());
        assertEquals("file_access", fileEvent.getEventType());
        assertEquals("process_execution", processEvent.getEventType());
        assertEquals("dns_query", dnsEvent.getEventType());
    }

    @Test
    @DisplayName("Polymorphic deserialization returns correct subtype")
    void testPolymorphicDeserialization() throws Exception {
        // Create events of different types
        AuthEvent auth = new AuthEvent("1.1.1.1", "2.2.2.2", "user1", 0.5, "password", "success");
        NetworkEvent network = new NetworkEvent("1.1.1.1", "2.2.2.2", "user2", 0.3, "tcp", 1234, 80);

        // Serialize
        String authJson = JsonUtil.toJson(auth);
        String networkJson = JsonUtil.toJson(network);

        // Deserialize as base Event class
        Event deserializedAuth = JsonUtil.fromJson(authJson);
        Event deserializedNetwork = JsonUtil.fromJson(networkJson);

        // Verify correct subtypes
        assertTrue(deserializedAuth instanceof AuthEvent);
        assertTrue(deserializedNetwork instanceof NetworkEvent);
        assertFalse(deserializedAuth instanceof NetworkEvent);
        assertFalse(deserializedNetwork instanceof AuthEvent);
    }

    @Test
    @DisplayName("Timestamp is set automatically on creation")
    void testTimestampAutoGeneration() {
        long beforeCreation = System.currentTimeMillis();
        AuthEvent event = new AuthEvent();
        long afterCreation = System.currentTimeMillis();

        assertTrue(event.getTimestamp() >= beforeCreation);
        assertTrue(event.getTimestamp() <= afterCreation);
    }

    @Test
    @DisplayName("Metadata is initialized on event creation")
    void testMetadataInitialization() {
        AuthEvent event = new AuthEvent();
        assertNotNull(event.getMetadata());

        // Should be able to set metadata fields
        event.getMetadata().setHostname("test-host");
        assertEquals("test-host", event.getMetadata().getHostname());
    }
}