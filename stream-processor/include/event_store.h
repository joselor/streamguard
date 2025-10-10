#ifndef STREAMGUARD_EVENT_STORE_H
#define STREAMGUARD_EVENT_STORE_H

#include <string>
#include <vector>
#include <memory>
#include <optional>
#include <rocksdb/db.h>
#include "event.h"
#include "ai_analyzer.h"

namespace streamguard {

// Forward declaration
struct AnomalyResult;

/**
 * Time-series event storage using RocksDB.
 *
 * This class provides persistent storage for security events with efficient
 * time-based queries. Events are stored with composite keys that enable:
 * - Range queries by event type and time range
 * - Fast lookups by event ID
 * - Ordered iteration through events
 *
 * Key Format: event_type:timestamp:event_id
 * Example: "auth_attempt:1760043114588:evt_abc123..."
 *
 * This key design enables efficient queries like:
 * - All auth events in the last hour
 * - All events between timestamps X and Y
 * - Latest N events of a specific type
 *
 * Thread Safety: All operations are thread-safe via RocksDB's internal locking
 *
 * RAII: Database is automatically closed on destruction
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class EventStore {
public:
    /**
     * Opens or creates a RocksDB database at the specified path.
     *
     * @param dbPath Path to the database directory
     * @throws std::runtime_error if database cannot be opened
     */
    explicit EventStore(const std::string& dbPath);

    /**
     * Destructor - automatically closes the database.
     */
    ~EventStore();

    // Disable copy (RocksDB::DB is non-copyable)
    EventStore(const EventStore&) = delete;
    EventStore& operator=(const EventStore&) = delete;

    // Enable move semantics
    EventStore(EventStore&& other) noexcept;
    EventStore& operator=(EventStore&& other) noexcept;

    /**
     * Stores an event in the database.
     *
     * Key format: event_type:timestamp:event_id
     * Value: JSON serialized event
     *
     * @param event The event to store
     * @return true if successful, false otherwise
     */
    bool put(const Event& event);

    /**
     * Retrieves an event by its ID.
     *
     * Note: This is less efficient than range queries as it requires
     * scanning, but useful for point lookups.
     *
     * @param eventId The event ID to look up
     * @param event Output parameter for the found event
     * @return true if found, false otherwise
     */
    bool get(const std::string& eventId, Event& event);

    /**
     * Retrieves events within a time range for a specific event type.
     *
     * This is the most efficient query pattern due to the key design.
     *
     * @param eventType The event type to filter by
     * @param startTime Start timestamp (inclusive, milliseconds since epoch)
     * @param endTime End timestamp (inclusive, milliseconds since epoch)
     * @param limit Maximum number of events to return (0 = no limit)
     * @return Vector of events matching the criteria
     */
    std::vector<Event> getByTimeRange(
        EventType eventType,
        uint64_t startTime,
        uint64_t endTime,
        size_t limit = 0
    );

    /**
     * Retrieves the latest N events of a specific type.
     *
     * @param eventType The event type to filter by
     * @param limit Maximum number of events to return
     * @return Vector of latest events (most recent first)
     */
    std::vector<Event> getLatest(EventType eventType, size_t limit);

    /**
     * Returns the total number of events stored.
     *
     * Note: This operation scans the entire database and may be slow.
     *
     * @return Total event count
     */
    uint64_t getEventCount();

    /**
     * Returns database statistics and properties.
     *
     * @return String containing database stats (size, keys, etc.)
     */
    std::string getStats();

    /**
     * Deletes all events older than the specified timestamp.
     *
     * This is useful for implementing data retention policies.
     *
     * @param timestamp Events older than this will be deleted
     * @return Number of events deleted
     */
    uint64_t deleteOlderThan(uint64_t timestamp);

    /**
     * Stores an AI threat analysis result.
     *
     * Key format: event_id
     * Value: JSON serialized ThreatAnalysis
     *
     * @param analysis The threat analysis to store
     * @return true if successful, false otherwise
     */
    bool putAnalysis(const ThreatAnalysis& analysis);

    /**
     * Retrieves an AI analysis by event ID.
     *
     * @param eventId The event ID to look up
     * @return Optional ThreatAnalysis if found
     */
    std::optional<ThreatAnalysis> getAnalysis(const std::string& eventId);

    /**
     * Retrieves AI analyses within a time range.
     *
     * @param startTime Start timestamp (inclusive, seconds since epoch)
     * @param endTime End timestamp (inclusive, seconds since epoch)
     * @param limit Maximum number of analyses to return (0 = no limit)
     * @return Vector of analyses matching the criteria
     */
    std::vector<ThreatAnalysis> getAnalysesByTimeRange(
        uint64_t startTime,
        uint64_t endTime,
        size_t limit = 0
    );

    /**
     * Retrieves analyses by severity level.
     *
     * @param severity Severity level (CRITICAL, HIGH, MEDIUM, LOW)
     * @param limit Maximum number of analyses to return (0 = no limit)
     * @return Vector of analyses with specified severity
     */
    std::vector<ThreatAnalysis> getAnalysesBySeverity(
        const std::string& severity,
        size_t limit = 0
    );

    /**
     * Returns the total number of AI analyses stored.
     *
     * @return Total analysis count
     */
    uint64_t getAnalysisCount();

    /**
     * Stores an embedding vector for an event.
     *
     * Key format: event_id
     * Value: Binary serialized embedding vector (float array)
     *
     * @param eventId The event ID
     * @param embedding The embedding vector to store
     * @return true if successful, false otherwise
     */
    bool putEmbedding(const std::string& eventId, const std::vector<float>& embedding);

    /**
     * Retrieves an embedding vector by event ID.
     *
     * @param eventId The event ID to look up
     * @return Optional embedding vector if found
     */
    std::optional<std::vector<float>> getEmbedding(const std::string& eventId);

    /**
     * Find top-K similar events based on embedding similarity.
     *
     * @param queryEmbedding The query embedding vector
     * @param k Number of similar events to return
     * @return Vector of (event_id, similarity_score) pairs, sorted by similarity
     */
    std::vector<std::pair<std::string, double>> findSimilar(
        const std::vector<float>& queryEmbedding,
        size_t k = 5
    );

    /**
     * Returns the total number of embeddings stored.
     *
     * @return Total embedding count
     */
    uint64_t getEmbeddingCount();

    /**
     * Stores an anomaly detection result.
     *
     * Key format: timestamp:event_id
     * Value: JSON serialized AnomalyResult
     *
     * @param anomaly The anomaly result to store
     * @return true if successful, false otherwise
     */
    bool putAnomaly(const struct AnomalyResult& anomaly);

    /**
     * Retrieves an anomaly by event ID.
     *
     * @param eventId The event ID to look up
     * @return Optional AnomalyResult if found
     */
    std::optional<struct AnomalyResult> getAnomaly(const std::string& eventId);

    /**
     * Retrieves anomalies within a time range.
     *
     * @param startTime Start timestamp (inclusive, milliseconds since epoch)
     * @param endTime End timestamp (inclusive, milliseconds since epoch)
     * @param limit Maximum number of anomalies to return (0 = no limit)
     * @return Vector of anomalies matching the criteria
     */
    std::vector<struct AnomalyResult> getAnomaliesByTimeRange(
        uint64_t startTime,
        uint64_t endTime,
        size_t limit = 0
    );

    /**
     * Retrieves anomalies by user.
     *
     * @param user Username
     * @param limit Maximum number of anomalies to return (0 = no limit)
     * @return Vector of anomalies for the user
     */
    std::vector<struct AnomalyResult> getAnomaliesByUser(
        const std::string& user,
        size_t limit = 0
    );

    /**
     * Retrieves high-score anomalies (score > threshold).
     *
     * @param threshold Minimum anomaly score (default: 0.7)
     * @param limit Maximum number of anomalies to return (0 = no limit)
     * @return Vector of high-score anomalies
     */
    std::vector<struct AnomalyResult> getHighScoreAnomalies(
        double threshold = 0.7,
        size_t limit = 0
    );

    /**
     * Returns the total number of anomalies stored.
     *
     * @return Total anomaly count
     */
    uint64_t getAnomalyCount();

private:
    /**
     * Generates a composite key for storage.
     *
     * @param event The event to generate a key for
     * @return Composite key string
     */
    std::string makeKey(const Event& event);

    /**
     * Generates a key prefix for range queries.
     *
     * @param eventType Event type for the prefix
     * @param timestamp Optional timestamp for more specific prefix
     * @return Key prefix string
     */
    std::string makeKeyPrefix(EventType eventType, uint64_t timestamp = 0);

    std::unique_ptr<rocksdb::DB> db_;
    std::string dbPath_;
    rocksdb::ColumnFamilyHandle* ai_analysis_cf_;  // Column family for AI analyses
    rocksdb::ColumnFamilyHandle* embeddings_cf_;   // Column family for embeddings
    rocksdb::ColumnFamilyHandle* anomalies_cf_;    // Column family for anomalies
};

} // namespace streamguard

#endif // STREAMGUARD_EVENT_STORE_H
