#ifndef STREAMGUARD_EVENT_H
#define STREAMGUARD_EVENT_H

#include <string>
#include <map>
#include <nlohmann/json.hpp>

namespace streamguard {

// Event type enumeration
enum class EventType {
    AUTH_ATTEMPT,
    NETWORK_CONNECTION,
    FILE_ACCESS,
    PROCESS_EXECUTION,
    DNS_QUERY
};

// Event status enumeration
enum class EventStatus {
    SUCCESS,
    FAILED,
    BLOCKED,
    PENDING
};

// Metadata structure for event-specific details
struct EventMetadata {
    std::string user_agent;
    std::string geo_location;
    int port = 0;
    std::string protocol;
    std::string file_path;
    std::string process_name;
    std::string domain;
    int64_t bytes_transferred = 0;
    
    // Custom fields for extensibility
    std::map<std::string, std::string> custom_fields;
};

// Main Event structure
struct Event {
    std::string event_id;
    uint64_t timestamp;
    EventType event_type;
    std::string source_ip;
    std::string destination_ip;
    std::string user;
    EventStatus status;
    double threat_score;
    EventMetadata metadata;
    
    // Constructor
    Event() : timestamp(0), event_type(EventType::AUTH_ATTEMPT), 
              status(EventStatus::PENDING), threat_score(0.0) {}
    
    // Validate event data
    bool isValid() const;
    
    // Serialization methods
    std::string toJson() const;
    static Event fromJson(const std::string& json_str);
    static Event fromJson(const nlohmann::json& json_obj);
    
    // Utility methods
    std::string getEventTypeString() const;
    std::string getStatusString() const;
};

// Conversion functions for enums
std::string eventTypeToString(EventType type);
EventType stringToEventType(const std::string& str);
std::string eventStatusToString(EventStatus status);
EventStatus stringToEventStatus(const std::string& str);

// JSON serialization support using nlohmann/json
void to_json(nlohmann::json& j, const EventMetadata& meta);
void from_json(const nlohmann::json& j, EventMetadata& meta);
void to_json(nlohmann::json& j, const Event& event);
void from_json(const nlohmann::json& j, Event& event);

} // namespace streamguard

#endif // STREAMGUARD_EVENT_H