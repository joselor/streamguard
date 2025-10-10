#include "metrics.h"
#include <iostream>
#include <stdexcept>

namespace streamguard {

Metrics::Metrics(int port)
    : registry_(std::make_shared<prometheus::Registry>()),
      exposer_(std::make_unique<prometheus::Exposer>("0.0.0.0:" + std::to_string(port))),
      events_processed_family_(prometheus::BuildCounter()
          .Name("streamguard_events_processed_total")
          .Help("Total number of events processed")
          .Register(*registry_)),
      threats_detected_family_(prometheus::BuildCounter()
          .Name("streamguard_threats_detected_total")
          .Help("Total number of threats detected")
          .Register(*registry_)),
      storage_errors_(prometheus::BuildCounter()
          .Name("streamguard_storage_errors_total")
          .Help("Total number of storage errors")
          .Register(*registry_)),
      kafka_errors_(prometheus::BuildCounter()
          .Name("streamguard_kafka_errors_total")
          .Help("Total number of Kafka consumer errors")
          .Register(*registry_)),
      processing_latency_family_(prometheus::BuildHistogram()
          .Name("streamguard_processing_latency_seconds")
          .Help("Event processing latency in seconds")
          .Register(*registry_)),
      rocksdb_size_family_(prometheus::BuildGauge()
          .Name("streamguard_rocksdb_size_bytes")
          .Help("Current RocksDB database size in bytes")
          .Register(*registry_))
{
    // Register the registry with the exposer
    exposer_->RegisterCollectable(registry_);

    std::cout << "[Metrics] Prometheus metrics server started on port " << port << std::endl;
    std::cout << "[Metrics] Metrics available at http://localhost:" << port << "/metrics" << std::endl;
}

void Metrics::incrementEventsProcessed(const std::string& event_type) {
    events_processed_family_.Add({{"event_type", event_type}}).Increment();
}

void Metrics::incrementThreatsDetected(const std::string& severity) {
    threats_detected_family_.Add({{"severity", severity}}).Increment();
}

void Metrics::recordProcessingLatency(double latency_seconds) {
    // Create histogram with exponential buckets:
    // 0.001s, 0.0025s, 0.005s, 0.01s, 0.025s, 0.05s, 0.1s, 0.25s, 0.5s, 1s, 2.5s, 5s, 10s
    static const std::vector<double> buckets = {
        0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0
    };

    processing_latency_family_.Add({}, buckets).Observe(latency_seconds);
}

void Metrics::setRocksDBSize(uint64_t size_bytes) {
    rocksdb_size_family_.Add({}).Set(static_cast<double>(size_bytes));
}

void Metrics::incrementStorageErrors() {
    storage_errors_.Add({}).Increment();
}

void Metrics::incrementKafkaErrors() {
    kafka_errors_.Add({}).Increment();
}

} // namespace streamguard
