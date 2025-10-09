#include "event.h"
#include <stdexcept>
#include <regex>

namespace streamguard {

// Event type conversions
std::string eventTypeToString(EventType type) {
    switch (type) {
        case EventType::AUTH_ATTEMPT: return "auth_attempt";
        case EventType::NETWORK_CONNECTION: return "network_connection";
        case EventType::FILE_ACCESS: return "file_access";
        case EventType::PROCESS_EXECUTION: return "process_execution";
        case EventType::DNS_QUERY: return "dns_query";
        default: throw std::invalid_argument("Unknown event type");
    }
}

EventType stringToEventType(const std::string& str) {
    if (str == "auth_attempt") return EventType::AUTH_ATTEMPT;
    if (str == "network_connection") return EventType::NETWORK_CONNECTION;
    if (str == "file_access") return EventType::FILE_ACCESS;
    if (str == "process_execution") return EventType::PROCESS_EXECUTION;
    if (str == "dns_query") return EventType::DNS_QUERY;
    throw std::invalid_argument("Invalid event type: " + str);
}

// Event status conversions
std::string eventStatusToString(EventStatus status) {
    switch (status) {
        case EventStatus::SUCCESS: return "success";
        case EventStatus::FAILED: return "failed";
        case EventStatus::BLOCKED: return "blocked";
        case EventStatus::PENDING: return "pending";
        default: throw std::invalid_argument("Unknown event status");
    }
}

EventStatus stringToEventStatus(const std::string& str) {
    if (str == "success") return EventStatus::SUCCESS;
    if (str == "failed") return EventStatus::FAILED;
    if (str == "blocked") return EventStatus::BLOCKED;
    if (str == "pending") return EventStatus::PENDING;
    throw std::invalid_argument("Invalid event status: " + str);
}

// Event validation
bool Event::isValid() const {
    // Check required fields
    if (event_id.empty() || user.empty()) {
        return false;
    }
    
    // Validate event_id format: evt_XXXXXXXXXXXX
    std::regex event_id_pattern("^evt_[a-zA-Z0-9]{12}$");
    if (!std::regex_match(event_id, event_id_pattern)) {
        return false;
    }
    
    // Validate timestamp (reasonable range)
    if (timestamp < 1600000000000ULL || timestamp > 2000000000000ULL) {
        return false;
    }
    
    // Validate threat score range
    if (threat_score < 0.0 || threat_score > 1.0) {
        return false;
    }
    
    // Validate IP addresses (basic check)
    if (!source_ip.empty()) {
        std::regex ip_pattern("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        if (!std::regex_match(source_ip, ip_pattern)) {
            return false;
        }
    }
    
    return true;
}

std::string Event::getEventTypeString() const {
    return eventTypeToString(event_type);
}

std::string Event::getStatusString() const {
    return eventStatusToString(status);
}

// JSON serialization for EventMetadata
void to_json(nlohmann::json& j, const EventMetadata& meta) {
    j = nlohmann::json{};
    
    if (!meta.user_agent.empty()) j["user_agent"] = meta.user_agent;
    if (!meta.geo_location.empty()) j["geo_location"] = meta.geo_location;
    if (meta.port > 0) j["port"] = meta.port;
    if (!meta.protocol.empty()) j["protocol"] = meta.protocol;
    if (!meta.file_path.empty()) j["file_path"] = meta.file_path;
    if (!meta.process_name.empty()) j["process_name"] = meta.process_name;
    if (!meta.domain.empty()) j["domain"] = meta.domain;
    if (meta.bytes_transferred > 0) j["bytes_transferred"] = meta.bytes_transferred;
    
    // Add custom fields
    for (const auto& [key, value] : meta.custom_fields) {
        j[key] = value;
    }
}

void from_json(const nlohmann::json& j, EventMetadata& meta) {
    // Helper lambda to safely get string values (handle null)
    auto get_string_or_empty = [](const nlohmann::json& json_obj, const std::string& key) -> std::string {
        if (json_obj.contains(key) && !json_obj.at(key).is_null()) {
            return json_obj.at(key).get<std::string>();
        }
        return "";
    };

    meta.user_agent = get_string_or_empty(j, "user_agent");
    meta.geo_location = get_string_or_empty(j, "geo_location");
    meta.protocol = get_string_or_empty(j, "protocol");
    meta.file_path = get_string_or_empty(j, "file_path");
    meta.process_name = get_string_or_empty(j, "process_name");
    meta.domain = get_string_or_empty(j, "domain");

    if (j.contains("port") && !j.at("port").is_null()) {
        j.at("port").get_to(meta.port);
    }
    if (j.contains("bytes_transferred") && !j.at("bytes_transferred").is_null()) {
        j.at("bytes_transferred").get_to(meta.bytes_transferred);
    }
}

// JSON serialization for Event
void to_json(nlohmann::json& j, const Event& event) {
    j = nlohmann::json{
        {"event_id", event.event_id},
        {"timestamp", event.timestamp},
        {"event_type", eventTypeToString(event.event_type)},
        {"source_ip", event.source_ip},
        {"user", event.user},
        {"status", eventStatusToString(event.status)},
        {"threat_score", event.threat_score}
    };
    
    if (!event.destination_ip.empty()) {
        j["destination_ip"] = event.destination_ip;
    }
    
    // Add metadata if it has content
    nlohmann::json meta_json;
    to_json(meta_json, event.metadata);
    if (!meta_json.empty()) {
        j["metadata"] = meta_json;
    }
}

void from_json(const nlohmann::json& j, Event& event) {
    j.at("event_id").get_to(event.event_id);
    j.at("timestamp").get_to(event.timestamp);
    
    std::string type_str = j.at("event_type").get<std::string>();
    event.event_type = stringToEventType(type_str);
    
    j.at("source_ip").get_to(event.source_ip);
    j.at("user").get_to(event.user);

    // Status field - handle null values
    if (j.contains("status") && !j.at("status").is_null()) {
        std::string status_str = j.at("status").get<std::string>();
        event.status = stringToEventStatus(status_str);
    } else {
        event.status = EventStatus::SUCCESS;
    }

    j.at("threat_score").get_to(event.threat_score);

    // destination_ip is optional and may be null
    if (j.contains("destination_ip") && !j.at("destination_ip").is_null()) {
        j.at("destination_ip").get_to(event.destination_ip);
    }
    
    if (j.contains("metadata")) {
        from_json(j.at("metadata"), event.metadata);
    }
}

std::string Event::toJson() const {
    nlohmann::json j;
    to_json(j, *this);
    return j.dump();
}

Event Event::fromJson(const std::string& json_str) {
    nlohmann::json j = nlohmann::json::parse(json_str);
    return fromJson(j);
}

Event Event::fromJson(const nlohmann::json& json_obj) {
    Event event;
    from_json(json_obj, event);
    return event;
}

} // namespace streamguard