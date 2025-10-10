#include "event_store.h"
#include "anomaly_detector.h"
#include <rocksdb/options.h>
#include <rocksdb/slice.h>
#include <rocksdb/table.h>
#include <rocksdb/filter_policy.h>
#include <rocksdb/cache.h>
#include <iostream>
#include <sstream>
#include <iomanip>
#include <algorithm>
#include <cstring>

namespace streamguard {

EventStore::EventStore(const std::string& dbPath) : dbPath_(dbPath), ai_analysis_cf_(nullptr), embeddings_cf_(nullptr), anomalies_cf_(nullptr) {
    rocksdb::Options options;

    // Create database if it doesn't exist
    options.create_if_missing = true;
    options.create_missing_column_families = true;

    // Optimize for time-series workload
    options.compression = rocksdb::kSnappyCompression;
    options.write_buffer_size = 64 * 1024 * 1024;  // 64MB write buffer
    options.max_write_buffer_number = 3;
    options.target_file_size_base = 64 * 1024 * 1024;  // 64MB SST files

    // Enable bloom filters for faster lookups
    rocksdb::BlockBasedTableOptions table_options;
    table_options.filter_policy.reset(rocksdb::NewBloomFilterPolicy(10, false));
    table_options.block_cache = rocksdb::NewLRUCache(256 * 1024 * 1024);  // 256MB cache
    options.table_factory.reset(rocksdb::NewBlockBasedTableFactory(table_options));

    // Define column families
    std::vector<rocksdb::ColumnFamilyDescriptor> column_families;
    column_families.push_back(rocksdb::ColumnFamilyDescriptor(
        rocksdb::kDefaultColumnFamilyName, rocksdb::ColumnFamilyOptions(options)));
    column_families.push_back(rocksdb::ColumnFamilyDescriptor(
        "ai_analysis", rocksdb::ColumnFamilyOptions(options)));
    column_families.push_back(rocksdb::ColumnFamilyDescriptor(
        "embeddings", rocksdb::ColumnFamilyOptions(options)));
    column_families.push_back(rocksdb::ColumnFamilyDescriptor(
        "anomalies", rocksdb::ColumnFamilyOptions(options)));

    // Open database with column families
    std::vector<rocksdb::ColumnFamilyHandle*> handles;
    rocksdb::DB* db_ptr;
    rocksdb::Status status = rocksdb::DB::Open(options, dbPath, column_families, &handles, &db_ptr);

    if (!status.ok()) {
        throw std::runtime_error("Failed to open RocksDB: " + status.ToString());
    }

    db_.reset(db_ptr);
    ai_analysis_cf_ = handles[1];  // ai_analysis column family
    embeddings_cf_ = handles[2];   // embeddings column family
    anomalies_cf_ = handles[3];    // anomalies column family

    // Note: handles[0] is default column family (events), owned by db_
    // We store handles for column families we query separately

    std::cout << "[EventStore] Database opened at: " << dbPath << std::endl;
    std::cout << "[EventStore] Column families: default (events), ai_analysis, embeddings, anomalies" << std::endl;
}

EventStore::~EventStore() {
    if (db_) {
        std::cout << "[EventStore] Closing database..." << std::endl;

        // Delete column family handles before closing DB
        if (ai_analysis_cf_) {
            delete ai_analysis_cf_;
            ai_analysis_cf_ = nullptr;
        }
        if (embeddings_cf_) {
            delete embeddings_cf_;
            embeddings_cf_ = nullptr;
        }
        if (anomalies_cf_) {
            delete anomalies_cf_;
            anomalies_cf_ = nullptr;
        }

        db_.reset();  // Unique_ptr will call delete and close the DB
    }
}

EventStore::EventStore(EventStore&& other) noexcept
    : db_(std::move(other.db_)),
      dbPath_(std::move(other.dbPath_)),
      ai_analysis_cf_(other.ai_analysis_cf_),
      embeddings_cf_(other.embeddings_cf_),
      anomalies_cf_(other.anomalies_cf_) {
    other.ai_analysis_cf_ = nullptr;
    other.embeddings_cf_ = nullptr;
    other.anomalies_cf_ = nullptr;
}

EventStore& EventStore::operator=(EventStore&& other) noexcept {
    if (this != &other) {
        // Delete existing column family handles
        if (ai_analysis_cf_) {
            delete ai_analysis_cf_;
        }
        if (embeddings_cf_) {
            delete embeddings_cf_;
        }
        if (anomalies_cf_) {
            delete anomalies_cf_;
        }

        db_ = std::move(other.db_);
        dbPath_ = std::move(other.dbPath_);
        ai_analysis_cf_ = other.ai_analysis_cf_;
        embeddings_cf_ = other.embeddings_cf_;
        anomalies_cf_ = other.anomalies_cf_;
        other.ai_analysis_cf_ = nullptr;
        other.embeddings_cf_ = nullptr;
        other.anomalies_cf_ = nullptr;
    }
    return *this;
}

std::string EventStore::makeKey(const Event& event) {
    // Key format: event_type:timestamp:event_id
    // Example: "auth_attempt:001760043114588:evt_abc123..."
    //
    // Timestamp is zero-padded to 15 digits to ensure correct lexicographic ordering
    // This allows efficient range scans by time
    std::ostringstream oss;
    oss << eventTypeToString(event.event_type) << ":"
        << std::setfill('0') << std::setw(15) << event.timestamp << ":"
        << event.event_id;
    return oss.str();
}

std::string EventStore::makeKeyPrefix(EventType eventType, uint64_t timestamp) {
    std::ostringstream oss;
    oss << eventTypeToString(eventType) << ":";

    if (timestamp > 0) {
        oss << std::setfill('0') << std::setw(15) << timestamp << ":";
    }

    return oss.str();
}

bool EventStore::put(const Event& event) {
    if (!db_) {
        return false;
    }

    try {
        std::string key = makeKey(event);
        std::string value = event.toJson();

        rocksdb::WriteOptions write_options;
        write_options.sync = false;  // Async writes for better performance

        rocksdb::Status status = db_->Put(write_options, key, value);

        if (!status.ok()) {
            std::cerr << "[EventStore] Failed to put event: " << status.ToString() << std::endl;
            return false;
        }

        return true;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in put: " << e.what() << std::endl;
        return false;
    }
}

bool EventStore::get(const std::string& eventId, Event& event) {
    if (!db_) {
        return false;
    }

    try {
        // Since we don't know the timestamp, we need to scan
        // This is less efficient but necessary for ID-based lookups
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string key = it->key().ToString();

            // Check if this key ends with our event ID
            if (key.find(":" + eventId) != std::string::npos) {
                std::string value = it->value().ToString();
                event = Event::fromJson(value);
                return true;
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

        return false;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in get: " << e.what() << std::endl;
        return false;
    }
}

std::vector<Event> EventStore::getByTimeRange(
    EventType eventType,
    uint64_t startTime,
    uint64_t endTime,
    size_t limit
) {
    std::vector<Event> events;

    if (!db_) {
        return events;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options));

        // Create key prefix for the event type and start time
        std::string startKey = makeKeyPrefix(eventType, startTime);
        std::string endPrefix = eventTypeToString(eventType) + ":";

        std::ostringstream endKeyStream;
        endKeyStream << endPrefix << std::setfill('0') << std::setw(15) << endTime << ":";
        std::string endKey = endKeyStream.str();

        // Seek to the start position
        it->Seek(startKey);

        while (it->Valid()) {
            std::string key = it->key().ToString();

            // Check if we've moved past our event type or time range
            if (key.compare(0, endPrefix.length(), endPrefix) != 0 || key > endKey + "~") {
                break;
            }

            // Only include keys that match our time range
            if (key >= startKey && key <= endKey + "~") {
                std::string value = it->value().ToString();
                Event event = Event::fromJson(value);
                events.push_back(event);

                if (limit > 0 && events.size() >= limit) {
                    break;
                }
            }

            it->Next();
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getByTimeRange: " << e.what() << std::endl;
    }

    return events;
}

std::vector<Event> EventStore::getLatest(EventType eventType, size_t limit) {
    std::vector<Event> events;

    if (!db_) {
        return events;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options));

        std::string prefix = makeKeyPrefix(eventType);

        // Seek to the last event of this type by seeking past it
        std::string seekKey = eventTypeToString(eventType) + ";";  // Next type lexicographically
        it->SeekForPrev(seekKey);

        // Move backwards collecting events
        while (it->Valid() && events.size() < limit) {
            std::string key = it->key().ToString();

            if (key.compare(0, prefix.length(), prefix) != 0) {
                break;
            }

            std::string value = it->value().ToString();
            Event event = Event::fromJson(value);
            events.push_back(event);

            it->Prev();
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getLatest: " << e.what() << std::endl;
    }

    return events;
}

uint64_t EventStore::getEventCount() {
    if (!db_) {
        return 0;
    }

    uint64_t count = 0;
    rocksdb::ReadOptions read_options;
    std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options));

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        count++;
    }

    return count;
}

std::string EventStore::getStats() {
    if (!db_) {
        return "Database not open";
    }

    std::string stats;

    // Get database properties
    db_->GetProperty("rocksdb.stats", &stats);

    // Get approximate sizes
    std::string size_str;
    db_->GetProperty("rocksdb.total-sst-files-size", &size_str);

    std::ostringstream oss;
    oss << "=== EventStore Statistics ===" << std::endl;
    oss << "Database path: " << dbPath_ << std::endl;
    oss << "Total events: " << getEventCount() << std::endl;
    oss << "SST files size: " << size_str << " bytes" << std::endl;
    oss << std::endl;
    oss << "RocksDB Stats:" << std::endl;
    oss << stats;

    return oss.str();
}

uint64_t EventStore::deleteOlderThan(uint64_t timestamp) {
    if (!db_) {
        return 0;
    }

    uint64_t deleted = 0;

    try {
        rocksdb::ReadOptions read_options;
        rocksdb::WriteOptions write_options;
        write_options.sync = false;

        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options));

        // Iterate through all keys
        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string key = it->key().ToString();

            // Extract timestamp from key (format: type:timestamp:id)
            size_t first_colon = key.find(':');
            size_t second_colon = key.find(':', first_colon + 1);

            if (first_colon != std::string::npos && second_colon != std::string::npos) {
                std::string timestamp_str = key.substr(first_colon + 1, second_colon - first_colon - 1);
                uint64_t event_timestamp = std::stoull(timestamp_str);

                if (event_timestamp < timestamp) {
                    rocksdb::Status status = db_->Delete(write_options, key);
                    if (status.ok()) {
                        deleted++;
                    }
                }
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in deleteOlderThan: " << e.what() << std::endl;
    }

    return deleted;
}

// AI Analysis storage methods

bool EventStore::putAnalysis(const ThreatAnalysis& analysis) {
    if (!db_ || !ai_analysis_cf_) {
        return false;
    }

    try {
        // Key format: event_id
        std::string key = analysis.event_id;
        std::string value = analysis.toJson();

        rocksdb::WriteOptions write_options;
        write_options.sync = false;  // Async writes for better performance

        rocksdb::Status status = db_->Put(write_options, ai_analysis_cf_, key, value);

        if (!status.ok()) {
            std::cerr << "[EventStore] Failed to put AI analysis: " << status.ToString() << std::endl;
            return false;
        }

        return true;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in putAnalysis: " << e.what() << std::endl;
        return false;
    }
}

std::optional<ThreatAnalysis> EventStore::getAnalysis(const std::string& eventId) {
    if (!db_ || !ai_analysis_cf_) {
        return std::nullopt;
    }

    try {
        std::string value;
        rocksdb::ReadOptions read_options;
        rocksdb::Status status = db_->Get(read_options, ai_analysis_cf_, eventId, &value);

        if (status.ok()) {
            return ThreatAnalysis::fromJson(value);
        } else if (status.IsNotFound()) {
            return std::nullopt;
        } else {
            std::cerr << "[EventStore] Failed to get AI analysis: " << status.ToString() << std::endl;
            return std::nullopt;
        }
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnalysis: " << e.what() << std::endl;
        return std::nullopt;
    }
}

std::vector<ThreatAnalysis> EventStore::getAnalysesByTimeRange(
    uint64_t startTime,
    uint64_t endTime,
    size_t limit
) {
    std::vector<ThreatAnalysis> analyses;

    if (!db_ || !ai_analysis_cf_) {
        return analyses;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, ai_analysis_cf_));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string value = it->value().ToString();
            auto analysis = ThreatAnalysis::fromJson(value);

            if (analysis) {
                // Filter by timestamp
                if (analysis->timestamp >= static_cast<int64_t>(startTime) &&
                    analysis->timestamp <= static_cast<int64_t>(endTime)) {
                    analyses.push_back(*analysis);

                    if (limit > 0 && analyses.size() >= limit) {
                        break;
                    }
                }
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnalysesByTimeRange: " << e.what() << std::endl;
    }

    return analyses;
}

std::vector<ThreatAnalysis> EventStore::getAnalysesBySeverity(
    const std::string& severity,
    size_t limit
) {
    std::vector<ThreatAnalysis> analyses;

    if (!db_ || !ai_analysis_cf_) {
        return analyses;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, ai_analysis_cf_));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string value = it->value().ToString();
            auto analysis = ThreatAnalysis::fromJson(value);

            if (analysis && analysis->severity == severity) {
                analyses.push_back(*analysis);

                if (limit > 0 && analyses.size() >= limit) {
                    break;
                }
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnalysesBySeverity: " << e.what() << std::endl;
    }

    return analyses;
}

uint64_t EventStore::getAnalysisCount() {
    if (!db_ || !ai_analysis_cf_) {
        return 0;
    }

    uint64_t count = 0;
    rocksdb::ReadOptions read_options;
    std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, ai_analysis_cf_));

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        count++;
    }

    return count;
}

// Embedding storage methods

bool EventStore::putEmbedding(const std::string& eventId, const std::vector<float>& embedding) {
    if (!db_ || !embeddings_cf_) {
        return false;
    }

    try {
        // Serialize float vector to binary blob
        // Simple binary serialization: convert float array to byte string
        std::string value(reinterpret_cast<const char*>(embedding.data()),
                         embedding.size() * sizeof(float));

        rocksdb::WriteOptions write_options;
        write_options.sync = false;  // Async writes for better performance

        rocksdb::Status status = db_->Put(write_options, embeddings_cf_, eventId, value);

        if (!status.ok()) {
            std::cerr << "[EventStore] Failed to put embedding: " << status.ToString() << std::endl;
            return false;
        }

        return true;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in putEmbedding: " << e.what() << std::endl;
        return false;
    }
}

std::optional<std::vector<float>> EventStore::getEmbedding(const std::string& eventId) {
    if (!db_ || !embeddings_cf_) {
        return std::nullopt;
    }

    try {
        std::string value;
        rocksdb::ReadOptions read_options;
        rocksdb::Status status = db_->Get(read_options, embeddings_cf_, eventId, &value);

        if (status.ok()) {
            // Deserialize binary blob to float vector
            size_t num_floats = value.size() / sizeof(float);
            std::vector<float> embedding(num_floats);
            std::memcpy(embedding.data(), value.data(), value.size());
            return embedding;
        } else if (status.IsNotFound()) {
            return std::nullopt;
        } else {
            std::cerr << "[EventStore] Failed to get embedding: " << status.ToString() << std::endl;
            return std::nullopt;
        }
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getEmbedding: " << e.what() << std::endl;
        return std::nullopt;
    }
}

std::vector<std::pair<std::string, double>> EventStore::findSimilar(
    const std::vector<float>& queryEmbedding,
    size_t k
) {
    std::vector<std::pair<std::string, double>> results;

    if (!db_ || !embeddings_cf_ || queryEmbedding.empty()) {
        return results;
    }

    try {
        // Simple linear scan approach - good enough for demo
        std::vector<std::pair<std::string, double>> all_similarities;

        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, embeddings_cf_));

        // Iterate through all embeddings and calculate similarity
        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string event_id = it->key().ToString();
            std::string value = it->value().ToString();

            // Deserialize embedding
            size_t num_floats = value.size() / sizeof(float);
            std::vector<float> embedding(num_floats);
            std::memcpy(embedding.data(), value.data(), value.size());

            // Calculate cosine similarity
            double similarity = AIAnalyzer::cosineSimilarity(queryEmbedding, embedding);
            all_similarities.push_back({event_id, similarity});
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error in findSimilar: " << it->status().ToString() << std::endl;
            return results;
        }

        // Sort by similarity (descending) and take top-K
        std::sort(all_similarities.begin(), all_similarities.end(),
                 [](const auto& a, const auto& b) { return a.second > b.second; });

        // Return top-K results
        size_t result_count = std::min(k, all_similarities.size());
        results.assign(all_similarities.begin(), all_similarities.begin() + result_count);

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in findSimilar: " << e.what() << std::endl;
    }

    return results;
}

uint64_t EventStore::getEmbeddingCount() {
    if (!db_ || !embeddings_cf_) {
        return 0;
    }

    uint64_t count = 0;
    rocksdb::ReadOptions read_options;
    std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, embeddings_cf_));

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        count++;
    }

    return count;
}

// Anomaly storage methods

bool EventStore::putAnomaly(const AnomalyResult& anomaly) {
    if (!db_ || !anomalies_cf_) {
        return false;
    }

    try {
        // Key format: timestamp:event_id (for time-ordered retrieval)
        std::ostringstream key_stream;
        key_stream << std::setfill('0') << std::setw(15) << anomaly.timestamp << ":" << anomaly.event_id;
        std::string key = key_stream.str();
        std::string value = anomaly.toJson();

        rocksdb::WriteOptions write_options;
        write_options.sync = false;  // Async writes for better performance

        rocksdb::Status status = db_->Put(write_options, anomalies_cf_, key, value);

        if (!status.ok()) {
            std::cerr << "[EventStore] Failed to put anomaly: " << status.ToString() << std::endl;
            return false;
        }

        return true;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in putAnomaly: " << e.what() << std::endl;
        return false;
    }
}

std::optional<AnomalyResult> EventStore::getAnomaly(const std::string& eventId) {
    if (!db_ || !anomalies_cf_) {
        return std::nullopt;
    }

    try {
        // Since we don't know the timestamp, we need to scan
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, anomalies_cf_));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string key = it->key().ToString();

            // Check if this key ends with our event ID
            if (key.find(":" + eventId) != std::string::npos) {
                std::string value = it->value().ToString();
                return AnomalyResult::fromJson(value);
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

        return std::nullopt;
    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnomaly: " << e.what() << std::endl;
        return std::nullopt;
    }
}

std::vector<AnomalyResult> EventStore::getAnomaliesByTimeRange(
    uint64_t startTime,
    uint64_t endTime,
    size_t limit
) {
    std::vector<AnomalyResult> anomalies;

    if (!db_ || !anomalies_cf_) {
        return anomalies;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, anomalies_cf_));

        // Create key range for time-based scan
        std::ostringstream start_key_stream, end_key_stream;
        start_key_stream << std::setfill('0') << std::setw(15) << startTime << ":";
        end_key_stream << std::setfill('0') << std::setw(15) << endTime << ":~";  // '~' is after ':'
        std::string start_key = start_key_stream.str();
        std::string end_key = end_key_stream.str();

        // Seek to start position
        it->Seek(start_key);

        while (it->Valid()) {
            std::string key = it->key().ToString();

            // Check if we've moved past our time range
            if (key > end_key) {
                break;
            }

            std::string value = it->value().ToString();
            auto anomaly = AnomalyResult::fromJson(value);
            anomalies.push_back(anomaly);

            if (limit > 0 && anomalies.size() >= limit) {
                break;
            }

            it->Next();
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnomaliesByTimeRange: " << e.what() << std::endl;
    }

    return anomalies;
}

std::vector<AnomalyResult> EventStore::getAnomaliesByUser(
    const std::string& user,
    size_t limit
) {
    std::vector<AnomalyResult> anomalies;

    if (!db_ || !anomalies_cf_) {
        return anomalies;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, anomalies_cf_));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string value = it->value().ToString();
            auto anomaly = AnomalyResult::fromJson(value);

            if (anomaly.user == user) {
                anomalies.push_back(anomaly);

                if (limit > 0 && anomalies.size() >= limit) {
                    break;
                }
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getAnomaliesByUser: " << e.what() << std::endl;
    }

    return anomalies;
}

std::vector<AnomalyResult> EventStore::getHighScoreAnomalies(
    double threshold,
    size_t limit
) {
    std::vector<AnomalyResult> anomalies;

    if (!db_ || !anomalies_cf_) {
        return anomalies;
    }

    try {
        rocksdb::ReadOptions read_options;
        std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, anomalies_cf_));

        for (it->SeekToFirst(); it->Valid(); it->Next()) {
            std::string value = it->value().ToString();
            auto anomaly = AnomalyResult::fromJson(value);

            if (anomaly.anomaly_score >= threshold) {
                anomalies.push_back(anomaly);

                if (limit > 0 && anomalies.size() >= limit) {
                    break;
                }
            }
        }

        if (!it->status().ok()) {
            std::cerr << "[EventStore] Iterator error: " << it->status().ToString() << std::endl;
        }

    } catch (const std::exception& e) {
        std::cerr << "[EventStore] Exception in getHighScoreAnomalies: " << e.what() << std::endl;
    }

    return anomalies;
}

uint64_t EventStore::getAnomalyCount() {
    if (!db_ || !anomalies_cf_) {
        return 0;
    }

    uint64_t count = 0;
    rocksdb::ReadOptions read_options;
    std::unique_ptr<rocksdb::Iterator> it(db_->NewIterator(read_options, anomalies_cf_));

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        count++;
    }

    return count;
}

} // namespace streamguard
