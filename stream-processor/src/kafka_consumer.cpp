#include "kafka_consumer.h"
#include <iostream>
#include <sstream>
#include <nlohmann/json.hpp>

namespace streamguard {

KafkaConsumer::KafkaConsumer(const std::string& brokers,
                             const std::string& groupId,
                             const std::string& topic)
    : brokers_(brokers),
      groupId_(groupId),
      topic_(topic),
      pollTimeoutMs_(1000),
      running_(false),
      messagesConsumed_(0),
      errors_(0) {

    initializeConsumer();
}

KafkaConsumer::~KafkaConsumer() {
    shutdown();
}

void KafkaConsumer::setEventCallback(EventCallback callback) {
    eventCallback_ = callback;
}

void KafkaConsumer::setPollTimeout(int timeoutMs) {
    pollTimeoutMs_ = timeoutMs;
}

void KafkaConsumer::initializeConsumer() {
    std::string errstr;

    // Create configuration
    conf_.reset(RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL));

    if (!conf_) {
        throw std::runtime_error("Failed to create Kafka configuration");
    }

    // Set configuration properties
    if (conf_->set("bootstrap.servers", brokers_, errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set bootstrap.servers: " + errstr);
    }

    if (conf_->set("group.id", groupId_, errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set group.id: " + errstr);
    }

    // Enable auto offset commits
    if (conf_->set("enable.auto.commit", "true", errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set enable.auto.commit: " + errstr);
    }

    // Auto commit interval
    if (conf_->set("auto.commit.interval.ms", "5000", errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set auto.commit.interval.ms: " + errstr);
    }

    // Start reading from beginning if no offset
    if (conf_->set("auto.offset.reset", "earliest", errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set auto.offset.reset: " + errstr);
    }

    // Session timeout
    if (conf_->set("session.timeout.ms", "30000", errstr) != RdKafka::Conf::CONF_OK) {
        throw std::runtime_error("Failed to set session.timeout.ms: " + errstr);
    }

    // Create consumer
    consumer_.reset(RdKafka::KafkaConsumer::create(conf_.get(), errstr));

    if (!consumer_) {
        throw std::runtime_error("Failed to create Kafka consumer: " + errstr);
    }

    std::cout << "[KafkaConsumer] Consumer created successfully" << std::endl;
    std::cout << "[KafkaConsumer] Brokers: " << brokers_ << std::endl;
    std::cout << "[KafkaConsumer] Group ID: " << groupId_ << std::endl;
    std::cout << "[KafkaConsumer] Topic: " << topic_ << std::endl;
}

void KafkaConsumer::start() {
    // Subscribe to topic
    std::vector<std::string> topics = {topic_};
    RdKafka::ErrorCode err = consumer_->subscribe(topics);

    if (err) {
        throw std::runtime_error("Failed to subscribe to topic: " +
                                 RdKafka::err2str(err));
    }

    std::cout << "[KafkaConsumer] Subscribed to topic: " << topic_ << std::endl;
    std::cout << "[KafkaConsumer] Starting consumer loop..." << std::endl;

    running_ = true;
    uint64_t lastLogCount = 0;
    auto lastLogTime = std::chrono::steady_clock::now();

    while (running_) {
        // Poll for messages
        std::unique_ptr<RdKafka::Message> msg(consumer_->consume(pollTimeoutMs_));

        if (!msg) {
            continue;
        }

        processMessage(msg.get());

        // Log metrics every 10 seconds
        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - lastLogTime);

        if (elapsed.count() >= 10) {
            uint64_t currentCount = messagesConsumed_;
            uint64_t messagesInPeriod = currentCount - lastLogCount;
            double rate = messagesInPeriod / 10.0;

            std::cout << "[KafkaConsumer] Messages consumed: " << currentCount
                      << " | Rate: " << rate << " msg/sec | Errors: " << errors_
                      << std::endl;

            lastLogCount = currentCount;
            lastLogTime = now;
        }
    }

    std::cout << "[KafkaConsumer] Consumer loop ended" << std::endl;
}

void KafkaConsumer::processMessage(RdKafka::Message* message) {
    switch (message->err()) {
        case RdKafka::ERR_NO_ERROR: {
            // Valid message
            std::string payload(
                static_cast<const char*>(message->payload()),
                message->len()
            );

            try {
                Event event = deserializeEvent(payload);
                messagesConsumed_++;

                // Call callback if set
                if (eventCallback_) {
                    eventCallback_(event);
                }
            } catch (const std::exception& e) {
                std::cerr << "[KafkaConsumer] Error deserializing event: " << e.what()
                          << std::endl;
                errors_++;
            }
            break;
        }

        case RdKafka::ERR__PARTITION_EOF:
            // Reached end of partition (not an error)
            break;

        case RdKafka::ERR__TIMED_OUT:
            // Poll timeout (not an error)
            break;

        default:
            // Error
            std::cerr << "[KafkaConsumer] Kafka error: " << message->errstr()
                      << std::endl;
            errors_++;
            break;
    }
}

Event KafkaConsumer::deserializeEvent(const std::string& jsonStr) {
    using nlohmann::json;

    try {
        auto j = json::parse(jsonStr);
        return Event::fromJson(j.dump());
    } catch (const json::exception& e) {
        throw std::runtime_error(std::string("JSON parse error: ") + e.what());
    }
}

void KafkaConsumer::shutdown() {
    if (!running_) {
        return;
    }

    std::cout << "[KafkaConsumer] Shutting down..." << std::endl;

    running_ = false;

    if (consumer_) {
        // Close consumer (commits offsets)
        std::cout << "[KafkaConsumer] Closing consumer and committing offsets..." << std::endl;
        consumer_->close();
    }

    std::cout << "[KafkaConsumer] === Consumer Statistics ===" << std::endl;
    std::cout << "[KafkaConsumer] Total messages consumed: " << messagesConsumed_ << std::endl;
    std::cout << "[KafkaConsumer] Total errors: " << errors_ << std::endl;
    std::cout << "[KafkaConsumer] Success rate: "
              << (messagesConsumed_ > 0 ?
                  (100.0 * messagesConsumed_ / (messagesConsumed_ + errors_)) : 0.0)
              << "%" << std::endl;
    std::cout << "[KafkaConsumer] ============================" << std::endl;
}

uint64_t KafkaConsumer::getMessagesConsumed() const {
    return messagesConsumed_;
}

uint64_t KafkaConsumer::getErrors() const {
    return errors_;
}

} // namespace streamguard
