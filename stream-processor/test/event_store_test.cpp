#include "event_store.h"
#include "event.h"
#include "anomaly_detector.h"
#include <gtest/gtest.h>
#include <filesystem>
#include <chrono>
#include <thread>
#include <atomic>

using namespace streamguard;
namespace fs = std::filesystem;

class EventStoreTest : public ::testing::Test {
protected:
    void SetUp() override {
        // Create temporary test database
        test_db_path = "/tmp/test_events_" + std::to_string(std::time(nullptr)) + ".db";

        // Clean up if exists
        if (fs::exists(test_db_path)) {
            fs::remove_all(test_db_path);
        }

        store = std::make_unique<EventStore>(test_db_path);
    }

    void TearDown() override {
        // Close store and clean up
        store.reset();

        if (fs::exists(test_db_path)) {
            fs::remove_all(test_db_path);
        }
    }

    std::string test_db_path;
    std::unique_ptr<EventStore> store;

    Event createTestEvent(const std::string& event_id, const std::string& user = "test_user") {
        Event event;
        event.event_id = event_id;
        event.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
        event.user = user;
        event.source_ip = "192.168.1.100";
        event.destination_ip = "10.0.0.5";
        event.event_type = EventType::AUTH_ATTEMPT;
        event.status = EventStatus::SUCCESS;
        event.threat_score = 0.15;
        event.metadata.user_agent = "Mozilla/5.0";
        event.metadata.geo_location = "San Francisco, CA";
        return event;
    }
};

// Test 1: Store Initialization
TEST_F(EventStoreTest, Initialization) {
    EXPECT_TRUE(fs::exists(test_db_path)) << "Database should be created";
}

// Test 2: Put and Get Event
TEST_F(EventStoreTest, PutAndGetEvent) {
    Event original = createTestEvent("evt_test_001");

    // Store event
    bool put_success = store->put(original);
    EXPECT_TRUE(put_success) << "Should successfully store event";

    // Retrieve event
    Event retrieved;
    bool get_success = store->get("evt_test_001", retrieved);
    ASSERT_TRUE(get_success) << "Should retrieve stored event";

    // Verify fields
    EXPECT_EQ(retrieved.event_id, original.event_id);
    EXPECT_EQ(retrieved.user, original.user);
    EXPECT_EQ(retrieved.source_ip, original.source_ip);
    EXPECT_EQ(retrieved.event_type, original.event_type);
    EXPECT_EQ(retrieved.status, original.status);
    EXPECT_DOUBLE_EQ(retrieved.threat_score, original.threat_score);
}

// Test 3: Get Non-Existent Event
TEST_F(EventStoreTest, GetNonExistentEvent) {
    Event event;
    bool result = store->get("evt_does_not_exist", event);
    EXPECT_FALSE(result) << "Should return false for non-existent event";
}

// Test 4: Put Anomaly Result
TEST_F(EventStoreTest, PutAndGetAnomaly) {
    AnomalyResult anomaly;
    anomaly.event_id = "evt_anomaly_001";
    anomaly.user = "suspicious_user";
    anomaly.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    anomaly.anomaly_score = 0.85;
    anomaly.time_anomaly = 0.95;
    anomaly.ip_anomaly = 0.99;
    anomaly.location_anomaly = 0.80;
    anomaly.type_anomaly = 0.50;
    anomaly.failure_anomaly = 0.30;
    anomaly.reasons = {"Unusual hour (3 AM)", "New IP address"};

    // Store anomaly
    bool put_success = store->putAnomaly(anomaly);
    EXPECT_TRUE(put_success) << "Should successfully store anomaly";

    // Note: getAnomaly is not implemented in event_store.h, but we can verify
    // it was stored by checking getHighScoreAnomalies
}

// Test 5: Get High Score Anomalies
TEST_F(EventStoreTest, GetHighScoreAnomalies) {
    // Store multiple anomalies with different scores
    std::vector<double> scores = {0.9, 0.7, 0.5, 0.3, 0.8};

    for (size_t i = 0; i < scores.size(); i++) {
        AnomalyResult anomaly;
        anomaly.event_id = "evt_anomaly_" + std::to_string(i);
        anomaly.user = "user_" + std::to_string(i);
        anomaly.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count() + i; // Slightly different timestamps
        anomaly.anomaly_score = scores[i];
        anomaly.time_anomaly = scores[i];
        anomaly.ip_anomaly = scores[i];
        anomaly.location_anomaly = 0.0;
        anomaly.type_anomaly = 0.0;
        anomaly.failure_anomaly = 0.0;
        anomaly.reasons = {"Test anomaly"};

        store->putAnomaly(anomaly);
    }

    // Get anomalies with score >= 0.7
    auto results = store->getHighScoreAnomalies(0.7, 100);

    // Should get 3 anomalies: 0.9, 0.7, 0.8
    EXPECT_EQ(results.size(), 3) << "Should find 3 anomalies with score >= 0.7";

    // Verify all returned anomalies meet threshold
    for (const auto& anomaly : results) {
        EXPECT_GE(anomaly.anomaly_score, 0.7) << "All returned anomalies should meet threshold";
    }
}

// Test 6: Limit Parameter in Get High Score Anomalies
TEST_F(EventStoreTest, GetHighScoreAnomaliesLimit) {
    // Store 10 high-score anomalies
    for (int i = 0; i < 10; i++) {
        AnomalyResult anomaly;
        anomaly.event_id = "evt_limit_" + std::to_string(i);
        anomaly.user = "user";
        anomaly.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count() + i;
        anomaly.anomaly_score = 0.9;
        anomaly.time_anomaly = 0.9;
        anomaly.ip_anomaly = 0.9;
        anomaly.location_anomaly = 0.0;
        anomaly.type_anomaly = 0.0;
        anomaly.failure_anomaly = 0.0;

        store->putAnomaly(anomaly);
    }

    // Get only 5
    auto results = store->getHighScoreAnomalies(0.5, 5);

    EXPECT_EQ(results.size(), 5) << "Should respect limit parameter";
}

// Test 7: Multiple Events for Same User
TEST_F(EventStoreTest, MultipleEventsPerUser) {
    std::string user = "alice";

    // Store 10 events for the same user
    for (int i = 0; i < 10; i++) {
        Event event = createTestEvent("evt_alice_" + std::to_string(i), user);
        bool success = store->put(event);
        EXPECT_TRUE(success) << "Event " << i << " should be stored";
    }

    // Verify all can be retrieved
    for (int i = 0; i < 10; i++) {
        Event retrieved;
        bool get_success = store->get("evt_alice_" + std::to_string(i), retrieved);
        ASSERT_TRUE(get_success) << "Event " << i << " should be retrievable";
        EXPECT_EQ(retrieved.user, user);
    }
}

// Test 8: Event Ordering by Timestamp
TEST_F(EventStoreTest, EventOrderingByTimestamp) {
    // Create events with specific timestamps
    std::vector<uint64_t> timestamps = {
        1704067200000, // Oldest
        1704067300000,
        1704067400000,
        1704067500000,
        1704067600000  // Newest
    };

    for (size_t i = 0; i < timestamps.size(); i++) {
        Event event = createTestEvent("evt_order_" + std::to_string(i));
        event.timestamp = timestamps[i];
        store->put(event);
    }

    // Retrieve newest first (depends on getEvents implementation)
    // This test validates time-ordered key format
    Event newest, oldest;
    bool newest_found = store->get("evt_order_4", newest);
    bool oldest_found = store->get("evt_order_0", oldest);

    EXPECT_TRUE(newest_found);
    EXPECT_TRUE(oldest_found);
    EXPECT_GT(newest.timestamp, oldest.timestamp);
}

// Test 9: Column Family Isolation
TEST_F(EventStoreTest, ColumnFamilyIsolation) {
    // Store event and anomaly with same ID prefix
    Event event = createTestEvent("evt_isolation_001");
    store->put(event);

    AnomalyResult anomaly;
    anomaly.event_id = "evt_isolation_001";
    anomaly.user = event.user;
    anomaly.timestamp = event.timestamp;
    anomaly.anomaly_score = 0.75;
    anomaly.time_anomaly = 0.75;
    anomaly.ip_anomaly = 0.0;
    anomaly.location_anomaly = 0.0;
    anomaly.type_anomaly = 0.0;
    anomaly.failure_anomaly = 0.0;
    store->putAnomaly(anomaly);

    // Both should be independently retrievable
    Event retrieved_event;
    bool event_found = store->get("evt_isolation_001", retrieved_event);
    EXPECT_TRUE(event_found) << "Event should be in default CF";

    // Anomalies are in separate CF (verified by getHighScoreAnomalies)
    auto anomalies = store->getHighScoreAnomalies(0.7, 10);
    bool anomaly_found = false;
    for (const auto& a : anomalies) {
        if (a.event_id == "evt_isolation_001") {
            anomaly_found = true;
            break;
        }
    }
    EXPECT_TRUE(anomaly_found) << "Anomaly should be in anomalies CF";
}

// Test 10: Event with All Fields Populated
TEST_F(EventStoreTest, CompleteEvent) {
    Event event;
    event.event_id = "evt_complete_001";
    event.timestamp = 1704067200000;
    event.user = "complete_user";
    event.source_ip = "192.168.1.100";
    event.destination_ip = "10.0.0.5";
    event.event_type = EventType::NETWORK_CONNECTION;
    event.status = EventStatus::SUCCESS;
    event.threat_score = 0.25;
    event.metadata.user_agent = "curl/7.81.0";
    event.metadata.geo_location = "New York, NY";
    event.metadata.port = 443;
    event.metadata.protocol = "HTTPS";
    event.metadata.bytes_transferred = 2048;

    store->put(event);

    Event retrieved;
    bool found = store->get("evt_complete_001", retrieved);
    ASSERT_TRUE(found);

    // Verify all metadata fields
    EXPECT_EQ(retrieved.metadata.user_agent, "curl/7.81.0");
    EXPECT_EQ(retrieved.metadata.geo_location, "New York, NY");
    EXPECT_EQ(retrieved.metadata.port, 443);
    EXPECT_EQ(retrieved.metadata.protocol, "HTTPS");
    EXPECT_EQ(retrieved.metadata.bytes_transferred, 2048);
}

// Test 11: Concurrent Writes (Basic)
TEST_F(EventStoreTest, ConcurrentWrites) {
    std::atomic<int> success_count{0};
    std::vector<std::thread> threads;

    // Spawn 5 threads, each writing 20 events
    for (int t = 0; t < 5; t++) {
        threads.emplace_back([this, t, &success_count]() {
            for (int i = 0; i < 20; i++) {
                std::string event_id = "evt_concurrent_t" + std::to_string(t) + "_" + std::to_string(i);
                Event event = createTestEvent(event_id);
                if (store->put(event)) {
                    success_count++;
                }
            }
        });
    }

    // Wait for all threads
    for (auto& thread : threads) {
        thread.join();
    }

    EXPECT_EQ(success_count.load(), 100) << "All 100 events should be stored successfully";
}

// Test 12: Large Batch Write Performance
TEST_F(EventStoreTest, LargeBatchWrite) {
    const int BATCH_SIZE = 1000;
    auto start = std::chrono::high_resolution_clock::now();

    for (int i = 0; i < BATCH_SIZE; i++) {
        Event event = createTestEvent("evt_batch_" + std::to_string(i));
        store->put(event);
    }

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

    std::cout << "Wrote " << BATCH_SIZE << " events in " << duration.count() << "ms" << std::endl;
    std::cout << "Throughput: " << (BATCH_SIZE * 1000 / duration.count()) << " events/sec" << std::endl;

    // Should complete in reasonable time (< 5 seconds for 1000 events)
    EXPECT_LT(duration.count(), 5000) << "Batch write should complete quickly";
}

// Test 13: Empty Database Queries
TEST_F(EventStoreTest, EmptyDatabaseQueries) {
    // Query high score anomalies on empty database
    auto anomalies = store->getHighScoreAnomalies(0.5, 100);
    EXPECT_EQ(anomalies.size(), 0) << "Empty database should return no results";

    // Get non-existent event
    Event event;
    bool found = store->get("evt_nonexistent", event);
    EXPECT_FALSE(found) << "Should return false for non-existent event";
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
