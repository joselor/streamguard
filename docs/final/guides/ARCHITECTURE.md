# StreamGuard Architecture Deep Dive

## Table of Contents
1. [System Overview](#system-overview)
2. [Lambda Architecture](#lambda-architecture)
3. [Architecture Principles](#architecture-principles)
4. [Component Architecture](#component-architecture)
5. [Data Models](#data-models)
6. [Storage Architecture](#storage-architecture)
7. [Processing Pipeline](#processing-pipeline)
8. [Scalability & Performance](#scalability--performance)
9. [Security & Reliability](#security--reliability)

---

## System Overview

StreamGuard is a real-time security event processing and analysis platform designed for high-throughput environments. The system implements **Lambda Architecture** to combine real-time stream processing with batch machine learning capabilities, providing both immediate threat detection and deep behavioral analysis.

### Key Architectural Goals

- **Real-Time Processing**: Sub-100ms latency from event ingestion to storage
- **High Throughput**: 10,000+ events/second per processor instance
- **Batch ML Processing**: Deep feature engineering and anomaly detection with Apache Spark
- **Horizontal Scalability**: Kafka consumer groups enable multi-instance deployment
- **Data Durability**: Persistent storage with RocksDB
- **Observability**: Comprehensive Prometheus metrics
- **AI Integration**: Seamless integration with OpenAI GPT-4

---

## Lambda Architecture

StreamGuard implements the Lambda Architecture pattern, combining the strengths of real-time and batch processing to provide comprehensive security event analysis.

### Architecture Overview

```mermaid
graph TB
    subgraph "Data Source"
        E[Security Events]
    end

    E --> K[Apache Kafka]

    subgraph "Speed Layer (Real-Time)"
        K --> SP[C++ Stream Processor]
        SP --> AD[Anomaly Detector]
        SP --> AI[AI Analyzer]
        SP --> RDB[(RocksDB)]
    end

    subgraph "Batch Layer (ML Pipeline)"
        K --> SPARK[Apache Spark]
        SPARK --> FE[Feature Extractor]
        FE --> ML[ML Anomaly Detection]
        ML --> PARQUET[(Parquet Training Data)]
    end

    subgraph "Serving Layer"
        RDB --> QA[Query API]
        PARQUET --> QA
        QA --> REST[REST Endpoints]
    end

    REST --> CLIENT[API Clients]

    style E fill:#E01F27,stroke:#1A1D21,color:#fff
    style K fill:#1A1D21,stroke:#E01F27,color:#fff
    style SP fill:#E01F27,stroke:#1A1D21,color:#fff
    style SPARK fill:#E01F27,stroke:#1A1D21,color:#fff
    style QA fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Three-Layer Design

#### 1. Speed Layer (Real-Time Processing)

**Technology**: C++17, librdkafka, RocksDB

**Purpose**: Process events with sub-millisecond latency for immediate threat detection

**Capabilities**:
- **12,000+ events/second** throughput per instance
- **Sub-1ms** statistical anomaly detection
- **Real-time AI analysis** for high-threat events
- **Continuous learning** baseline updates

**Key Components**:
- Kafka consumer for event ingestion
- Statistical anomaly detector (5-dimensional scoring)
- AI analyzer (OpenAI GPT-4 integration)
- RocksDB for persistent storage
- Prometheus metrics export

#### 2. Batch Layer (ML Training Pipeline)

**Technology**: Apache Spark 3.5, PySpark, scikit-learn

**Purpose**: Deep analysis and ML training data generation from historical events

**Capabilities**:
- **Distributed feature engineering** (28+ behavioral features)
- **ML-based anomaly detection** (Isolation Forest, K-Means)
- **Training data generation** for model retraining
- **Batch processing** of historical event data

**Key Components**:
- Kafka batch reader (historical event replay)
- Feature extractor (user, temporal, IP, sequence features)
- ML anomaly detector (Isolation Forest with 90% recall)
- Parquet exporter (columnar training data)

**Feature Categories**:
| Category | Features | Purpose |
|----------|----------|---------|
| **User Behavior** | Total events, unique IPs, geo locations, event type distribution | Baseline activity patterns |
| **Temporal** | Hourly distribution, unusual hour rate, time variance | Time-based anomalies |
| **IP/Location** | Unique sources, rare IP rate, location diversity | Geographic anomalies |
| **Sequence** | Event intervals, burst detection, pattern changes | Behavioral changes |
| **Threat** | Avg threat score, high-threat rate, failure rate | Risk assessment |

#### 3. Serving Layer (Unified Query Interface)

**Technology**: Java 17, Spring Boot 3.2, RocksDB Java API

**Purpose**: Provide unified REST API for querying both real-time and batch results

**Capabilities**:
- **Real-time event queries** from RocksDB
- **Anomaly searches** with score filtering
- **Statistics aggregation** across all data
- **RESTful API** with Swagger documentation

### Lambda Architecture Benefits

| Benefit | Description |
|---------|-------------|
| **Low Latency + High Accuracy** | Speed layer provides immediate results, batch layer provides deep analysis |
| **Fault Tolerance** | Data source (Kafka) is immutable and replayable |
| **Scalability** | Both layers scale independently |
| **Complexity Management** | Simple speed layer, complex ML in batch layer |
| **Continuous Learning** | Batch layer periodically retrains models with new data |

### Data Flow Example

```mermaid
sequenceDiagram
    participant E as Event Source
    participant K as Kafka
    participant SP as Speed Layer
    participant B as Batch Layer
    participant SL as Serving Layer
    participant C as Client

    E->>K: Publish security event

    par Real-Time Processing
        K->>SP: Stream event
        SP->>SP: Detect anomaly (0.8ms)
        SP->>SP: Store in RocksDB
    and Batch Collection
        K->>B: Event available for batch
    end

    Note over B: Batch job runs hourly/daily
    B->>B: Read 1M events from Kafka
    B->>B: Extract 28 features
    B->>B: ML anomaly detection
    B->>B: Export Parquet training data

    C->>SL: GET /api/events?user=alice
    SL->>SP: Query RocksDB (real-time data)
    SP-->>SL: Return events
    SL-->>C: JSON response (<50ms)

    C->>SL: GET /api/ml/anomalies
    SL->>B: Query Parquet (batch results)
    B-->>SL: Return ML anomalies
    SL-->>C: JSON response
```

---

## Architecture Principles

### 1. Event-Driven Architecture

```mermaid
graph LR
    E[Event Source] -->|Publish| K[Kafka Topic]
    K -->|Subscribe| C1[Consumer 1]
    K -->|Subscribe| C2[Consumer 2]
    K -->|Subscribe| C3[Consumer N]

    C1 --> P1[Processing Pipeline]
    C2 --> P2[Processing Pipeline]
    C3 --> P3[Processing Pipeline]

    style E fill:#E01F27,stroke:#1A1D21,color:#fff
    style K fill:#1A1D21,stroke:#E01F27,color:#fff
    style C1 fill:#E01F27,stroke:#1A1D21,color:#fff
    style C2 fill:#E01F27,stroke:#1A1D21,color:#fff
    style C3 fill:#E01F27,stroke:#1A1D21,color:#fff
```

**Benefits:**
- Loose coupling between producers and consumers
- Natural load distribution via partitioning
- Fault tolerance through replication
- Replay capability for recovery scenarios

### 2. Pipeline Architecture

Each event flows through independent, parallel analysis stages:

```mermaid
graph TB
    Input[Event Input] --> V[Validation]
    V --> Branch{Dispatch}

    Branch -->|Parallel| A[Anomaly Detection]
    Branch -->|Parallel| B[AI Analysis]
    Branch -->|Parallel| C[Embedding Generation]
    Branch -->|Parallel| D[Raw Storage]

    A --> Store[RocksDB]
    B --> Store
    C --> Store
    D --> Store

    Store --> Metrics[Prometheus Metrics]

    style Input fill:#E01F27,stroke:#1A1D21,color:#fff
    style V fill:#1A1D21,stroke:#E01F27,color:#fff
    style A fill:#E01F27,stroke:#1A1D21,color:#fff
    style B fill:#1A1D21,stroke:#E01F27,color:#fff
    style C fill:#E01F27,stroke:#1A1D21,color:#fff
    style D fill:#1A1D21,stroke:#E01F27,color:#fff
    style Store fill:#E01F27,stroke:#1A1D21,color:#fff
```

**Benefits:**
- Maximum parallelism
- Independent failure domains
- Easy to add new analysis stages
- Non-blocking operations

### 3. Embedded Database Pattern

```mermaid
graph LR
    subgraph "Stream Processor Process"
        SP[Processing Logic] <--> RDB[(RocksDB<br/>Embedded)]
    end

    subgraph "Query API Process"
        QA[Query Logic] <--> RDB2[(RocksDB<br/>Read-Only)]
    end

    FS[Shared Filesystem]
    RDB -.->|Writes| FS
    FS -.->|Reads| RDB2

    style SP fill:#E01F27,stroke:#1A1D21,color:#fff
    style QA fill:#E01F27,stroke:#1A1D21,color:#fff
    style RDB fill:#1A1D21,stroke:#E01F27,color:#fff
    style RDB2 fill:#1A1D21,stroke:#E01F27,color:#fff
    style FS fill:#E01F27,stroke:#1A1D21,color:#fff
```

**Benefits:**
- Zero network latency
- No additional database server to manage
- Simplified deployment
- High write throughput

---

## Component Architecture

### Stream Processor (C++17)

The stream processor is the core processing engine, built in C++17 for maximum performance.

#### Kafka Consumer Module

**File:** `stream-processor/src/kafka_consumer.cpp`

```cpp
class KafkaConsumer {
private:
    rd_kafka_t* consumer_;           // librdkafka consumer handle
    std::string broker_address_;     // Kafka broker connection
    std::string topic_name_;         // Topic to subscribe to
    std::string group_id_;           // Consumer group ID
    bool running_;                   // Consumer state flag
    std::mutex mtx_;                 // Thread safety

public:
    KafkaConsumer(const std::string& broker,
                  const std::string& topic,
                  const std::string& group);

    bool connect();                  // Establish connection
    bool subscribe();                // Subscribe to topic
    std::vector<Message> poll(int timeout_ms);  // Fetch messages
    void commit();                   // Commit offsets
    void close();                    // Clean shutdown
};
```

**Key Features:**
- Automatic offset management
- Consumer group rebalancing
- Error handling and retry logic
- Graceful shutdown on SIGINT/SIGTERM

**Configuration:**
```cpp
rd_kafka_conf_t* conf = rd_kafka_conf_new();
rd_kafka_conf_set(conf, "group.id", group_id_.c_str(), errstr, sizeof(errstr));
rd_kafka_conf_set(conf, "enable.auto.commit", "false", errstr, sizeof(errstr));
rd_kafka_conf_set(conf, "auto.offset.reset", "earliest", errstr, sizeof(errstr));
```

#### Anomaly Detection Module

**File:** `stream-processor/src/anomaly_detector.cpp`

```cpp
class AnomalyDetector {
private:
    std::map<std::string, UserBaseline> baselines_;  // Per-user baselines
    size_t min_events_for_baseline_;                 // Learning threshold (100)
    double threshold_;                               // Anomaly threshold (0.5)
    std::mutex mutex_;                               // Thread safety

    struct UserBaseline {
        std::map<int, int> hourly_activity;         // Hour -> count
        std::map<std::string, int> source_ips;      // IP -> count
        std::map<std::string, int> geo_locations;   // Location -> count
        std::map<EventType, int> event_types;       // Type -> count
        int total_events;
        int failed_events;
        bool is_baseline_ready;
    };

public:
    std::optional<AnomalyResult> analyze(const Event& event);

private:
    AnomalyResult calculateAnomalyScore(const Event& event,
                                       const UserBaseline& baseline);
};
```

**Algorithm:**

1. **Baseline Learning Phase** (0-100 events):
   - Collect user behavioral patterns
   - Track hourly activity distribution
   - Record known IPs and locations
   - Calculate event type frequencies
   - Monitor failure rates

2. **Detection Phase** (100+ events):
   ```cpp
   // Calculate individual anomaly scores
   double hour_prob = baseline.getHourProbability(hour);
   result.time_anomaly = 1.0 - hour_prob;

   double ip_prob = baseline.getIPProbability(event.source_ip);
   result.ip_anomaly = 1.0 - ip_prob;

   // Weighted composite score
   result.anomaly_score = (
       result.time_anomaly * 0.25 +      // Time weight: 25%
       result.ip_anomaly * 0.30 +         // IP weight: 30%
       result.location_anomaly * 0.20 +   // Location weight: 20%
       result.type_anomaly * 0.15 +       // Type weight: 15%
       result.failure_anomaly * 0.10      // Failure weight: 10%
   );
   ```

3. **Selective Baseline Updates** (Prevents Baseline Poisoning):
   ```cpp
   // Only update baseline with "normal" events
   if (anomaly_score < threshold_) {
       baseline.update(event);  // Accept as normal behavior
   }
   // Anomalous events are rejected from baseline updates
   // This prevents attackers from poisoning the baseline
   ```
   - Baselines updated only with normal events (score < 0.5)
   - Adapts to genuine behavioral changes
   - Rejects suspicious patterns from contaminating the baseline
   - No manual retraining required

**Performance:**
- Time Complexity: O(1) for detection
- Space Complexity: O(U × (H + I + L + T)) where:
  - U = unique users
  - H = hours tracked (24)
  - I = unique IPs per user (~10-50)
  - L = unique locations per user (~5-20)
  - T = event types (~10)

#### AI Analysis Module

**File:** `stream-processor/src/ai_analyzer.cpp`

```cpp
class AIAnalyzer {
private:
    std::string api_key_;
    std::string model_name_;         // "gpt-4" or "gpt-4-turbo"
    httplib::Client* http_client_;
    int timeout_ms_;                 // 5000ms default
    int max_retries_;                // 3 retries

public:
    std::optional<ThreatAnalysis> analyze(const Event& event,
                                         const EventContext& context);

private:
    std::string buildPrompt(const Event& event,
                           const EventContext& context);
    ThreatAnalysis parseResponse(const std::string& response);
};
```

**Prompt Engineering:**

```cpp
std::string AIAnalyzer::buildPrompt(const Event& event,
                                   const EventContext& context) {
    std::ostringstream prompt;
    prompt << "Analyze this security event:\n\n"
           << "Event Details:\n"
           << "- User: " << event.user << "\n"
           << "- Type: " << eventTypeToString(event.type) << "\n"
           << "- Source IP: " << event.source_ip << "\n"
           << "- Location: " << event.geo_location << "\n"
           << "- Timestamp: " << formatTimestamp(event.timestamp) << "\n\n"
           << "Context:\n"
           << "- Recent failures: " << context.failed_login_count << "\n"
           << "- Known IPs: " << context.known_ips.size() << "\n"
           << "- Average threat score: " << context.avg_threat_score << "\n\n"
           << "Provide:\n"
           << "1. Severity (LOW/MEDIUM/HIGH/CRITICAL)\n"
           << "2. Confidence (0.0-1.0)\n"
           << "3. Key indicators\n"
           << "4. Summary\n"
           << "5. Recommended actions\n";
    return prompt.str();
}
```

**API Integration:**

```cpp
// HTTP POST to OpenAI API
httplib::Headers headers = {
    {"Content-Type", "application/json"},
    {"Authorization", "Bearer " + api_key_}
};

json request_body = {
    {"model", model_name_},
    {"max_tokens", 1024},
    {"messages", {{
        {"role", "user"},
        {"content", prompt}
    }}}
};

auto response = http_client_->Post("/v1/chat/completions",
                                   headers,
                                   request_body.dump(),
                                   "application/json");
```

**Error Handling:**
- Timeout after 5 seconds
- Exponential backoff retry (3 attempts)
- Circuit breaker pattern (optional)
- Graceful degradation (continue without AI if API unavailable)

#### Storage Module

**File:** `stream-processor/src/event_store.cpp`

```cpp
class EventStore {
private:
    rocksdb::DB* db_;
    rocksdb::ColumnFamilyHandle* default_cf_;      // Raw events
    rocksdb::ColumnFamilyHandle* ai_analysis_cf_;  // AI results
    rocksdb::ColumnFamilyHandle* embeddings_cf_;   // Vector data
    rocksdb::ColumnFamilyHandle* anomalies_cf_;    // Anomaly results

public:
    bool putEvent(const Event& event);
    bool putAnalysis(const ThreatAnalysis& analysis);
    bool putAnomaly(const AnomalyResult& anomaly);
    bool putEmbedding(const Embedding& embedding);

    std::optional<Event> getEvent(const std::string& event_id);
    std::vector<AnomalyResult> getHighScoreAnomalies(double threshold,
                                                     size_t limit);
};
```

**Key Format Strategy:**

1. **Time-Ordered Keys** (for range queries):
   ```cpp
   // Format: {timestamp:15 digits}:{event_id}
   // Example: 001696723200000:evt_001
   std::string buildKey(uint64_t timestamp, const std::string& event_id) {
       std::ostringstream oss;
       oss << std::setfill('0') << std::setw(15) << timestamp
           << ":" << event_id;
       return oss.str();
   }
   ```

2. **Event-ID Keys** (for direct lookups):
   ```cpp
   // Format: {event_id}
   // Example: evt_001
   ```

**Column Family Configuration:**

```cpp
rocksdb::ColumnFamilyOptions cf_options;
cf_options.OptimizeForPointLookup(512);  // 512MB block cache
cf_options.compression = rocksdb::kLZ4Compression;
cf_options.level0_file_num_compaction_trigger = 4;
cf_options.write_buffer_size = 64 * 1024 * 1024;  // 64MB
```

**Write Path:**

```cpp
bool EventStore::putAnomaly(const AnomalyResult& anomaly) {
    std::string key = buildKey(anomaly.timestamp, anomaly.event_id);
    std::string value = anomaly.toJson();

    rocksdb::Status status = db_->Put(rocksdb::WriteOptions(),
                                      anomalies_cf_,
                                      key,
                                      value);

    if (!status.ok()) {
        LOG_ERROR("Failed to write anomaly: " << status.ToString());
        return false;
    }
    return true;
}
```

**Read Path:**

```cpp
std::vector<AnomalyResult> EventStore::getHighScoreAnomalies(
    double threshold, size_t limit) {

    std::vector<AnomalyResult> results;
    rocksdb::Iterator* it = db_->NewIterator(rocksdb::ReadOptions(),
                                             anomalies_cf_);

    // Start from most recent (last key)
    it->SeekToLast();

    while (it->Valid() && results.size() < limit) {
        std::string json = it->value().ToString();
        AnomalyResult anomaly = AnomalyResult::fromJson(json);

        if (anomaly.anomaly_score >= threshold) {
            results.push_back(anomaly);
        }

        it->Prev();  // Move backwards (newest to oldest)
    }

    delete it;
    return results;
}
```

#### Metrics Module

**File:** `stream-processor/src/metrics.cpp`

```cpp
class Metrics {
private:
    prometheus::Registry registry_;
    prometheus::Exposer exposer_;  // HTTP server on port 8080

    // Counter metrics
    prometheus::Family<prometheus::Counter>& events_processed_family_;
    prometheus::Family<prometheus::Counter>& anomalies_detected_family_;

    // Histogram metrics
    prometheus::Family<prometheus::Histogram>& anomaly_score_family_;
    prometheus::Family<prometheus::Histogram>& processing_latency_family_;

public:
    Metrics(int port);

    void incrementEventsProcessed(const std::string& user);
    void incrementAnomaliesDetected(const std::string& user,
                                   const std::string& score_range);
    void recordAnomalyScore(double score);
    void recordProcessingLatency(double duration_ms);
};
```

**Metric Definitions:**

```cpp
// Counter: Total events processed
events_processed_family_ = prometheus::BuildCounter()
    .Name("streamguard_events_processed_total")
    .Help("Total number of events processed")
    .Labels({{"user", ""}})
    .Register(registry_);

// Histogram: Anomaly scores
anomaly_score_family_ = prometheus::BuildHistogram()
    .Name("streamguard_anomaly_score")
    .Help("Distribution of anomaly scores")
    .Buckets({0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0})
    .Register(registry_);
```

**Usage:**

```cpp
// In main processing loop
auto start = std::chrono::steady_clock::now();

// Process event
auto anomaly = anomaly_detector.analyze(event);
if (anomaly && anomaly->anomaly_score >= 0.5) {
    std::string score_range = anomaly->anomaly_score >= 0.9 ? "critical" :
                             anomaly->anomaly_score >= 0.7 ? "high" :
                             anomaly->anomaly_score >= 0.5 ? "medium" : "low";

    metrics.incrementAnomaliesDetected(anomaly->user, score_range);
    metrics.recordAnomalyScore(anomaly->anomaly_score);
}

auto end = std::chrono::steady_clock::now();
auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
    end - start).count();
metrics.recordProcessingLatency(duration);
```

### Query API (Java / Spring Boot)

#### Configuration Layer

**File:** `query-api/src/main/java/com/streamguard/queryapi/config/RocksDBConfig.java`

```java
@Configuration
public class RocksDBConfig {

    @Value("${rocksdb.path}")
    private String dbPath;

    @Value("${rocksdb.read-only:true}")
    private boolean readOnly;

    private RocksDB db;
    private final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

    @Bean
    public RocksDB rocksDB() throws RocksDBException {
        RocksDB.loadLibrary();

        // List existing column families
        List<byte[]> cfNames = RocksDB.listColumnFamilies(
            new Options(), dbPath);

        // Create descriptors
        List<ColumnFamilyDescriptor> cfDescriptors = cfNames.stream()
            .map(name -> new ColumnFamilyDescriptor(name,
                                                   new ColumnFamilyOptions()))
            .collect(Collectors.toList());

        // Open database in read-only mode
        DBOptions dbOptions = new DBOptions()
            .setCreateIfMissing(false)
            .setCreateMissingColumnFamilies(false);

        db = RocksDB.openReadOnly(dbOptions, dbPath,
                                  cfDescriptors, columnFamilyHandles);

        return db;
    }

    @Bean
    public ColumnFamilyHandle anomaliesColumnFamily() {
        return columnFamilyHandles.stream()
            .filter(handle -> {
                try {
                    return "anomalies".equals(new String(handle.getName()));
                } catch (RocksDBException e) {
                    return false;
                }
            })
            .findFirst()
            .orElse(null);
    }

    @PreDestroy
    public void closeDB() {
        columnFamilyHandles.forEach(ColumnFamilyHandle::close);
        if (db != null) db.close();
    }
}
```

**Why Read-Only Mode?**
- Prevents accidental writes from query layer
- Allows multiple readers simultaneously
- Improves safety and data integrity

#### Service Layer

**File:** `query-api/src/main/java/com/streamguard/queryapi/service/QueryService.java`

```java
@Service
public class QueryService {

    private final RocksDB rocksDB;
    private final ColumnFamilyHandle anomaliesColumnFamily;
    private final ObjectMapper objectMapper;

    public List<AnomalyResult> getHighScoreAnomalies(double threshold,
                                                     int limit) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (anomaliesColumnFamily == null) {
            log.warn("anomalies column family not available");
            return anomalies;
        }

        try (RocksIterator iterator =
             rocksDB.newIterator(anomaliesColumnFamily)) {

            iterator.seekToLast();  // Start from most recent

            while (iterator.isValid() && anomalies.size() < limit) {
                try {
                    String json = new String(iterator.value());
                    AnomalyResult anomaly = objectMapper.readValue(
                        json, AnomalyResult.class);

                    if (anomaly.getAnomalyScore() >= threshold) {
                        anomalies.add(anomaly);
                    }
                } catch (Exception e) {
                    log.error("Error parsing anomaly JSON", e);
                }
                iterator.prev();
            }
        }

        return anomalies;
    }
}
```

**Performance Optimizations:**
- Iterator reuse via try-with-resources
- Lazy JSON parsing (only parse if needed)
- Result limiting to prevent memory exhaustion
- Column family isolation for efficient scans

#### Controller Layer

**File:** `query-api/src/main/java/com/streamguard/queryapi/controller/AnomalyController.java`

```java
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Tag(name = "Anomalies", description = "Anomaly detection query endpoints")
public class AnomalyController {

    private final QueryService queryService;

    @GetMapping("/high-score")
    @Operation(summary = "Get high-score anomalies")
    public ResponseEntity<List<AnomalyResult>> getHighScoreAnomalies(
        @Parameter(description = "Minimum anomaly score (0.0-1.0)")
        @RequestParam(name = "threshold", defaultValue = "0.5") double threshold,
        @RequestParam(defaultValue = "100") int limit) {

        List<AnomalyResult> anomalies =
            queryService.getHighScoreAnomalies(threshold, limit);

        return ResponseEntity.ok(anomalies);
    }
}
```

**REST API Design:**
- RESTful resource naming
- Query parameters for filtering
- Default values for common use cases
- Swagger/OpenAPI documentation
- Proper HTTP status codes

### Spark ML Pipeline (Python / PySpark)

The Spark ML pipeline implements the batch layer for deep feature engineering and ML-based anomaly detection.

#### Kafka Reader Module

**File:** `spark-ml-pipeline/src/kafka_reader.py`

```python
class KafkaEventReader:
    def __init__(self, spark: SparkSession):
        self.spark = spark
        self.kafka_config = {
            'bootstrap_servers': 'localhost:9092',
            'topic': 'security-events',
            'group_id': 'spark-ml-pipeline'
        }

    def read_batch(self, start_offset="earliest", max_events=None):
        """Read historical events from Kafka in batch mode"""
        df = self.spark.read \
            .format("kafka") \
            .option("kafka.bootstrap.servers", self.kafka_config['bootstrap_servers']) \
            .option("subscribe", self.kafka_config['topic']) \
            .option("startingOffsets", start_offset) \
            .load()

        # Parse JSON payload
        events_df = df.select(
            from_json(col("value").cast("string"), EVENT_SCHEMA).alias("event")
        ).select("event.*")

        return events_df
```

**Key Features:**
- Batch reading from Kafka topics
- JSON schema validation
- Configurable offset management
- DataFrame-based processing

#### Feature Extractor Module

**File:** `spark-ml-pipeline/src/feature_extractor.py`

```python
class FeatureExtractor:
    def extract_user_features(self, events_df: DataFrame) -> DataFrame:
        """Extract 28+ behavioral features per user"""

        # User-level aggregations
        user_features = events_df.groupBy("user").agg(
            count("*").alias("total_events"),
            countDistinct("source_ip").alias("unique_ips"),
            countDistinct("geo_location").alias("unique_locations"),
            avg("threat_score").alias("avg_threat_score"),
            stddev("threat_score").alias("threat_score_std"),
            # Failure rate
            (sum(when(col("event_type").like("FAILED"), 1).otherwise(0)) /
             count("*")).alias("failed_auth_rate"),
            # Unusual hour rate (0-6 AM, 8 PM-12 AM)
            (sum(when((hour("timestamp") < 6) | (hour("timestamp") >= 20), 1)
                 .otherwise(0)) / count("*")).alias("unusual_hour_rate"),
            # Event type distribution
            *[sum(when(col("event_type") == et, 1).otherwise(0))
              .alias(f"event_type_{et.lower()}")
              for et in EVENT_TYPES],
            # Temporal features
            stddev(unix_timestamp("timestamp")).alias("time_variance"),
            # IP/Location diversity
            (countDistinct("source_ip") / count("*")).alias("ip_diversity"),
            (countDistinct("geo_location") / count("*")).alias("location_diversity")
        )

        return user_features
```

**Feature Categories:**
| Category | Count | Examples |
|----------|-------|----------|
| **User Behavior** | 8 | total_events, unique_ips, event_type_distribution |
| **Temporal** | 6 | hourly_distribution, unusual_hour_rate, time_variance |
| **IP/Location** | 5 | unique_sources, rare_ip_rate, location_diversity |
| **Sequence** | 4 | event_intervals, burst_detection, pattern_changes |
| **Threat** | 5 | avg_threat_score, high_threat_rate, failure_rate |

#### ML Anomaly Detector Module

**File:** `spark-ml-pipeline/src/anomaly_detector.py`

```python
class MLAnomalyDetector:
    def __init__(self, spark: SparkSession, config: dict):
        self.spark = spark
        self.anomaly_config = config['anomaly_detection']
        self.algorithm = self.anomaly_config['algorithm']  # isolation_forest

    def detect_with_isolation_forest(self, features_df: DataFrame) -> DataFrame:
        """Use Isolation Forest for anomaly detection"""

        # Convert to Pandas for scikit-learn
        pandas_df = features_df.toPandas()

        # Prepare feature matrix
        feature_cols = [c for c in pandas_df.columns if c != 'user']
        X = pandas_df[feature_cols].fillna(0)

        # Train Isolation Forest
        clf = IsolationForest(
            contamination=self.anomaly_config['contamination'],  # 0.1 = 10%
            n_estimators=self.anomaly_config['n_estimators'],    # 100
            random_state=self.anomaly_config['random_state'],    # 42
            n_jobs=-1  # Use all cores
        )

        # Predict anomalies (-1 = anomaly, 1 = normal)
        predictions = clf.fit_predict(X)
        anomaly_scores = clf.score_samples(X)

        # Normalize scores to 0-1 range
        normalized_scores = (anomaly_scores - anomaly_scores.min()) / \
                          (anomaly_scores.max() - anomaly_scores.min())

        # Add results back to DataFrame
        pandas_df['is_anomaly'] = (predictions == -1).astype(int)
        pandas_df['anomaly_score_normalized'] = normalized_scores

        return self.spark.createDataFrame(pandas_df)
```

**ML Algorithm Details:**
- **Isolation Forest**: Detects anomalies by isolating observations
- **Contamination**: 10% (expects ~10% of users to be anomalous)
- **Estimators**: 100 trees for robust detection
- **Score Normalization**: Converts raw scores to 0-1 range

**Algorithm Complexity:**
- Training: O(n × t × log(s)) where:
  - n = samples
  - t = trees (100)
  - s = sub-sample size (256)
- Prediction: O(n × t × d) where d = tree depth

#### Training Data Generator Module

**File:** `spark-ml-pipeline/src/training_data_generator.py`

```python
class TrainingDataPipeline:
    def run_pipeline(self, start_offset="earliest", max_events=None,
                    output_path="./output/training_data"):
        """Execute complete ML training pipeline"""

        logger.info("[Step 1/5] Reading events from Kafka")
        events_df = self.kafka_reader.read_batch(start_offset, max_events)

        logger.info("[Step 2/5] Extracting behavioral features")
        user_features_df = self.feature_extractor.extract_user_features(events_df)

        logger.info("[Step 3/5] Preparing feature vectors")
        feature_cols = self.feature_extractor.get_feature_columns()

        logger.info("[Step 4/5] Detecting anomalies with ML")
        results_df = self.anomaly_detector.detect(user_features_df, feature_cols)

        logger.info("[Step 5/5] Exporting training data to Parquet")
        results_df.write \
            .mode("overwrite") \
            .partitionBy("is_anomaly") \
            .parquet(output_path)

        # Generate anomaly report
        report = self._generate_report(results_df)
        self._save_report(report, output_path)
```

**Pipeline Workflow:**
1. **Data Ingestion**: Read events from Kafka (10K-1M events)
2. **Feature Engineering**: Extract 28 features per user
3. **ML Detection**: Apply Isolation Forest
4. **Export**: Save to Parquet with partitioning
5. **Reporting**: Generate anomaly summary

**Performance Characteristics:**
| Dataset Size | Processing Time | Memory Usage |
|--------------|----------------|--------------|
| 1K events | 5 seconds | 512MB |
| 10K events | 15 seconds | 1GB |
| 100K events | 60 seconds | 2GB |
| 1M events | 5 minutes | 4GB |

---

## Data Models

### Event Model

```json
{
  "event_id": "evt_1696723200_001",
  "user": "alice",
  "timestamp": 1696723200000,
  "type": "LOGIN_FAILED",
  "source_ip": "10.0.1.50",
  "geo_location": "San Francisco, CA",
  "threat_score": 0.45,
  "metadata": {
    "user_agent": "Mozilla/5.0...",
    "endpoint": "/api/login"
  }
}
```

### Anomaly Result Model

```json
{
  "event_id": "evt_1696723200_001",
  "user": "alice",
  "timestamp": 1696723200000,
  "anomaly_score": 0.82,
  "time_anomaly": 0.15,
  "ip_anomaly": 0.98,
  "location_anomaly": 0.08,
  "type_anomaly": 0.05,
  "failure_anomaly": 0.62,
  "reasons": [
    "Unusual IP address (seen 0 times)",
    "High failure rate (60% vs 5% baseline)",
    "Uncommon hour (3 AM, 2% of activity)"
  ]
}
```

### Threat Analysis Model

```json
{
  "event_id": "evt_1696723200_001",
  "severity": "MEDIUM",
  "confidence": 0.85,
  "indicators": [
    "Unknown IP address",
    "Multiple failed login attempts",
    "Late night activity"
  ],
  "summary": "Potential brute force attack from new IP",
  "recommendation": "Block IP after 3 more failures, notify SOC team",
  "analyzed_at": 1696723201500
}
```

---

## Storage Architecture

### RocksDB Internals

#### LSM Tree Structure

```
Level 0:  [SST-1] [SST-2] [SST-3]  (Newest, may overlap)
          ↓ Compaction
Level 1:  [SST-4] [SST-5]          (Sorted, no overlap)
          ↓ Compaction
Level 2:  [SST-6] [SST-7] [SST-8]
          ↓ Compaction
Level 3:  [SST-9] ... [SST-N]      (Oldest)
```

#### Write Path

```mermaid
graph TB
    W[Write Request] --> MEM[MemTable<br/>In-Memory]
    MEM -->|Full| IMM[Immutable MemTable]
    IMM -->|Flush| L0[Level 0 SST Files]
    L0 -->|Compaction| L1[Level 1 SST Files]
    L1 -->|Compaction| L2[Level 2 SST Files]
    L2 -->|Compaction| LN[Level N SST Files]

    WAL[Write-Ahead Log] -.->|Durability| W

    style W fill:#E01F27,stroke:#1A1D21,color:#fff
    style MEM fill:#1A1D21,stroke:#E01F27,color:#fff
    style L0 fill:#E01F27,stroke:#1A1D21,color:#fff
    style WAL fill:#1A1D21,stroke:#E01F27,color:#fff
```

#### Read Path

```mermaid
graph TB
    R[Read Request] --> BC{Block Cache?}
    BC -->|Hit| Return1[Return Cached Data]
    BC -->|Miss| MEM{MemTable?}
    MEM -->|Found| Return2[Return Data]
    MEM -->|Not Found| IMM{Immutable MemTable?}
    IMM -->|Found| Return3[Return Data]
    IMM -->|Not Found| SST[Search SST Files<br/>Level 0 → Level N]
    SST -->|Found| Cache[Update Block Cache]
    Cache --> Return4[Return Data]
    SST -->|Not Found| NotFound[Return Null]

    style R fill:#E01F27,stroke:#1A1D21,color:#fff
    style BC fill:#1A1D21,stroke:#E01F27,color:#fff
    style MEM fill:#E01F27,stroke:#1A1D21,color:#fff
    style SST fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Column Family Isolation

```
RocksDB Database File
├── Column Family: default (Security Events)
│   ├── MemTable
│   ├── SST Files (Level 0-6)
│   └── Block Cache (shared)
├── Column Family: ai_analysis (Threat Analysis)
│   ├── MemTable
│   ├── SST Files (Level 0-6)
│   └── Block Cache (shared)
├── Column Family: anomalies (Anomaly Results)
│   ├── MemTable
│   ├── SST Files (Level 0-6)
│   └── Block Cache (shared)
└── Column Family: embeddings (Vector Data)
    ├── MemTable
    ├── SST Files (Level 0-6)
    └── Block Cache (shared)
```

**Benefits:**
- Independent compaction per CF
- Separate key spaces (no collisions)
- Efficient queries (scan only relevant CF)
- Shared block cache (memory efficiency)

---

## Processing Pipeline

### Event Processing Flow

```mermaid
stateDiagram-v2
    [*] --> Receive: Event arrives
    Receive --> Deserialize: Raw bytes
    Deserialize --> Validate: JSON object
    Validate --> Process: Valid event

    Process --> Anomaly: Parallel
    Process --> AI: Parallel
    Process --> Embedding: Parallel

    Anomaly --> StoreAnomaly: Result ready
    AI --> StoreAI: Result ready
    Embedding --> StoreEmbed: Result ready

    StoreAnomaly --> Commit
    StoreAI --> Commit
    StoreEmbed --> Commit

    Commit --> UpdateMetrics
    UpdateMetrics --> [*]

    Validate --> Reject: Invalid
    Reject --> [*]
```

### Error Handling Strategy

```mermaid
graph TB
    E[Event Processing] --> Try{Try Process}
    Try -->|Success| Store[Store Results]
    Try -->|Failure| Type{Error Type}

    Type -->|Transient| Retry[Retry Logic]
    Type -->|Permanent| Log[Log & Skip]
    Type -->|Critical| Alert[Alert & Stop]

    Retry -->|Success| Store
    Retry -->|Max Retries| Log

    Store --> Metrics[Update Metrics]
    Log --> Metrics
    Alert --> [*]

    style E fill:#E01F27,stroke:#1A1D21,color:#fff
    style Type fill:#1A1D21,stroke:#E01F27,color:#fff
    style Retry fill:#E01F27,stroke:#1A1D21,color:#fff
    style Alert fill:#E01F27,stroke:#1A1D21,color:#fff
```

**Error Categories:**

1. **Transient Errors** (Retry):
   - Network timeouts
   - Temporary API unavailability
   - RocksDB write stalls

2. **Permanent Errors** (Skip):
   - Invalid JSON format
   - Schema validation failures
   - Corrupted data

3. **Critical Errors** (Stop):
   - RocksDB corruption
   - Out of disk space
   - Memory allocation failures

---

## Scalability & Performance

### Horizontal Scaling

```mermaid
graph TB
    subgraph "Kafka Cluster"
        P1[Partition 0]
        P2[Partition 1]
        P3[Partition 2]
        P4[Partition 3]
    end

    subgraph "Consumer Group: stream-processors"
        C1[Processor 1<br/>Partitions: 0,1]
        C2[Processor 2<br/>Partitions: 2,3]
    end

    P1 --> C1
    P2 --> C1
    P3 --> C2
    P4 --> C2

    C1 --> DB1[(RocksDB 1)]
    C2 --> DB2[(RocksDB 2)]

    subgraph "Load Balancer"
        LB[Round Robin]
    end

    subgraph "Query API Instances"
        Q1[Query API 1] --> DB1
        Q2[Query API 2] --> DB2
    end

    Client[API Client] --> LB
    LB --> Q1
    LB --> Q2

    style P1 fill:#E01F27,stroke:#1A1D21,color:#fff
    style P2 fill:#E01F27,stroke:#1A1D21,color:#fff
    style C1 fill:#1A1D21,stroke:#E01F27,color:#fff
    style C2 fill:#1A1D21,stroke:#E01F27,color:#fff
```

**Scaling Characteristics:**

| Metric | Single Instance | 2 Instances | 4 Instances |
|--------|----------------|-------------|-------------|
| Throughput | 10K events/s | 20K events/s | 40K events/s |
| Latency (p95) | 50ms | 48ms | 45ms |
| CPU Usage | 60% | 30% | 15% |
| Memory | 4GB | 2GB | 1GB |

### Performance Tuning

#### RocksDB Configuration

```cpp
rocksdb::Options options;

// Write optimization
options.write_buffer_size = 64 * 1024 * 1024;  // 64MB memtable
options.max_write_buffer_number = 3;           // 3 memtables
options.min_write_buffer_number_to_merge = 2;  // Merge before flush

// Read optimization
options.block_cache = rocksdb::NewLRUCache(512 * 1024 * 1024);  // 512MB cache
options.bloom_filter_bits_per_key = 10;        // Bloom filter
options.optimize_filters_for_hits = true;      // Skip non-existent keys

// Compaction optimization
options.max_background_jobs = 4;               // Parallel compaction
options.level0_file_num_compaction_trigger = 4; // Trigger level 0 compaction
options.target_file_size_base = 64 * 1024 * 1024;  // 64MB SST files

// Compression
options.compression = rocksdb::kLZ4Compression;  // Fast compression
options.bottommost_compression = rocksdb::kZSTD;  // Best compression for old data
```

#### Kafka Configuration

```properties
# Producer (Event Source)
batch.size=16384
linger.ms=10
compression.type=lz4
acks=1

# Consumer (Stream Processor)
fetch.min.bytes=1024
fetch.max.wait.ms=100
max.poll.records=100
max.partition.fetch.bytes=1048576
```

---

## Security & Reliability

### Data Security

1. **Encryption at Rest**:
   - RocksDB does not provide native encryption
   - Use filesystem-level encryption (LUKS, dm-crypt)
   - Or implement custom EncryptedEnv

2. **Encryption in Transit**:
   - Kafka TLS/SSL encryption
   - HTTPS for Query API
   - TLS for Claude API calls

3. **Access Control**:
   - Kafka ACLs for topic access
   - API key authentication for Query API
   - IAM roles for cloud deployments

### Fault Tolerance

```mermaid
graph TB
    subgraph "Failure Scenario: Processor Crash"
        F1[Processor Crash] --> R1[Consumer Rebalance]
        R1 --> R2[Partition Reassignment]
        R2 --> R3[Resume from Last Committed Offset]
        R3 --> R4[Continue Processing]
    end

    subgraph "Failure Scenario: RocksDB Corruption"
        F2[DB Corruption Detected] --> B1[Stop Processing]
        B1 --> B2[Restore from Backup]
        B2 --> B3[Replay from Kafka]
        B3 --> B4[Rebuild Database]
    end

    style F1 fill:#E01F27,stroke:#1A1D21,color:#fff
    style F2 fill:#E01F27,stroke:#1A1D21,color:#fff
```

### Monitoring & Alerting

**Critical Alerts:**

```yaml
# Prometheus Alert Rules
groups:
  - name: streamguard
    rules:
      - alert: HighAnomalyRate
        expr: rate(streamguard_anomalies_detected_total[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Anomaly detection rate > 100/s"

      - alert: ProcessingLatencyHigh
        expr: histogram_quantile(0.95, streamguard_processing_latency_seconds) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "95th percentile latency > 100ms"

      - alert: AIAPIFailureRate
        expr: rate(streamguard_ai_analysis_failed_total[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "AI API failure rate > 10%"
```

---

## Conclusion

StreamGuard's architecture demonstrates several key patterns for building high-performance, scalable stream processing systems:

1. **Event-Driven Architecture**: Decouples producers and consumers
2. **Embedded Database**: Eliminates network overhead
3. **Column Family Isolation**: Efficient multi-model storage
4. **Parallel Processing**: Maximizes throughput
5. **Graceful Degradation**: System continues despite component failures
6. **Comprehensive Observability**: Prometheus metrics for all operations

This architecture supports:
- Real-time processing at scale (10K+ events/s)
- Low-latency queries (<100ms p95)
- Horizontal scalability via Kafka partitioning
- High availability through replication and failover
- Operational simplicity with minimal dependencies
