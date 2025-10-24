#include <iostream>
#include <csignal>
#include <atomic>
#include <thread>
#include <chrono>
#include <cstdlib>
#include "kafka_consumer.h"
#include "event_store.h"
#include "event.h"
#include "metrics.h"
#include "anomaly_detector.h"
#include "ai_analyzer.h"

// Global flag for graceful shutdown
std::atomic<bool> running_(true);

void signalHandler(int signal) {
    std::cout << "\n[Main] Received signal " << signal << ", shutting down gracefully..." << std::endl;
    running_ = false;
}

int main(int argc, char* argv[]) {
    std::cout << "[Main] StreamGuard Stream Processor starting..." << std::endl;

    // Default configuration
    std::string broker = "localhost:9092";
    std::string topic = "security-events";
    std::string groupId = "streamguard-processor";
    std::string dbPath = "./data/events.db";
    int metricsPort = 8080;

    // Parse command line arguments
    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];
        if (arg == "--broker" && i + 1 < argc) {
            broker = argv[++i];
        } else if (arg == "--topic" && i + 1 < argc) {
            topic = argv[++i];
        } else if (arg == "--group" && i + 1 < argc) {
            groupId = argv[++i];
        } else if (arg == "--db" && i + 1 < argc) {
            dbPath = argv[++i];
        } else if (arg == "--metrics-port" && i + 1 < argc) {
            metricsPort = std::stoi(argv[++i]);
        }
    }

    std::cout << "[Main] Configuration:" << std::endl;
    std::cout << "[Main]   Kafka broker: " << broker << std::endl;
    std::cout << "[Main]   Topic: " << topic << std::endl;
    std::cout << "[Main]   Consumer group: " << groupId << std::endl;
    std::cout << "[Main]   Database path: " << dbPath << std::endl;
    std::cout << "[Main]   Metrics port: " << metricsPort << std::endl;

    // Check for AI analysis configuration
    bool enableAI = false;
    std::string openaiApiKey;

    const char* apiKeyEnv = std::getenv("OPENAI_API_KEY");
    if (apiKeyEnv != nullptr && std::string(apiKeyEnv).length() > 0) {
        openaiApiKey = apiKeyEnv;

        std::cout << "\n[AI] OPENAI_API_KEY detected" << std::endl;
        std::cout << "[AI] Enable AI-powered threat analysis? (yes/no) [default: no]: ";
        std::cout.flush();

        std::string response;
        std::getline(std::cin, response);

        // Trim whitespace
        response.erase(0, response.find_first_not_of(" \t\n\r"));
        response.erase(response.find_last_not_of(" \t\n\r") + 1);

        enableAI = (response == "yes" || response == "y" || response == "YES" || response == "Y");

        if (enableAI) {
            std::cout << "[AI] ✓ AI analysis ENABLED - Will analyze high-threat and anomalous events" << std::endl;
            std::cout << "[AI]   Model: GPT-4o-mini" << std::endl;
            std::cout << "[AI]   Trigger: threat_score >= 0.7 OR anomaly detected" << std::endl;
        } else {
            std::cout << "[AI] AI analysis DISABLED (default)" << std::endl;
        }
    } else {
        std::cout << "\n[AI] OPENAI_API_KEY not found - AI analysis disabled" << std::endl;
    }
    std::cout << std::endl;

    // Setup signal handlers
    std::signal(SIGINT, signalHandler);
    std::signal(SIGTERM, signalHandler);

    try {
        // Initialize metrics server
        std::cout << "[Main] Starting metrics server on port " << metricsPort << "..." << std::endl;
        streamguard::Metrics metrics(metricsPort);

        // Initialize event store
        std::cout << "[Main] Opening RocksDB at: " << dbPath << std::endl;
        streamguard::EventStore store(dbPath);

        // Initialize anomaly detector (100 events per user for baseline)
        std::cout << "[Main] Initializing anomaly detector..." << std::endl;
        streamguard::AnomalyDetector detector(100);

        // Initialize AI analyzer if enabled
        std::unique_ptr<streamguard::AIAnalyzer> aiAnalyzer;
        if (enableAI) {
            std::cout << "[Main] Initializing AI analyzer (GPT-4o-mini)..." << std::endl;
            aiAnalyzer = std::make_unique<streamguard::AIAnalyzer>(openaiApiKey);
        }

        // Initialize Kafka consumer
        std::cout << "[Main] Creating Kafka consumer..." << std::endl;
        streamguard::KafkaConsumer consumer(broker, groupId, topic);

        // Setup event callback with anomaly detection and AI analysis
        consumer.setEventCallback([&](const streamguard::Event& event) {
            // 1. Store event in RocksDB
            if (store.put(event)) {
                metrics.incrementEventsProcessed(streamguard::eventTypeToString(event.event_type));

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

            // 2. Anomaly detection
            bool isAnomalous = false;
            auto anomaly_result = detector.analyze(event);

            if (anomaly_result.has_value()) {
                isAnomalous = true;

                // Anomaly detected!
                std::cout << "[Anomaly] User " << anomaly_result->user
                          << " score=" << anomaly_result->anomaly_score
                          << " reasons: ";

                for (const auto& reason : anomaly_result->reasons) {
                    std::cout << reason << "; ";
                }
                std::cout << std::endl;

                // Store anomaly in RocksDB
                if (store.putAnomaly(*anomaly_result)) {
                    std::string severity = anomaly_result->anomaly_score >= 0.9 ? "critical" :
                                          anomaly_result->anomaly_score >= 0.8 ? "high" :
                                          anomaly_result->anomaly_score >= 0.7 ? "medium" : "low";

                    metrics.incrementAnomaliesDetected(anomaly_result->user, severity);
                    metrics.incrementThreatsDetected(severity);
                    metrics.recordAnomalyScore(anomaly_result->anomaly_score);
                } else {
                    std::cerr << "[Processor] Failed to store anomaly for event: "
                              << event.event_id << std::endl;
                }
            }

            // 3. AI-powered threat analysis (selective)
            // Only analyze if: (a) AI is enabled AND (b) high-threat OR anomalous
            bool shouldAnalyzeWithAI = aiAnalyzer && aiAnalyzer->isEnabled() &&
                                       (event.threat_score >= 0.7 || isAnomalous);

            if (shouldAnalyzeWithAI) {
                std::cout << "[AI] Analyzing event " << event.event_id
                          << " (threat=" << event.threat_score
                          << ", anomaly=" << (isAnomalous ? "yes" : "no") << ")..." << std::endl;

                auto analysis = aiAnalyzer->analyze(event);

                if (analysis.has_value()) {
                    std::cout << "[AI] ✓ Analysis complete: severity=" << analysis->severity
                              << ", confidence=" << analysis->confidence << std::endl;

                    // Store AI analysis in RocksDB
                    if (store.putAnalysis(*analysis)) {
                        std::cout << "[AI] ✓ Stored AI analysis for event: " << event.event_id << std::endl;

                        // Report AI-detected threat to Prometheus metrics
                        metrics.incrementThreatsDetected(analysis->severity);
                    } else {
                        std::cerr << "[AI] Failed to store AI analysis for event: " << event.event_id << std::endl;
                    }
                } else {
                    std::cerr << "[AI] Analysis failed for event: " << event.event_id << std::endl;
                }
            }
        });

        // Start consuming (blocks until shutdown)
        // Pass the global running flag so consumer can respond to signals
        std::cout << "[Main] Starting event consumer..." << std::endl;
        consumer.start(&running_);  // Blocks until shutdown or signal received

        std::cout << "[Main] Consumer stopped" << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "[Main] Fatal error: " << e.what() << std::endl;
        return 1;
    }

    std::cout << "[Main] Stream processor terminated successfully" << std::endl;
    return 0;
}