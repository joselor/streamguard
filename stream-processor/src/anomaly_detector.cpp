#include "anomaly_detector.h"
#include <nlohmann/json.hpp>
#include <cmath>
#include <algorithm>
#include <sstream>
#include <iomanip>
#include <chrono>

namespace streamguard {

// ========== UserBaseline Implementation ==========

void UserBaseline::update(const Event& event) {
    // Extract hour from timestamp
    auto time_t_val = static_cast<time_t>(event.timestamp / 1000);
    struct tm* tm_info = std::localtime(&time_t_val);
    int hour = tm_info->tm_hour;

    // Update hourly activity
    hourly_activity[hour]++;

    // Update source IPs
    source_ips[event.source_ip]++;

    // Update geo-locations
    if (!event.metadata.geo_location.empty()) {
        geo_locations[event.metadata.geo_location]++;
    }

    // Update event types
    event_types[event.event_type]++;

    // Update failure tracking
    total_events++;
    if (event.status == EventStatus::FAILED) {
        failed_events++;
    }

    // Update average threat score (running average)
    avg_threat_score = (avg_threat_score * (total_events - 1) + event.threat_score) / total_events;
}

double UserBaseline::getHourProbability(int hour) const {
    if (total_events == 0) return 0.5;  // Unknown, neutral

    auto it = hourly_activity.find(hour);
    if (it == hourly_activity.end()) {
        return 0.01;  // Very rare (unseen hour)
    }

    return static_cast<double>(it->second) / total_events;
}

double UserBaseline::getSourceIPProbability(const std::string& ip) const {
    if (total_events == 0) return 0.5;  // Unknown, neutral

    auto it = source_ips.find(ip);
    if (it == source_ips.end()) {
        return 0.01;  // Very rare (new IP)
    }

    return static_cast<double>(it->second) / total_events;
}

double UserBaseline::getGeoLocationProbability(const std::string& location) const {
    if (total_events == 0 || location.empty()) return 0.5;  // Unknown, neutral

    auto it = geo_locations.find(location);
    if (it == geo_locations.end()) {
        return 0.01;  // Very rare (new location)
    }

    return static_cast<double>(it->second) / total_events;
}

double UserBaseline::getEventTypeProbability(EventType type) const {
    if (total_events == 0) return 0.5;  // Unknown, neutral

    auto it = event_types.find(type);
    if (it == event_types.end()) {
        return 0.01;  // Very rare (new event type)
    }

    return static_cast<double>(it->second) / total_events;
}

double UserBaseline::getFailureRate() const {
    if (total_events == 0) return 0.0;
    return static_cast<double>(failed_events) / total_events;
}

// ========== AnomalyResult Implementation ==========

std::string AnomalyResult::toJson() const {
    nlohmann::json j;
    j["event_id"] = event_id;
    j["user"] = user;
    j["timestamp"] = timestamp;
    j["anomaly_score"] = anomaly_score;
    j["time_anomaly"] = time_anomaly;
    j["ip_anomaly"] = ip_anomaly;
    j["location_anomaly"] = location_anomaly;
    j["type_anomaly"] = type_anomaly;
    j["failure_anomaly"] = failure_anomaly;
    j["reasons"] = reasons;
    return j.dump();
}

AnomalyResult AnomalyResult::fromJson(const std::string& json_str) {
    nlohmann::json j = nlohmann::json::parse(json_str);
    AnomalyResult result;
    result.event_id = j.value("event_id", "");
    result.user = j.value("user", "");
    result.timestamp = j.value("timestamp", 0ULL);
    result.anomaly_score = j.value("anomaly_score", 0.0);
    result.time_anomaly = j.value("time_anomaly", 0.0);
    result.ip_anomaly = j.value("ip_anomaly", 0.0);
    result.location_anomaly = j.value("location_anomaly", 0.0);
    result.type_anomaly = j.value("type_anomaly", 0.0);
    result.failure_anomaly = j.value("failure_anomaly", 0.0);
    if (j.contains("reasons")) {
        result.reasons = j["reasons"].get<std::vector<std::string>>();
    }
    return result;
}

// ========== AnomalyDetector Implementation ==========

AnomalyDetector::AnomalyDetector(size_t min_events_for_baseline)
    : min_events_for_baseline_(min_events_for_baseline)
    , threshold_(0.7)
{
}

std::optional<AnomalyResult> AnomalyDetector::analyze(const Event& event) {
    std::lock_guard<std::mutex> lock(mutex_);

    // Get or create baseline for user
    auto& baseline = baselines_[event.user];
    baseline.user = event.user;

    // If baseline not ready, just update and return
    if (!baseline.is_baseline_ready) {
        baseline.update(event);

        // Check if we have enough events to establish baseline
        if (static_cast<size_t>(baseline.total_events) >= min_events_for_baseline_) {
            baseline.is_baseline_ready = true;
        }

        return std::nullopt;  // Not enough data yet
    }

    // Calculate anomaly score
    AnomalyResult result = calculateAnomalyScore(event, baseline);

    // Update baseline with new event (continuous learning)
    baseline.update(event);

    // Return anomaly if score exceeds threshold
    if (result.anomaly_score >= threshold_) {
        return result;
    }

    return std::nullopt;
}

AnomalyResult AnomalyDetector::calculateAnomalyScore(const Event& event, const UserBaseline& baseline) {
    AnomalyResult result;
    result.event_id = event.event_id;
    result.user = event.user;
    result.timestamp = event.timestamp;

    // 1. Time-based anomaly (unusual hour)
    int hour = extractHour(event.timestamp);
    double hour_prob = baseline.getHourProbability(hour);
    result.time_anomaly = 1.0 - hour_prob;  // Low probability = high anomaly

    if (hour_prob < 0.05) {
        std::ostringstream oss;
        oss << "Unusual login time (" << std::setfill('0') << std::setw(2) << hour << ":00)";
        result.reasons.push_back(oss.str());
    }

    // 2. Source IP anomaly (new or rare IP)
    double ip_prob = baseline.getSourceIPProbability(event.source_ip);
    result.ip_anomaly = 1.0 - ip_prob;

    if (ip_prob < 0.05) {
        result.reasons.push_back("New or unusual source IP: " + event.source_ip);
    }

    // 3. Geo-location anomaly (new or rare location)
    double location_prob = baseline.getGeoLocationProbability(event.metadata.geo_location);
    result.location_anomaly = 1.0 - location_prob;

    if (location_prob < 0.05 && !event.metadata.geo_location.empty()) {
        result.reasons.push_back("New or unusual location: " + event.metadata.geo_location);
    }

    // 4. Event type anomaly (unusual type for this user)
    double type_prob = baseline.getEventTypeProbability(event.event_type);
    result.type_anomaly = 1.0 - type_prob;

    if (type_prob < 0.05) {
        result.reasons.push_back("Unusual event type: " + eventTypeToString(event.event_type));
    }

    // 5. Failure anomaly (check if this is a failure and if failures are spiking)
    double failure_rate = baseline.getFailureRate();
    result.failure_anomaly = 0.0;

    if (event.status == EventStatus::FAILED) {
        // If this user rarely fails, flag it
        if (failure_rate < 0.1) {
            result.failure_anomaly = 0.8;  // High anomaly for rare failures
            result.reasons.push_back("User typically has low failure rate");
        }
    }

    // Calculate composite anomaly score (weighted average)
    // Time and IP are most important, so weight them higher
    result.anomaly_score = (
        result.time_anomaly * 0.25 +
        result.ip_anomaly * 0.30 +
        result.location_anomaly * 0.20 +
        result.type_anomaly * 0.15 +
        result.failure_anomaly * 0.10
    );

    // Clamp to [0.0, 1.0]
    result.anomaly_score = std::max(0.0, std::min(1.0, result.anomaly_score));

    // Add overall message if high score
    if (result.anomaly_score >= 0.9) {
        result.reasons.insert(result.reasons.begin(), "CRITICAL: Highly anomalous behavior detected");
    } else if (result.anomaly_score >= 0.7) {
        result.reasons.insert(result.reasons.begin(), "HIGH: Significant deviation from normal behavior");
    }

    return result;
}

int AnomalyDetector::extractHour(uint64_t timestamp) const {
    auto time_t_val = static_cast<time_t>(timestamp / 1000);
    struct tm* tm_info = std::localtime(&time_t_val);
    return tm_info->tm_hour;
}

std::optional<UserBaseline> AnomalyDetector::getBaseline(const std::string& user) const {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = baselines_.find(user);
    if (it != baselines_.end()) {
        return it->second;
    }

    return std::nullopt;
}

size_t AnomalyDetector::getUserCount() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return baselines_.size();
}

void AnomalyDetector::setThreshold(double threshold) {
    std::lock_guard<std::mutex> lock(mutex_);
    threshold_ = std::max(0.0, std::min(1.0, threshold));
}

double AnomalyDetector::getThreshold() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return threshold_;
}

} // namespace streamguard
