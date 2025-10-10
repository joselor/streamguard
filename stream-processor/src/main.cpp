#include <iostream>
#include <csignal>
#include <atomic>
#include <thread>
#include <chrono>
#include "kafka_consumer.h"
#include "event_store.h"
#include "event.h"
#include "metrics.h"
#include "ai_analyzer.h"
#include "anomaly_detector.h"

/**
 * StreamGuard Stream Processor - Main Entry Point
 *
 * This application consumes security events from Kafka, processes them in real-time,
 * and stores them in RocksDB for time-series analysis.
 *
 * Command-line arguments:
 *   --broker <address>  Kafka bootstrap servers (default: localhost:9092)
 *   --topic <name>      Kafka topic to consume (default: security-events)
 *   --group <id>        Consumer group ID (default: streamguard-processor)
 *   --db <path>         RocksDB database path (default: ./data/events.db)
 *
 * @author Jose Ortuno
 * @version 1.0
 */

namespace {
    // Shutdown flag for signal handler - safer than storing raw pointer
    std::atomic<bool> shutdownRequested(false);
}

/**
 * Signal handler for graceful shutdown (SIGINT, SIGTERM)
 *
 * Sets a flag that is checked by the main loop. This avoids directly
 * accessing the consumer object from the signal handler context.
 */
void signalHandler(int signal) {
    std::cout << "\n[Main] Received signal " << signal << ", shutting down..." << std::endl;
    shutdownRequested.store(true);
}

/**
 * Prints usage information
 */
void printUsage(const char* progName) {
    std::cout << "StreamGuard Stream Processor" << std::endl;
    std::cout << std::endl;
    std::cout << "Usage: " << progName << " [OPTIONS]" << std::endl;
    std::cout << std::endl;
    std::cout << "Options:" << std::endl;
    std::cout << "  --broker <address>      Kafka bootstrap servers (default: localhost:9092)" << std::endl;
    std::cout << "  --topic <name>          Kafka topic to consume (default: security-events)" << std::endl;
    std::cout << "  --group <id>            Consumer group ID (default: streamguard-processor)" << std::endl;
    std::cout << "  --db <path>             RocksDB database path (default: ./data/events.db)" << std::endl;
    std::cout << "  --metrics-port <port>   Prometheus metrics port (default: 8080)" << std::endl;
    std::cout << "  --openai-key <key>      OpenAI API key for AI threat analysis (optional)" << std::endl;
    std::cout << "  --help, -h              Show this help message" << std::endl;
    std::cout << std::endl;
    std::cout << "Examples:" << std::endl;
    std::cout << "  # Use default settings" << std::endl;
    std::cout << "  " << progName << std::endl;
    std::cout << std::endl;
    std::cout << "  # Connect to remote Kafka cluster" << std::endl;
    std::cout << "  " << progName << " --broker kafka1:9092,kafka2:9092,kafka3:9092" << std::endl;
    std::cout << std::endl;
}

/**
 * Parses command-line arguments
 */
struct Config {
    std::string broker = "localhost:9092";
    std::string topic = "security-events";
    std::string groupId = "streamguard-processor";
    std::string dbPath = "./data/events.db";
    int metricsPort = 8080;
    std::string openaiKey = "";  // Optional: empty means AI disabled
};

Config parseArgs(int argc, char* argv[]) {
    Config config;

    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];

        if (arg == "--broker" && i + 1 < argc) {
            config.broker = argv[++i];
        } else if (arg == "--topic" && i + 1 < argc) {
            config.topic = argv[++i];
        } else if (arg == "--group" && i + 1 < argc) {
            config.groupId = argv[++i];
        } else if (arg == "--db" && i + 1 < argc) {
            config.dbPath = argv[++i];
        } else if (arg == "--metrics-port" && i + 1 < argc) {
            config.metricsPort = std::stoi(argv[++i]);
        } else if (arg == "--openai-key" && i + 1 < argc) {
            config.openaiKey = argv[++i];
        } else if (arg == "--help" || arg == "-h") {
            printUsage(argv[0]);
            exit(0);
        } else {
            std::cerr << "Unknown argument: " << arg << std::endl;
            printUsage(argv[0]);
            exit(1);
        }
    }

    return config;
}

int main(int argc, char* argv[]) {
    std::cout << "=== StreamGuard Stream Processor ===" << std::endl;
    std::cout << "Version: 1.0" << std::endl;
    std::cout << "Author: Jose Ortuno" << std::endl;
    std::cout << "====================================" << std::endl;
    std::cout << std::endl;

    // Parse command-line arguments
    Config config = parseArgs(argc, argv);

    try {
        // Create Metrics collector
        streamguard::Metrics metrics(config.metricsPort);

        // Create EventStore for persistence
        streamguard::EventStore store(config.dbPath);

        // Create AI Analyzer (optional - only if API key provided)
        streamguard::AIAnalyzer aiAnalyzer(config.openaiKey);
        if (aiAnalyzer.isEnabled()) {
            std::cout << "[Main] AI threat analysis enabled (OpenAI GPT-4o-mini)" << std::endl;
        } else {
            std::cout << "[Main] AI threat analysis disabled (no API key provided)" << std::endl;
        }

        // Create Anomaly Detector
        streamguard::AnomalyDetector anomalyDetector(100);  // 100 events minimum baseline
        std::cout << "[Main] Anomaly detection enabled (baseline: 100 events per user)" << std::endl;

        // Create Kafka consumer
        streamguard::KafkaConsumer consumer(
            config.broker,
            config.groupId,
            config.topic
        );

        // Register signal handlers
        std::signal(SIGINT, signalHandler);
        std::signal(SIGTERM, signalHandler);

        // Set event processing callback with shutdown check, persistence, metrics, AI analysis, and anomaly detection
        consumer.setEventCallback([&consumer, &store, &metrics, &aiAnalyzer, &anomalyDetector](const streamguard::Event& event) {
            // Check for shutdown signal
            if (shutdownRequested.load()) {
                consumer.shutdown();
                return;
            }

            // Record start time for latency measurement
            auto start = std::chrono::high_resolution_clock::now();

            // Store event in RocksDB
            if (store.put(event)) {
                // Calculate processing latency
                auto end = std::chrono::high_resolution_clock::now();
                std::chrono::duration<double> latency = end - start;

                // Record metrics
                metrics.incrementEventsProcessed(streamguard::eventTypeToString(event.event_type));
                metrics.recordProcessingLatency(latency.count());

                // Perform anomaly detection
                auto anomaly = anomalyDetector.analyze(event);
                if (anomaly) {
                    // Store anomaly in RocksDB
                    if (store.putAnomaly(*anomaly)) {
                        std::cout << "[Anomaly] User " << anomaly->user
                                  << " score=" << anomaly->anomaly_score
                                  << " reasons: ";
                        for (const auto& reason : anomaly->reasons) {
                            std::cout << reason << "; ";
                        }
                        std::cout << std::endl;
                    }

                    // Record anomaly metrics
                    std::string score_range = anomaly->anomaly_score >= 0.9 ? "critical" :
                                             anomaly->anomaly_score >= 0.8 ? "high" :
                                             anomaly->anomaly_score >= 0.7 ? "medium" : "low";
                    metrics.incrementAnomaliesDetected(anomaly->user, score_range);
                    metrics.recordAnomalyScore(anomaly->anomaly_score);
                }

                // Check threat score and record if it's a threat
                if (event.threat_score > 0.7) {
                    std::string severity = event.threat_score > 0.9 ? "critical" :
                                         event.threat_score > 0.8 ? "high" : "medium";
                    metrics.incrementThreatsDetected(severity);

                    // Perform AI threat analysis for high-risk events
                    if (aiAnalyzer.isEnabled()) {
                        auto analysis = aiAnalyzer.analyze(event);
                        if (analysis) {
                            // Store AI analysis in RocksDB
                            if (store.putAnalysis(*analysis)) {
                                std::cout << "[AI Analysis] Event " << event.event_id << " (stored)" << std::endl;
                            } else {
                                std::cout << "[AI Analysis] Event " << event.event_id << " (storage failed)" << std::endl;
                            }

                            std::cout << "  Attack Type: " << analysis->attack_type << std::endl;
                            std::cout << "  Severity: " << analysis->severity << std::endl;
                            std::cout << "  Description: " << analysis->description << std::endl;
                            std::cout << "  Confidence: " << analysis->confidence << std::endl;
                            std::cout << "  Recommendations:" << std::endl;
                            for (const auto& rec : analysis->recommendations) {
                                std::cout << "    - " << rec << std::endl;
                            }
                        }

                        // Generate and store embedding for semantic search
                        auto embedding = aiAnalyzer.generateEmbedding(event);
                        if (embedding) {
                            if (store.putEmbedding(event.event_id, *embedding)) {
                                std::cout << "[Embedding] Event " << event.event_id
                                          << " embedding stored (" << embedding->size() << " dimensions)" << std::endl;
                            } else {
                                std::cout << "[Embedding] Event " << event.event_id << " (storage failed)" << std::endl;
                            }
                        }
                    }
                }

                std::cout << "[Processor] Stored event: "
                          << "id=" << event.event_id
                          << ", type=" << streamguard::eventTypeToString(event.event_type)
                          << ", user=" << event.user
                          << ", source=" << event.source_ip
                          << ", threat=" << event.threat_score
                          << std::endl;
            } else {
                std::cerr << "[Processor] Failed to store event: " << event.event_id << std::endl;
                metrics.incrementStorageErrors();
            }
        });

        // Start consuming
        std::cout << "[Main] Starting event consumer..." << std::endl;
        consumer.start();  // Blocks until shutdown

        std::cout << "[Main] Consumer stopped" << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "[Main] Fatal error: " << e.what() << std::endl;
        return 1;
    }

    std::cout << "[Main] Stream processor terminated successfully" << std::endl;
    return 0;
}
