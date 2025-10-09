#include "event.h"
#include <gtest/gtest.h>
#include <string>

using namespace streamguard;

// Test event type conversions
TEST(EventTest, EventTypeConversions) {
    EXPECT_EQ(eventTypeToString(EventType::AUTH_ATTEMPT), "auth_attempt");
    EXPECT_EQ(eventTypeToString(EventType::NETWORK_CONNECTION), "network_connection");
    EXPECT_EQ(eventTypeToString(EventType::FILE_ACCESS), "file_access");
    EXPECT_EQ(eventTypeToString(EventType::PROCESS_EXECUTION), "process_execution");
    EXPECT_EQ(eventTypeToString(EventType::DNS_QUERY), "dns_query");
    
    EXPECT_EQ(stringToEventType("auth_attempt"), EventType::AUTH_ATTEMPT);
    EXPECT_EQ(stringToEventType("network_connection"), EventType::NETWORK_CONNECTION);
    EXPECT_EQ(stringToEventType("file_access"), EventType::FILE_ACCESS);
    EXPECT_EQ(stringToEventType("process_execution"), EventType::PROCESS_EXECUTION);
    EXPECT_EQ(stringToEventType("dns_query"), EventType::DNS_QUERY);
}

// Test event status conversions
TEST(EventTest, EventStatusConversions) {
    EXPECT_EQ(eventStatusToString(EventStatus::SUCCESS), "success");
    EXPECT_EQ(eventStatusToString(EventStatus::FAILED), "failed");
    EXPECT_EQ(eventStatusToString(EventStatus::BLOCKED), "blocked");
    EXPECT_EQ(eventStatusToString(EventStatus::PENDING), "pending");
    
    EXPECT_EQ(stringToEventStatus("success"), EventStatus::SUCCESS);
    EXPECT_EQ(stringToEventStatus("failed"), EventStatus::FAILED);
    EXPECT_EQ(stringToEventStatus("blocked"), EventStatus::BLOCKED);
    EXPECT_EQ(stringToEventStatus("pending"), EventStatus::PENDING);
}

// Test event validation
TEST(EventTest, EventValidation) {
    Event validEvent;
    validEvent.event_id = "evt_a1b2c3d4e5f6";
    validEvent.timestamp = 1704067200000;
    validEvent.event_type = EventType::AUTH_ATTEMPT;
    validEvent.source_ip = "192.168.1.100";
    validEvent.user = "alice";
    validEvent.status = EventStatus::SUCCESS;
    validEvent.threat_score = 0.5;
    
    EXPECT_TRUE(validEvent.isValid());
    
    // Test invalid event_id
    Event invalidId = validEvent;
    invalidId.event_id = "bad_id";
    EXPECT_FALSE(invalidId.isValid());
    
    // Test invalid timestamp
    Event invalidTimestamp = validEvent;
    invalidTimestamp.timestamp = 0;
    EXPECT_FALSE(invalidTimestamp.isValid());
    
    // Test invalid threat_score
    Event invalidScore = validEvent;
    invalidScore.threat_score = 1.5;
    EXPECT_FALSE(invalidScore.isValid());
}

// Test JSON serialization
TEST(EventTest, JsonSerialization) {
    Event event;
    event.event_id = "evt_test12345678";
    event.timestamp = 1704067200000;
    event.event_type = EventType::AUTH_ATTEMPT;
    event.source_ip = "192.168.1.100";
    event.destination_ip = "10.0.0.5";
    event.user = "alice";
    event.status = EventStatus::SUCCESS;
    event.threat_score = 0.05;
    event.metadata.user_agent = "Mozilla/5.0";
    event.metadata.geo_location = "US-MN-Minneapolis";
    
    // Serialize to JSON
    std::string json = event.toJson();
    
    // Verify JSON contains expected fields
    EXPECT_NE(json.find("evt_test12345678"), std::string::npos);
    EXPECT_NE(json.find("auth_attempt"), std::string::npos);
    EXPECT_NE(json.find("alice"), std::string::npos);
    EXPECT_NE(json.find("success"), std::string::npos);
}

// Test JSON deserialization
TEST(EventTest, JsonDeserialization) {
    std::string json = R"({
        "event_id": "evt_a1b2c3d4e5f6",
        "timestamp": 1704067200000,
        "event_type": "auth_attempt",
        "source_ip": "192.168.1.100",
        "destination_ip": "10.0.0.5",
        "user": "alice",
        "status": "success",
        "threat_score": 0.05,
        "metadata": {
            "user_agent": "Mozilla/5.0",
            "geo_location": "US-MN-Minneapolis"
        }
    })";
    
    Event event = Event::fromJson(json);
    
    EXPECT_EQ(event.event_id, "evt_a1b2c3d4e5f6");
    EXPECT_EQ(event.timestamp, 1704067200000);
    EXPECT_EQ(event.event_type, EventType::AUTH_ATTEMPT);
    EXPECT_EQ(event.source_ip, "192.168.1.100");
    EXPECT_EQ(event.destination_ip, "10.0.0.5");
    EXPECT_EQ(event.user, "alice");
    EXPECT_EQ(event.status, EventStatus::SUCCESS);
    EXPECT_DOUBLE_EQ(event.threat_score, 0.05);
    EXPECT_EQ(event.metadata.user_agent, "Mozilla/5.0");
    EXPECT_EQ(event.metadata.geo_location, "US-MN-Minneapolis");
}

// Test round-trip serialization
TEST(EventTest, RoundTripSerialization) {
    Event original;
    original.event_id = "evt_roundtrip123";
    original.timestamp = 1704067300000;
    original.event_type = EventType::NETWORK_CONNECTION;
    original.source_ip = "10.0.0.100";
    original.destination_ip = "185.220.101.5";
    original.user = "bob";
    original.status = EventStatus::BLOCKED;
    original.threat_score = 0.82;
    original.metadata.port = 9050;
    original.metadata.protocol = "TCP";
    original.metadata.bytes_transferred = 1048576;
    
    // Serialize
    std::string json = original.toJson();
    
    // Deserialize
    Event deserialized = Event::fromJson(json);
    
    // Verify
    EXPECT_EQ(original.event_id, deserialized.event_id);
    EXPECT_EQ(original.timestamp, deserialized.timestamp);
    EXPECT_EQ(original.event_type, deserialized.event_type);
    EXPECT_EQ(original.source_ip, deserialized.source_ip);
    EXPECT_EQ(original.destination_ip, deserialized.destination_ip);
    EXPECT_EQ(original.user, deserialized.user);
    EXPECT_EQ(original.status, deserialized.status);
    EXPECT_DOUBLE_EQ(original.threat_score, deserialized.threat_score);
    EXPECT_EQ(original.metadata.port, deserialized.metadata.port);
    EXPECT_EQ(original.metadata.protocol, deserialized.metadata.protocol);
    EXPECT_EQ(original.metadata.bytes_transferred, deserialized.metadata.bytes_transferred);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}