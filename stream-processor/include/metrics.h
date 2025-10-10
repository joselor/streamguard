#ifndef STREAMGUARD_METRICS_H
#define STREAMGUARD_METRICS_H

#include <memory>
#include <prometheus/counter.h>
#include <prometheus/histogram.h>
#include <prometheus/gauge.h>
#include <prometheus/registry.h>
#include <prometheus/exposer.h>

namespace streamguard {

/**
 * Metrics collector for StreamGuard
 *
 * Provides Prometheus-compatible metrics for monitoring:
 * - Event processing throughput
 * - Threat detection counts
 * - Processing latency
 * - Storage size
 * - Error rates
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class Metrics {
public:
    /**
     * Constructor - initializes metrics and HTTP server
     * @param port HTTP port for Prometheus scraping (default: 8080)
     */
    explicit Metrics(int port = 8080);

    /**
     * Destructor
     */
    ~Metrics() = default;

    // Disable copy and move
    Metrics(const Metrics&) = delete;
    Metrics& operator=(const Metrics&) = delete;
    Metrics(Metrics&&) = delete;
    Metrics& operator=(Metrics&&) = delete;

    /**
     * Increment events processed counter
     * @param event_type Type of event (auth_attempt, network_connection, etc.)
     */
    void incrementEventsProcessed(const std::string& event_type);

    /**
     * Increment threats detected counter
     * @param severity Severity level (low, medium, high, critical)
     */
    void incrementThreatsDetected(const std::string& severity);

    /**
     * Record processing latency
     * @param latency_seconds Processing time in seconds
     */
    void recordProcessingLatency(double latency_seconds);

    /**
     * Update RocksDB size gauge
     * @param size_bytes Current database size in bytes
     */
    void setRocksDBSize(uint64_t size_bytes);

    /**
     * Increment storage errors counter
     */
    void incrementStorageErrors();

    /**
     * Increment Kafka consumer errors counter
     */
    void incrementKafkaErrors();

    /**
     * Get the metrics registry (for testing)
     */
    std::shared_ptr<prometheus::Registry> getRegistry() const {
        return registry_;
    }

private:
    // Prometheus registry - holds all metrics
    std::shared_ptr<prometheus::Registry> registry_;

    // HTTP exposer - serves metrics on /metrics endpoint
    std::unique_ptr<prometheus::Exposer> exposer_;

    // Counters - monotonically increasing values
    prometheus::Family<prometheus::Counter>& events_processed_family_;
    prometheus::Family<prometheus::Counter>& threats_detected_family_;
    prometheus::Family<prometheus::Counter>& storage_errors_;
    prometheus::Family<prometheus::Counter>& kafka_errors_;

    // Histograms - track distributions
    prometheus::Family<prometheus::Histogram>& processing_latency_family_;

    // Gauges - values that can go up or down
    prometheus::Family<prometheus::Gauge>& rocksdb_size_family_;
};

} // namespace streamguard

#endif // STREAMGUARD_METRICS_H
