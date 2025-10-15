# Stream Processor - Speed Layer

[![C++17](https://img.shields.io/badge/C%2B%2B-17-blue.svg)](https://isocpp.org/)
[![RocksDB](https://img.shields.io/badge/RocksDB-8.9-orange.svg)](https://rocksdb.org/)
[![librdkafka](https://img.shields.io/badge/librdkafka-latest-green.svg)](https://github.com/edenhill/librdkafka)
[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-red.svg)](https://prometheus.io/)

The **Stream Processor** is the **Speed Layer** in StreamGuard's Lambda Architecture, providing real-time security event processing with sub-millisecond latency. Built in C++17 for maximum performance, it consumes events from Apache Kafka, performs statistical anomaly detection, stores events in RocksDB, and exposes Prometheus metrics.

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                     Stream Processor                          │
│                      (Speed Layer)                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────────┐  │
│  │   Kafka     │───▶│   Anomaly    │───▶│   RocksDB      │  │
│  │  Consumer   │    │   Detector   │    │    Store       │  │
│  └─────────────┘    └──────────────┘    └────────────────┘  │
│                            │                                  │
│                            ▼                                  │
│                    ┌──────────────┐                          │
│                    │ AI Analyzer  │                          │
│                    │  (Claude)    │                          │
│                    └──────────────┘                          │
│                            │                                  │
│                            ▼                                  │
│                    ┌──────────────┐                          │
│                    │  Prometheus  │                          │
│                    │   Metrics    │ :8080/metrics            │
│                    └──────────────┘                          │
└──────────────────────────────────────────────────────────────┘
```

## Features

### Real-Time Processing
- **Sub-1ms latency**: Ultra-low latency event processing
- **High throughput**: Handles thousands of events per second
- **Parallel processing**: Multi-threaded architecture for maximum performance

### Statistical Anomaly Detection
- **5-Dimensional Scoring**: Analyzes multiple behavioral factors
  - **Time Anomaly** (25% weight): Unusual login hours
  - **IP Anomaly** (30% weight): New or rare source IPs
  - **Location Anomaly** (20% weight): New or unusual geo-locations
  - **Type Anomaly** (15% weight): Unusual event types for user
  - **Failure Anomaly** (10% weight): Spikes in failure rates

- **User Baseline Learning**: Builds individual behavioral profiles
  - Minimum 100 events to establish baseline
  - Tracks hourly activity patterns, source IPs, locations, event types
  - Calculates probabilities for each dimension

- **Selective Baseline Updates**: Prevents baseline poisoning
  - Only updates baseline with normal events (score < threshold)
  - Allows adaptation to genuine behavioral changes
  - Rejects suspicious patterns from poisoning the baseline

- **Configurable Threshold**: Default 0.5 (0.0-1.0 scale)
  - Anomaly score ≥ 0.5 triggers detection
  - Lowered from 0.7 for improved sensitivity

### AI-Powered Threat Analysis
- **OpenAI GPT-4**: Advanced threat classification
- **Contextual Analysis**: Enriches anomalies with AI-generated insights
- **Severity Scoring**: CRITICAL, HIGH, MEDIUM, LOW classifications

### Persistent Storage
- **RocksDB 8.9**: High-performance embedded database
- **Column Families**:
  - `default`: Security events (key: event_id, value: JSON)
  - `ai_analysis`: AI threat analysis results
  - `embeddings`: Vector embeddings for similarity search
  - `anomalies`: Detected anomalies (key: event_id, value: AnomalyResult JSON)

### Observability
- **Prometheus Metrics**: Real-time performance monitoring
  - `streamguard_events_processed_total`: Event counter
  - `streamguard_processing_latency_seconds`: Processing time histogram
  - `streamguard_threats_detected_total`: Threat counter by severity
  - `streamguard_kafka_errors_total`: Kafka error counter
  - `streamguard_storage_errors_total`: Storage error counter

## Prerequisites

### System Requirements
- **macOS**: M1/M2/M3 (ARM64) or Intel (x86_64)
- **Linux**: x86_64 or ARM64
- **CMake**: 3.20 or higher
- **C++ Compiler**: GCC 9+, Clang 10+, or Apple Clang 12+

### Dependencies

Install dependencies using Homebrew (macOS) or your system package manager:

```bash
# macOS (Homebrew)
brew install cmake
brew install nlohmann-json
brew install librdkafka
brew install rocksdb
brew install prometheus-cpp
brew install curl

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y cmake g++ libjson-c-dev librdkafka-dev librocksdb-dev libcurl4-openssl-dev
# prometheus-cpp requires manual installation on Linux
```

#### Dependencies Summary
- **nlohmann_json** (3.2.0+): JSON parsing and serialization
- **librdkafka**: Kafka C++ client library
- **RocksDB**: Embedded key-value store
- **prometheus-cpp**: Prometheus metrics exporter
- **CURL**: HTTP client for AI API calls

## Build Instructions

### Standard Build

```bash
cd stream-processor
mkdir -p build
cd build
cmake ..
make -j$(nproc)
```

### Build Options

```bash
# Debug build (with symbols, no optimization)
cmake -DCMAKE_BUILD_TYPE=Debug ..
make -j$(nproc)

# Release build (optimized, no symbols)
cmake -DCMAKE_BUILD_TYPE=Release ..
make -j$(nproc)

# With tests (requires Google Test)
cmake -DBUILD_TESTING=ON ..
make -j$(nproc)
ctest --output-on-failure
```

### Build Artifacts

After successful build:
- **Executable**: `build/stream-processor`
- **Libraries**: `build/libstreamguard_*.a` (event, consumer, store, metrics, ai, anomaly)
- **Test Binary** (if enabled): `build/event-tests`
- **Data Directory**: `build/data/` (RocksDB files)

## Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `OPENAI_API_KEY` | OpenAI API key for GPT-4 | None | **Yes** |
| `ROCKSDB_PATH` | RocksDB database path | `./data/events.db` | No |

**Important:** Set `ROCKSDB_PATH` to an absolute path or relative from project root.
Example: `ROCKSDB_PATH=/Users/you/streamguard/data/events.db` or `ROCKSDB_PATH=./data/events.db`

### Running the Processor

#### Option 1: Using Startup Script (Recommended)

```bash
# Configure environment (edit .env file first)
cp .env.example .env

# Start stream processor
../scripts/start-stream-processor.sh
```

#### Option 2: Manual Start

```bash
# Export required environment variables
export OPENAI_API_KEY="your-api-key-here"

# Run with default configuration (database at ../../data/events.db)
./build/stream-processor --db ../../data/events.db

# Run with command-line arguments
./build/stream-processor \
    --broker localhost:9092 \
    --topic security-events \
    --group streamguard-processor \
    --db ../../data/events.db \
    --metrics-port 8080
```

### Command-Line Arguments

The stream processor accepts the following command-line arguments:

```bash
./stream-processor [options]

Options:
  --kafka-brokers <brokers>   Kafka bootstrap servers (default: localhost:9092)
  --kafka-topic <topic>       Kafka topic name (default: security-events)
  --kafka-group <group>       Consumer group ID (default: stream-processor)
  --rocksdb-path <path>       RocksDB database path (default: build/data/events.db)
  --metrics-port <port>       Prometheus metrics port (default: 8080)
  --threshold <value>         Anomaly detection threshold (default: 0.5)
  --help                      Show help message
```

## Anomaly Detection Algorithm

### Overview

The anomaly detector uses a **probabilistic baseline model** to detect deviations from normal user behavior:

```cpp
// Composite anomaly score (weighted average)
anomaly_score =
    time_anomaly     * 0.25 +  // Time weight: 25%
    ip_anomaly       * 0.30 +  // IP weight: 30%
    location_anomaly * 0.20 +  // Location weight: 20%
    type_anomaly     * 0.15 +  // Type weight: 15%
    failure_anomaly  * 0.10;   // Failure weight: 10%

// Detect if score >= threshold (default: 0.5)
```

### Dimensional Scoring

#### 1. Time Anomaly
Detects unusual login hours based on user's historical patterns.

```cpp
double hour_prob = baseline.getHourProbability(event_hour);
time_anomaly = 1.0 - hour_prob;  // Low probability = high anomaly

// Example: User normally logs in 9am-5pm
// 3am login → hour_prob = 0.01 → time_anomaly = 0.99
```

#### 2. IP Anomaly
Flags new or rarely-seen source IP addresses.

```cpp
double ip_prob = baseline.getSourceIPProbability(event.source_ip);
ip_anomaly = 1.0 - ip_prob;

// Example: User always from 192.168.1.100
// New IP 10.0.0.50 → ip_prob = 0.01 → ip_anomaly = 0.99
```

#### 3. Location Anomaly
Detects new or unusual geo-locations.

```cpp
double location_prob = baseline.getGeoLocationProbability(event.geo_location);
location_anomaly = 1.0 - location_prob;

// Example: User always from "San Francisco, CA"
// New location "Moscow, Russia" → location_prob = 0.01 → location_anomaly = 0.99
```

#### 4. Type Anomaly
Identifies unusual event types for the user.

```cpp
double type_prob = baseline.getEventTypeProbability(event.event_type);
type_anomaly = 1.0 - type_prob;

// Example: User always does LOGIN events
// ADMIN_ACCESS event → type_prob = 0.01 → type_anomaly = 0.99
```

#### 5. Failure Anomaly
Detects spikes in authentication failures.

```cpp
if (event.status == FAILED && baseline.failure_rate < 0.1) {
    failure_anomaly = 0.8;  // High anomaly for rare failures
}

// Example: User normally has 2% failure rate
// Failed login → failure_anomaly = 0.8
```

### Baseline Learning Strategy

#### Initial Learning Phase
```cpp
// Collect 100 events per user before anomaly detection starts
if (baseline.total_events < min_events_for_baseline) {
    baseline.update(event);  // Learn baseline
    return std::nullopt;     // No anomaly detection yet
}
```

#### Selective Updates (Baseline Poisoning Prevention)
```cpp
// Only update baseline with "normal" events
if (anomaly_score < threshold) {
    baseline.update(event);  // Accept as normal behavior
} else {
    // Reject suspicious event from baseline update
    // Prevents attackers from poisoning the baseline
}
```

### Detection Examples

#### Example 1: Credential Stuffing Attack
```
User: john.doe@example.com
Normal Behavior:
  - Login hours: 9am-5pm (Mon-Fri)
  - Source IP: 192.168.1.100 (office)
  - Location: San Francisco, CA
  - Failure rate: 2%

Detected Anomaly:
  - Time: 3:47am (time_anomaly = 0.99)
  - IP: 45.142.120.10 (ip_anomaly = 0.99)
  - Location: Moscow, Russia (location_anomaly = 0.99)
  - Status: FAILED (failure_anomaly = 0.8)

Composite Score:
  = 0.99*0.25 + 0.99*0.30 + 0.99*0.20 + 0.0*0.15 + 0.8*0.10
  = 0.8225 (≥ 0.5 threshold → ANOMALY DETECTED)

Reasons:
  - "Unusual login time (03:47)"
  - "New or unusual source IP: 45.142.120.10"
  - "New or unusual location: Moscow, Russia"
  - "User typically has low failure rate"
```

#### Example 2: Account Takeover (Subtle)
```
User: jane.smith@example.com
Normal Behavior:
  - Login hours: 8am-6pm
  - Source IPs: 10.0.0.50, 10.0.0.51 (home network)
  - Location: Austin, TX
  - Event types: LOGIN (95%), PASSWORD_CHANGE (5%)

Detected Anomaly:
  - Time: 10am (time_anomaly = 0.0, normal hour)
  - IP: 203.0.113.42 (ip_anomaly = 0.99, new IP)
  - Location: Austin, TX (location_anomaly = 0.0, same city)
  - Event type: ADMIN_ACCESS (type_anomaly = 0.99, never seen)

Composite Score:
  = 0.0*0.25 + 0.99*0.30 + 0.0*0.20 + 0.99*0.15 + 0.0*0.10
  = 0.4455 (< 0.5 threshold → NOT flagged)

# Note: With threshold 0.5, some subtle attacks may not be detected
# Lowering threshold to 0.4 would catch this case
```

## RocksDB Column Families

### Storage Schema

| Column Family | Key | Value | Purpose |
|---------------|-----|-------|---------|
| `default` | `event_id` (UUID) | JSON event object | Primary event storage |
| `ai_analysis` | `event_id` (UUID) | JSON AI analysis result | AI threat classifications |
| `embeddings` | `event_id` (UUID) | Binary vector (float[]) | Event embeddings for similarity search |
| `anomalies` | `event_id` (UUID) | JSON AnomalyResult object | Detected anomalies with scoring breakdown |

### Example Storage Operations

```cpp
// Store event
rocksdb::Status s = db->Put(rocksdb::WriteOptions(),
                            default_cf,
                            event.event_id,
                            event.toJson());

// Store anomaly
if (anomaly_result) {
    db->Put(rocksdb::WriteOptions(),
            anomalies_cf,
            event.event_id,
            anomaly_result->toJson());
}

// Read event
std::string value;
rocksdb::Status s = db->Get(rocksdb::ReadOptions(),
                            default_cf,
                            event_id,
                            &value);
Event event = Event::fromJson(value);
```

## Prometheus Metrics

### Exposed Metrics

Access metrics at `http://localhost:8080/metrics`

#### Event Metrics
```prometheus
# Total events processed
streamguard_events_processed_total{} 380000

# Processing latency histogram
streamguard_processing_latency_seconds_bucket{le="0.0001"} 350000
streamguard_processing_latency_seconds_bucket{le="0.001"} 375000
streamguard_processing_latency_seconds_bucket{le="0.01"} 380000
streamguard_processing_latency_seconds_sum 45.2
streamguard_processing_latency_seconds_count 380000
```

#### Threat Metrics
```prometheus
# Threats detected by severity
streamguard_threats_detected_total{severity="CRITICAL"} 245
streamguard_threats_detected_total{severity="HIGH"} 892
streamguard_threats_detected_total{severity="MEDIUM"} 678
streamguard_threats_detected_total{severity="LOW"} 325
```

#### Error Metrics
```prometheus
# Kafka errors
streamguard_kafka_errors_total{} 5

# Storage errors
streamguard_storage_errors_total{} 2
```

### Grafana Integration

Import the StreamGuard Performance Dashboard:
```bash
# Dashboard located at: monitoring/grafana/dashboards/streamguard-performance.json
# Access Grafana at: http://localhost:3000
# Default credentials: admin/admin
```

## Integration with Other Components

### Upstream: Kafka (Event Source)
```yaml
Topic: security-events
Partitions: 3
Replication Factor: 1
Message Format: JSON (Event schema)

# Stream processor consumes from Kafka
Consumer Group: stream-processor
Auto-commit: false (manual commit after processing)
```

### Downstream: Query API (Serving Layer)
```yaml
# Query API reads from RocksDB in read-only mode
RocksDB Path: ../stream-processor/build/data/events.db
Read-Only: true
Column Families: [default, ai_analysis, embeddings, anomalies]

# API Endpoints:
GET /api/events/{id}           → default CF
GET /api/events/{id}/analysis  → ai_analysis CF
GET /api/anomalies             → anomalies CF
```

### Downstream: Spark ML Pipeline (Batch Layer)
```yaml
# Spark reads RocksDB data for batch ML training
Input: RocksDB snapshots or Parquet exports
Output: ML models, batch anomaly scores
Frequency: Hourly/Daily

# Integration:
1. Stream processor writes to RocksDB
2. Spark reads historical data
3. Spark trains Isolation Forest model
4. Spark generates batch anomaly scores
5. Results merged with real-time scores in serving layer
```

## Performance Tuning

### RocksDB Tuning
```cpp
// Increase write buffer for better throughput
rocksdb::Options options;
options.write_buffer_size = 64 * 1024 * 1024;  // 64MB
options.max_write_buffer_number = 3;
options.target_file_size_base = 64 * 1024 * 1024;

// Enable compression
options.compression = rocksdb::kLZ4Compression;

// Tune for SSD
options.allow_mmap_reads = true;
options.allow_mmap_writes = false;
```

### Kafka Tuning
```cpp
// Increase fetch size for better throughput
conf->set("fetch.message.max.bytes", "10485760", errstr);  // 10MB
conf->set("max.partition.fetch.bytes", "10485760", errstr);

// Reduce latency
conf->set("fetch.wait.max.ms", "10", errstr);  // 10ms
```

### Anomaly Detection Tuning
```cpp
// Adjust threshold for sensitivity
detector.setThreshold(0.4);  // More sensitive (more anomalies)
detector.setThreshold(0.6);  // Less sensitive (fewer anomalies)

// Adjust minimum baseline events
AnomalyDetector detector(50);  // Faster baseline (less accurate)
AnomalyDetector detector(200); // Slower baseline (more accurate)
```

## Troubleshooting

### Common Issues

#### Issue: Kafka connection failed
```bash
# Check Kafka is running
docker ps | grep kafka

# Verify broker address
echo "KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS"

# Test connection
kafkacat -b localhost:9092 -L
```

#### Issue: RocksDB open failed
```bash
# Check RocksDB path exists
ls -la build/data/

# Create directory if missing
mkdir -p build/data

# Check permissions
chmod 755 build/data
```

#### Issue: No metrics exposed
```bash
# Check metrics port is accessible
curl http://localhost:8080/metrics

# Verify port not in use
lsof -i :8080

# Try custom port
METRICS_PORT=9090 ./build/stream-processor
```

#### Issue: AI analysis failures
```bash
# Verify API key is set
echo "OPENAI_API_KEY=$OPENAI_API_KEY"

# Check API connectivity
curl -H "Authorization: Bearer $OPENAI_API_KEY" https://api.openai.com/v1/models

# Check API quota/rate limits
```

### Debug Mode

Enable verbose logging:
```bash
# Rebuild with debug symbols
cmake -DCMAKE_BUILD_TYPE=Debug ..
make -j$(nproc)

# Run with logging
./build/stream-processor 2>&1 | tee processor.log
```

### Process Management

```bash
# Check if processor is running
ps aux | grep stream-processor

# Kill zombie processes
pkill -9 stream-processor

# Monitor resource usage
top -p $(pgrep stream-processor)
```

## Development

### Project Structure
```
stream-processor/
├── CMakeLists.txt              # Build configuration
├── README.md                   # This file
├── include/                    # Public headers
│   ├── event.h                 # Event data structures
│   ├── kafka_consumer.h        # Kafka consumer
│   ├── event_store.h           # RocksDB storage
│   ├── metrics.h               # Prometheus metrics
│   ├── ai_analyzer.h           # AI integration
│   └── anomaly_detector.h      # Anomaly detection
├── src/                        # Implementation
│   ├── main.cpp                # Entry point
│   ├── event.cpp
│   ├── kafka_consumer.cpp
│   ├── event_store.cpp
│   ├── metrics.cpp
│   ├── ai_analyzer.cpp
│   └── anomaly_detector.cpp
├── test/                       # Unit tests
│   └── event-test.cpp
└── build/                      # Build artifacts (gitignored)
    ├── stream-processor        # Executable
    ├── lib*.a                  # Static libraries
    └── data/                   # RocksDB data files
```

### Adding New Features

#### Example: Add Custom Metric
```cpp
// In metrics.h
class Metrics {
public:
    void incrementCustomMetric();
private:
    prometheus::Counter* custom_metric_;
};

// In metrics.cpp
Metrics::Metrics(int port) {
    // ... existing code ...
    custom_metric_ = &prometheus::BuildCounter()
        .Name("streamguard_custom_metric_total")
        .Help("Custom metric description")
        .Register(*registry_)
        .Add({});
}

void Metrics::incrementCustomMetric() {
    custom_metric_->Increment();
}

// In main.cpp
metrics.incrementCustomMetric();
```

### Running Tests

```bash
# Build with tests
cmake -DBUILD_TESTING=ON ..
make -j$(nproc)

# Run all tests
ctest --output-on-failure

# Run specific test
./build/event-tests
```

## License

Copyright © 2024 StreamGuard. All rights reserved.

## Support

For issues, questions, or contributions:
- **GitHub Issues**: https://github.com/your-org/streamguard/issues
- **Documentation**: docs/final/README.md
- **Architecture Guide**: docs/final/guides/ARCHITECTURE.md
- **API Reference**: docs/final/api/API_REFERENCE.md
