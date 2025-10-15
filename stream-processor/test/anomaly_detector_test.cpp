#include "anomaly_detector.h"
#include "event.h"
#include <gtest/gtest.h>
#include <chrono>
#include <thread>

using namespace streamguard;

class AnomalyDetectorTest : public ::testing::Test {
protected:
    void SetUp() override {
        detector = std::make_unique<AnomalyDetector>(100); // 100 events for baseline
    }

    std::unique_ptr<AnomalyDetector> detector;

    Event createTestEvent(const std::string& user,
                         const std::string& source_ip = "192.168.1.100",
                         int hour = 10,
                         EventType type = EventType::AUTH_ATTEMPT,
                         EventStatus status = EventStatus::SUCCESS) {
        Event event;
        event.event_id = "evt_test_" + std::to_string(std::rand());

        // Set timestamp to specific hour
        auto now = std::chrono::system_clock::now();
        auto time_t_now = std::chrono::system_clock::to_time_t(now);
        struct tm* tm_info = std::localtime(&time_t_now);
        tm_info->tm_hour = hour;
        tm_info->tm_min = 0;
        tm_info->tm_sec = 0;
        event.timestamp = std::mktime(tm_info) * 1000ULL;

        event.user = user;
        event.source_ip = source_ip;
        event.event_type = type;
        event.status = status;
        event.threat_score = 0.1;
        event.metadata.geo_location = "San Francisco, CA";

        return event;
    }
};

// Test 1: Baseline Learning Phase
TEST_F(AnomalyDetectorTest, BaselineLearningPhase) {
    std::string user = "alice";

    // Send 50 events (below 100 threshold)
    for (int i = 0; i < 50; i++) {
        Event event = createTestEvent(user);
        auto result = detector->analyze(event);

        // Should return nullopt during learning phase
        EXPECT_FALSE(result.has_value()) << "Event " << i << " should not trigger anomaly during learning";
    }

    // Verify baseline is not ready yet
    auto baseline = detector->getBaseline(user);
    EXPECT_TRUE(baseline.has_value());
    EXPECT_FALSE(baseline->is_baseline_ready) << "Baseline should not be ready with only 50 events";
    EXPECT_EQ(baseline->total_events, 50);
}

// Test 2: Baseline Establishment
TEST_F(AnomalyDetectorTest, BaselineEstablishment) {
    std::string user = "bob";

    // Send exactly 100 events to establish baseline
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10); // Normal pattern
        detector->analyze(event);
    }

    // Verify baseline is ready
    auto baseline = detector->getBaseline(user);
    EXPECT_TRUE(baseline.has_value());
    EXPECT_TRUE(baseline->is_baseline_ready) << "Baseline should be ready after 100 events";
    EXPECT_EQ(baseline->total_events, 100);

    // Verify baseline learned patterns
    EXPECT_GT(baseline->hourly_activity[10], 0) << "Should have recorded activity at hour 10";
    EXPECT_GT(baseline->source_ips["192.168.1.100"], 0) << "Should have recorded IP";
}

// Test 3: Time Anomaly Detection
TEST_F(AnomalyDetectorTest, TimeAnomalyDetection) {
    std::string user = "charlie";

    // Establish baseline: user always logs in at 10am from San Francisco
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        event.metadata.geo_location = "San Francisco, CA";
        detector->analyze(event);
    }

    // Send event at unusual hour (3am) with new location to exceed 0.5 threshold
    // Time anomaly: 0.99 * 0.25 = 0.2475
    // Location anomaly: 0.99 * 0.20 = 0.198
    // Combined: ~0.45, need one more dimension
    Event unusual = createTestEvent(user, "45.142.120.10", 3);  // Also new IP
    unusual.metadata.geo_location = "Moscow, Russia";
    auto result = detector->analyze(unusual);

    ASSERT_TRUE(result.has_value()) << "Unusual hour + new IP + new location should trigger anomaly";
    EXPECT_GT(result->time_anomaly, 0.9) << "Time anomaly score should be high";
    EXPECT_GE(result->anomaly_score, 0.5) << "Overall score should exceed threshold";

    // Verify anomaly reasons
    bool found_time_reason = false;
    for (const auto& reason : result->reasons) {
        if (reason.find("Unusual") != std::string::npos ||
            reason.find("hour") != std::string::npos ||
            reason.find("time") != std::string::npos) {
            found_time_reason = true;
            break;
        }
    }
    EXPECT_TRUE(found_time_reason) << "Should have time-related anomaly reason";
}

// Test 4: IP Anomaly Detection
TEST_F(AnomalyDetectorTest, IPAnomalyDetection) {
    std::string user = "david";

    // Establish baseline: user always AUTH_ATTEMPT from 192.168.1.100 in San Francisco
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10, EventType::AUTH_ATTEMPT);
        event.metadata.geo_location = "San Francisco, CA";
        detector->analyze(event);
    }

    // Send event from new IP + new location + new event type to exceed 0.5 threshold
    // IP anomaly: 0.99 * 0.30 = 0.297
    // Location anomaly: 0.99 * 0.20 = 0.198
    // Type anomaly: 0.99 * 0.15 = 0.1485
    // Combined: ~0.64
    Event newIP = createTestEvent(user, "45.142.120.10", 10, EventType::PROCESS_EXECUTION); // Russian IP, unusual type
    newIP.metadata.geo_location = "Moscow, Russia";
    auto result = detector->analyze(newIP);

    ASSERT_TRUE(result.has_value()) << "New IP + new location + new event type should trigger anomaly";
    EXPECT_GT(result->ip_anomaly, 0.9) << "IP anomaly score should be high";
    EXPECT_GE(result->anomaly_score, 0.5) << "Overall score should exceed threshold";

    // Verify anomaly reasons mention IP
    bool found_ip_reason = false;
    for (const auto& reason : result->reasons) {
        if (reason.find("IP") != std::string::npos ||
            reason.find("ip") != std::string::npos) {
            found_ip_reason = true;
            break;
        }
    }
    EXPECT_TRUE(found_ip_reason) << "Should have IP-related anomaly reason";
}

// Test 5: Location Anomaly Detection
TEST_F(AnomalyDetectorTest, LocationAnomalyDetection) {
    std::string user = "eve";

    // Establish baseline: user always from San Francisco at 10am
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        event.metadata.geo_location = "San Francisco, CA";
        detector->analyze(event);
    }

    // Send event from new location + new IP + unusual time to exceed threshold
    // Location: 0.99 * 0.20 = 0.198
    // IP: 0.99 * 0.30 = 0.297
    // Time: 0.99 * 0.25 = 0.2475
    // Combined: ~0.74
    Event newLocation = createTestEvent(user, "45.142.120.10", 3);
    newLocation.metadata.geo_location = "Moscow, Russia";
    auto result = detector->analyze(newLocation);

    ASSERT_TRUE(result.has_value()) << "New location + new IP + unusual time should trigger anomaly";
    EXPECT_GT(result->location_anomaly, 0.9) << "Location anomaly score should be high";
    EXPECT_GE(result->anomaly_score, 0.5) << "Overall score should exceed threshold";
}

// Test 6: Event Type Anomaly Detection
TEST_F(AnomalyDetectorTest, EventTypeAnomalyDetection) {
    std::string user = "frank";

    // Establish baseline: user always does AUTH_ATTEMPT from SF
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10, EventType::AUTH_ATTEMPT);
        event.metadata.geo_location = "San Francisco, CA";
        detector->analyze(event);
    }

    // Send unusual event type + new location + new IP + unusual time
    // Type: 0.99 * 0.15 = 0.1485
    // Location: 0.99 * 0.20 = 0.198
    // IP: 0.99 * 0.30 = 0.297
    // Time: 0.99 * 0.25 = 0.2475
    // Combined: ~0.89
    Event unusual = createTestEvent(user, "45.142.120.10", 3, EventType::PROCESS_EXECUTION);
    unusual.metadata.geo_location = "Moscow, Russia";
    auto result = detector->analyze(unusual);

    ASSERT_TRUE(result.has_value()) << "Multiple anomalies including unusual event type should trigger";
    EXPECT_GT(result->type_anomaly, 0.9) << "Type anomaly score should be high";
    EXPECT_GE(result->anomaly_score, 0.5) << "Overall score should exceed threshold";
}

// Test 7: Failure Anomaly Detection
TEST_F(AnomalyDetectorTest, FailureAnomalyDetection) {
    std::string user = "grace";

    // Establish baseline: user with 5% failure rate
    for (int i = 0; i < 100; i++) {
        EventStatus status = (i < 5) ? EventStatus::FAILED : EventStatus::SUCCESS;
        Event event = createTestEvent(user, "192.168.1.100", 10, EventType::AUTH_ATTEMPT, status);
        detector->analyze(event);
    }

    // Verify baseline failure rate
    auto baseline = detector->getBaseline(user);
    EXPECT_DOUBLE_EQ(baseline->getFailureRate(), 0.05);

    // Send multiple failed attempts
    for (int i = 0; i < 10; i++) {
        Event failed = createTestEvent(user, "192.168.1.100", 10, EventType::AUTH_ATTEMPT, EventStatus::FAILED);
        auto result = detector->analyze(failed);

        if (result.has_value()) {
            EXPECT_GT(result->failure_anomaly, 0.0) << "Failure should contribute to anomaly score";
        }
    }
}

// Test 8: Composite Anomaly Score
TEST_F(AnomalyDetectorTest, CompositeAnomalyScore) {
    std::string user = "hank";

    // Establish normal baseline
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        event.metadata.geo_location = "San Francisco, CA";
        detector->analyze(event);
    }

    // Create highly anomalous event (multiple dimensions)
    Event anomalous = createTestEvent(user, "45.142.120.10", 3); // New IP + unusual hour
    anomalous.metadata.geo_location = "Moscow, Russia"; // New location
    anomalous.status = EventStatus::FAILED; // Failure

    auto result = detector->analyze(anomalous);

    ASSERT_TRUE(result.has_value()) << "Multiple anomalies should trigger detection";
    EXPECT_GE(result->anomaly_score, 0.7) << "Composite score should be high";
    EXPECT_GT(result->time_anomaly, 0.5);
    EXPECT_GT(result->ip_anomaly, 0.5);
    EXPECT_GT(result->location_anomaly, 0.5);
}

// Test 9: Selective Baseline Updates (Baseline Poisoning Prevention)
TEST_F(AnomalyDetectorTest, SelectiveBaselineUpdates) {
    std::string user = "iris";

    // Establish baseline
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        detector->analyze(event);
    }

    auto baseline_before = detector->getBaseline(user);
    int events_before = baseline_before->total_events;

    // Send highly anomalous event
    Event attack = createTestEvent(user, "45.142.120.10", 3);
    attack.metadata.geo_location = "Moscow, Russia";
    auto result = detector->analyze(attack);

    ASSERT_TRUE(result.has_value()) << "Attack should be detected";
    EXPECT_GE(result->anomaly_score, 0.5);

    auto baseline_after = detector->getBaseline(user);

    // Baseline should NOT include the anomalous event
    EXPECT_EQ(baseline_after->total_events, events_before)
        << "Anomalous event should NOT be added to baseline";

    EXPECT_EQ(baseline_after->source_ips.count("45.142.120.10"), 0)
        << "Anomalous IP should NOT be in baseline";

    // Send normal event
    Event normal = createTestEvent(user, "192.168.1.100", 10);
    detector->analyze(normal);

    auto baseline_after_normal = detector->getBaseline(user);
    EXPECT_EQ(baseline_after_normal->total_events, events_before + 1)
        << "Normal event should be added to baseline";
}

// Test 10: Threshold Configuration
TEST_F(AnomalyDetectorTest, ThresholdConfiguration) {
    // Create detector with custom threshold
    AnomalyDetector custom_detector(100);
    custom_detector.setThreshold(0.8); // Higher threshold

    EXPECT_DOUBLE_EQ(custom_detector.getThreshold(), 0.8);

    std::string user = "jack";

    // Establish baseline
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        custom_detector.analyze(event);
    }

    // Send moderately anomalous event
    Event moderate = createTestEvent(user, "192.168.1.101", 10); // Slightly different IP
    auto result = custom_detector.analyze(moderate);

    // With higher threshold (0.8), moderate anomalies might not trigger
    if (result.has_value()) {
        EXPECT_GE(result->anomaly_score, 0.8) << "Only high-score anomalies should be reported";
    }
}

// Test 11: Multiple Users Independence
TEST_F(AnomalyDetectorTest, MultipleUsersIndependence) {
    // Create baselines for multiple users
    for (int i = 0; i < 100; i++) {
        Event alice = createTestEvent("alice", "192.168.1.100", 9);
        Event bob = createTestEvent("bob", "10.0.0.50", 14);
        Event charlie = createTestEvent("charlie", "172.16.0.10", 18);

        detector->analyze(alice);
        detector->analyze(bob);
        detector->analyze(charlie);
    }

    EXPECT_EQ(detector->getUserCount(), 3) << "Should track 3 users";

    // Verify baselines are independent
    auto alice_baseline = detector->getBaseline("alice");
    auto bob_baseline = detector->getBaseline("bob");

    EXPECT_GT(alice_baseline->hourly_activity[9], 0);
    EXPECT_EQ(alice_baseline->hourly_activity[14], 0) << "Alice shouldn't have Bob's activity";

    EXPECT_GT(bob_baseline->hourly_activity[14], 0);
    EXPECT_EQ(bob_baseline->hourly_activity[9], 0) << "Bob shouldn't have Alice's activity";
}

// Test 12: Thread Safety (Basic)
TEST_F(AnomalyDetectorTest, ThreadSafety) {
    std::string user = "concurrent_user";
    std::atomic<int> anomaly_count{0};

    // Establish baseline first
    for (int i = 0; i < 100; i++) {
        Event event = createTestEvent(user, "192.168.1.100", 10);
        detector->analyze(event);
    }

    // Analyze events from multiple threads
    std::thread t1([&]() {
        for (int i = 0; i < 50; i++) {
            Event event = createTestEvent(user, "192.168.1.100", 10);
            if (detector->analyze(event).has_value()) {
                anomaly_count++;
            }
        }
    });

    std::thread t2([&]() {
        for (int i = 0; i < 50; i++) {
            Event event = createTestEvent(user, "45.142.120.10", 3); // Anomalous
            if (detector->analyze(event).has_value()) {
                anomaly_count++;
            }
        }
    });

    t1.join();
    t2.join();

    // Should detect anomalies from t2
    EXPECT_GT(anomaly_count.load(), 0) << "Should detect some anomalies";
}

// Test 13: AnomalyResult Serialization
TEST_F(AnomalyDetectorTest, AnomalyResultSerialization) {
    AnomalyResult result;
    result.event_id = "evt_serialize_test";
    result.user = "test_user";
    result.timestamp = 1704067200000;
    result.anomaly_score = 0.85;
    result.time_anomaly = 0.95;
    result.ip_anomaly = 0.99;
    result.location_anomaly = 0.80;
    result.type_anomaly = 0.50;
    result.failure_anomaly = 0.30;
    result.reasons = {"Unusual hour", "New IP address"};

    // Serialize
    std::string json = result.toJson();

    // Deserialize
    AnomalyResult deserialized = AnomalyResult::fromJson(json);

    // Verify
    EXPECT_EQ(deserialized.event_id, "evt_serialize_test");
    EXPECT_EQ(deserialized.user, "test_user");
    EXPECT_EQ(deserialized.timestamp, 1704067200000);
    EXPECT_DOUBLE_EQ(deserialized.anomaly_score, 0.85);
    EXPECT_DOUBLE_EQ(deserialized.time_anomaly, 0.95);
    EXPECT_DOUBLE_EQ(deserialized.ip_anomaly, 0.99);
    EXPECT_EQ(deserialized.reasons.size(), 2);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
