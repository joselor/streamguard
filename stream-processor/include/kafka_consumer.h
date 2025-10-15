#ifndef STREAMGUARD_KAFKA_CONSUMER_H
#define STREAMGUARD_KAFKA_CONSUMER_H

#include <string>
#include <vector>
#include <memory>
#include <atomic>
#include <functional>
#include <librdkafka/rdkafkacpp.h>
#include "event.h"

namespace streamguard {

/**
 * Kafka consumer for StreamGuard security events.
 *
 * This class provides a high-level interface for consuming events from Kafka,
 * with automatic JSON deserialization, offset management, and graceful shutdown.
 *
 * Features:
 * - Consumer group management with automatic rebalancing
 * - JSON deserialization to Event objects
 * - Automatic offset commits
 * - Graceful shutdown with pending offset commits
 * - Error handling and retry logic
 * - Configurable poll timeout and batch size
 *
 * Example usage:
 * @code
 * KafkaConsumer consumer("localhost:9092", "streamguard-processor", "security-events");
 *
 * consumer.setEventCallback([](const Event& event) {
 *     std::cout << "Received event: " << event.event_id << std::endl;
 *     // Process event...
 * });
 *
 * consumer.start();  // Blocks until shutdown
 * @endcode
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class KafkaConsumer {
public:
    /**
     * Callback function type for processing events.
     *
     * @param event The deserialized Event object
     */
    using EventCallback = std::function<void(const Event&)>;

    /**
     * Creates a new Kafka consumer.
     *
     * @param brokers Comma-separated list of Kafka brokers (e.g., "localhost:9092")
     * @param groupId Consumer group ID for offset management
     * @param topic Kafka topic to consume from
     */
    KafkaConsumer(const std::string& brokers,
                  const std::string& groupId,
                  const std::string& topic);

    /**
     * Destructor - ensures graceful shutdown.
     */
    ~KafkaConsumer();

    // Disable copy constructor and assignment
    KafkaConsumer(const KafkaConsumer&) = delete;
    KafkaConsumer& operator=(const KafkaConsumer&) = delete;

    /**
     * Sets the callback function for processing events.
     *
     * @param callback Function to call for each received event
     */
    void setEventCallback(EventCallback callback);

    /**
     * Sets the poll timeout in milliseconds.
     * Default: 1000ms
     *
     * @param timeoutMs Poll timeout in milliseconds
     */
    void setPollTimeout(int timeoutMs);

    /**
     * Starts consuming events from Kafka.
     * This method blocks until shutdown() is called or the external running flag becomes false.
     *
     * @param externalRunning Optional external atomic flag to control the consumer loop
     */
    void start(std::atomic<bool>* externalRunning = nullptr);

    /**
     * Initiates graceful shutdown of the consumer.
     * Commits pending offsets and closes the consumer.
     */
    void shutdown();

    /**
     * Returns consumer statistics.
     *
     * @return Total number of messages consumed
     */
    uint64_t getMessagesConsumed() const;

    /**
     * Returns number of deserialization errors.
     *
     * @return Total number of JSON parse errors
     */
    uint64_t getErrors() const;

private:
    /**
     * Initializes the Kafka consumer with configuration.
     */
    void initializeConsumer();

    /**
     * Processes a single Kafka message.
     *
     * @param message The Kafka message to process
     */
    void processMessage(RdKafka::Message* message);

    /**
     * Deserializes JSON payload to Event object.
     *
     * @param jsonStr JSON string
     * @return Deserialized Event object
     * @throws std::runtime_error if JSON is invalid
     */
    Event deserializeEvent(const std::string& jsonStr);

    // Configuration
    std::string brokers_;
    std::string groupId_;
    std::string topic_;
    int pollTimeoutMs_;

    // Kafka consumer
    std::unique_ptr<RdKafka::KafkaConsumer> consumer_;
    std::unique_ptr<RdKafka::Conf> conf_;

    // Callback
    EventCallback eventCallback_;

    // State
    std::atomic<bool> running_;
    std::atomic<uint64_t> messagesConsumed_;
    std::atomic<uint64_t> errors_;
};

} // namespace streamguard

#endif // STREAMGUARD_KAFKA_CONSUMER_H
