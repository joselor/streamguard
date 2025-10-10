#ifndef STREAMGUARD_ANOMALY_DETECTOR_H
#define STREAMGUARD_ANOMALY_DETECTOR_H

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include "event.h"

namespace streamguard {

/**
 * User behavior baseline for anomaly detection.
 *
 * Tracks normal behavior patterns for each user to detect anomalies.
 * Baselines are learned from the first N events per user.
 */
struct UserBaseline {
    std::string user;

    // Login time patterns (hour of day distribution)
    std::map<int, int> hourly_activity;  // hour -> event count

    // Source IP addresses seen
    std::map<std::string, int> source_ips;  // ip -> count

    // Geo-locations seen
    std::map<std::string, int> geo_locations;  // location -> count

    // Event type distribution
    std::map<EventType, int> event_types;  // type -> count

    // Failure patterns
    int total_events = 0;
    int failed_events = 0;

    // Baseline statistics
    double avg_threat_score = 0.0;
    bool is_baseline_ready = false;  // true after min_events_for_baseline

    // Update baseline with new event
    void update(const Event& event);

    // Calculate probability of hour (0.0-1.0)
    double getHourProbability(int hour) const;

    // Calculate probability of source IP (0.0-1.0)
    double getSourceIPProbability(const std::string& ip) const;

    // Calculate probability of geo-location (0.0-1.0)
    double getGeoLocationProbability(const std::string& location) const;

    // Calculate probability of event type (0.0-1.0)
    double getEventTypeProbability(EventType type) const;

    // Get failure rate (0.0-1.0)
    double getFailureRate() const;
};

/**
 * Anomaly detection result.
 */
struct AnomalyResult {
    std::string event_id;
    std::string user;
    uint64_t timestamp;
    double anomaly_score;  // 0.0-1.0 (higher = more anomalous)

    // Breakdown of anomaly factors
    double time_anomaly;     // Unusual hour of day
    double ip_anomaly;       // New/unknown source IP
    double location_anomaly; // New/unknown location
    double type_anomaly;     // Unusual event type
    double failure_anomaly;  // Spike in failures

    // Anomaly reasons (human-readable)
    std::vector<std::string> reasons;

    // Serialization
    std::string toJson() const;
    static AnomalyResult fromJson(const std::string& json_str);
};

/**
 * Statistical anomaly detection engine.
 *
 * Detects anomalies based on deviations from learned user baselines.
 * Uses a simple probabilistic model:
 * - Low probability events = high anomaly score
 * - New behaviors (unseen IPs, locations) = high anomaly score
 * - Spikes in failures = high anomaly score
 *
 * Thread-safe: All operations are protected by mutex.
 */
class AnomalyDetector {
public:
    /**
     * Constructor.
     *
     * @param min_events_for_baseline Minimum events needed to establish baseline (default: 100)
     */
    explicit AnomalyDetector(size_t min_events_for_baseline = 100);

    /**
     * Analyze an event for anomalies.
     *
     * If user has sufficient baseline, calculates anomaly score.
     * Otherwise, updates baseline and returns no anomaly.
     *
     * @param event The event to analyze
     * @return Optional AnomalyResult if anomaly detected (score > threshold)
     */
    std::optional<AnomalyResult> analyze(const Event& event);

    /**
     * Get baseline for a user.
     *
     * @param user Username
     * @return Optional baseline if exists
     */
    std::optional<UserBaseline> getBaseline(const std::string& user) const;

    /**
     * Get number of users with baselines.
     *
     * @return Count of users being tracked
     */
    size_t getUserCount() const;

    /**
     * Set anomaly score threshold for reporting.
     *
     * @param threshold Minimum score to report (default: 0.7)
     */
    void setThreshold(double threshold);

    /**
     * Get current threshold.
     *
     * @return Current anomaly threshold
     */
    double getThreshold() const;

private:
    /**
     * Calculate anomaly score for an event.
     *
     * @param event The event to score
     * @param baseline User's baseline
     * @return AnomalyResult with detailed scoring
     */
    AnomalyResult calculateAnomalyScore(const Event& event, const UserBaseline& baseline);

    /**
     * Extract hour from timestamp (0-23).
     *
     * @param timestamp Milliseconds since epoch
     * @return Hour of day
     */
    int extractHour(uint64_t timestamp) const;

    // User baselines (user -> baseline)
    std::map<std::string, UserBaseline> baselines_;

    // Configuration
    size_t min_events_for_baseline_;
    double threshold_;

    // Thread safety
    mutable std::mutex mutex_;
};

} // namespace streamguard

#endif // STREAMGUARD_ANOMALY_DETECTOR_H
