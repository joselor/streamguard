# StreamGuard Class Diagrams

Comprehensive UML class diagrams for all major components in the StreamGuard system.

## Stream Processor - Core Classes

### Event Processing Module

```mermaid
classDiagram
    class Event {
        +string event_id
        +string user
        +uint64_t timestamp
        +EventType type
        +string source_ip
        +string geo_location
        +double threat_score
        +map~string,string~ metadata
        +string toJson() const
        +fromJson(json_str) Event$
    }

    class EventType {
        <<enumeration>>
        LOGIN_SUCCESS
        LOGIN_FAILED
        LOGOUT
        PRIVILEGE_ESCALATION
        DATA_ACCESS
        DATA_MODIFICATION
        SYSTEM_CHANGE
        NETWORK_CONNECTION
        FILE_ACCESS
        UNKNOWN
    }

    class EventValidator {
        -double min_threat_score
        -double max_threat_score
        +EventValidator()
        +validate(event) bool
        +validateTimestamp(timestamp) bool
        +validateThreatScore(score) bool
        +validateUser(user) bool
        +getValidationError() string
    }

    class EventDeserializer {
        -ObjectMapper mapper
        +deserialize(json_bytes) Event
        +deserializeBatch(json_array) vector~Event~
        +handleParseError(error) void
    }

    Event --> EventType : uses
    EventValidator --> Event : validates
    EventDeserializer --> Event : creates

    style Event fill:#E01F27,stroke:#1A1D21,color:#fff
    style EventType fill:#1A1D21,stroke:#E01F27,color:#fff
    style EventValidator fill:#E01F27,stroke:#1A1D21,color:#fff
    style EventDeserializer fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Kafka Consumer Module

```mermaid
classDiagram
    class KafkaConsumer {
        -rd_kafka_t* consumer
        -rd_kafka_topic_partition_list_t* topics
        -string broker_address
        -string topic_name
        -string group_id
        -bool running
        -mutex mtx
        +KafkaConsumer(broker, topic, group)
        +~KafkaConsumer()
        +connect() bool
        +subscribe() bool
        +poll(timeout_ms) vector~Message~
        +commit() void
        +close() void
        +isRunning() bool
        -handleRebalance(event) void
        -handleError(error) void
    }

    class Message {
        +string key
        +vector~byte~ payload
        +int64_t offset
        +int32_t partition
        +int64_t timestamp
        +size_t size
        +bool hasError
        +string errorMessage
        +toEvent() Event
    }

    class ConsumerConfig {
        +string broker
        +string topic
        +string group_id
        +int session_timeout_ms
        +int max_poll_records
        +string auto_offset_reset
        +bool enable_auto_commit
        +validate() bool
    }

    class OffsetManager {
        -map~int32_t, int64_t~ committed_offsets
        -mutex offset_mtx
        +getLastCommitted(partition) int64_t
        +markForCommit(partition, offset) void
        +commitAll() void
        +reset(partition) void
    }

    KafkaConsumer --> Message : produces
    KafkaConsumer --> ConsumerConfig : uses
    KafkaConsumer --> OffsetManager : manages
    Message --> Event : converts to

    style KafkaConsumer fill:#E01F27,stroke:#1A1D21,color:#fff
    style Message fill:#1A1D21,stroke:#E01F27,color:#fff
    style ConsumerConfig fill:#E01F27,stroke:#1A1D21,color:#fff
    style OffsetManager fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Anomaly Detection Module

```mermaid
classDiagram
    class AnomalyDetector {
        -map~string, UserBaseline~ baselines_
        -size_t min_events_for_baseline_
        -double threshold_
        -mutex mutex_
        +AnomalyDetector(min_events)
        +analyze(event) optional~AnomalyResult~
        +setThreshold(threshold) void
        +getBaseline(user) UserBaseline*
        -updateBaseline(user, event) void
        -calculateAnomalyScore(event, baseline) AnomalyResult
        -calculateTimeProbability(hour, baseline) double
        -calculateIPProbability(ip, baseline) double
        -calculateLocationProbability(location, baseline) double
    }

    class UserBaseline {
        +string user
        +map~int, int~ hourly_activity
        +map~string, int~ source_ips
        +map~string, int~ geo_locations
        +map~EventType, int~ event_types
        +int total_events
        +int failed_events
        +double avg_threat_score
        +bool is_baseline_ready
        +update(event) void
        +getHourProbability(hour) double
        +getIPProbability(ip) double
        +getLocationProbability(location) double
        +getTypeProbability(type) double
        +getFailureRate() double
        +isReady() bool
    }

    class AnomalyResult {
        +string event_id
        +string user
        +uint64_t timestamp
        +double anomaly_score
        +double time_anomaly
        +double ip_anomaly
        +double location_anomaly
        +double type_anomaly
        +double failure_anomaly
        +vector~string~ reasons
        +toJson() string
        +fromJson(json_str) AnomalyResult$
        +isHighRisk() bool
    }

    class AnomalyScorer {
        <<utility>>
        +WEIGHT_TIME : double = 0.25$
        +WEIGHT_IP : double = 0.30$
        +WEIGHT_LOCATION : double = 0.20$
        +WEIGHT_TYPE : double = 0.15$
        +WEIGHT_FAILURE : double = 0.10$
        +calculateCompositeScore(result) double$
        +classifyRiskLevel(score) string$
    }

    AnomalyDetector --> UserBaseline : manages
    AnomalyDetector --> AnomalyResult : produces
    AnomalyDetector --> AnomalyScorer : uses
    UserBaseline --> Event : analyzes

    style AnomalyDetector fill:#E01F27,stroke:#1A1D21,color:#fff
    style UserBaseline fill:#1A1D21,stroke:#E01F27,color:#fff
    style AnomalyResult fill:#E01F27,stroke:#1A1D21,color:#fff
    style AnomalyScorer fill:#1A1D21,stroke:#E01F27,color:#fff
```

### AI Analysis Module

```mermaid
classDiagram
    class AIAnalyzer {
        -string api_key
        -string model_name
        -HttpClient* http_client
        -int timeout_ms
        -int max_retries
        +AIAnalyzer(api_key, model)
        +analyze(event, context) ThreatAnalysis
        +analyzeWithEmbedding(event) pair~ThreatAnalysis,Embedding~
        +isAvailable() bool
        -buildPrompt(event, context) string
        -callClaudeAPI(prompt) string
        -parseResponse(response) ThreatAnalysis
        -handleAPIError(error) void
    }

    class ThreatAnalysis {
        +string event_id
        +string severity
        +double confidence
        +vector~string~ indicators
        +string summary
        +string recommendation
        +uint64_t analyzed_at
        +toJson() string
        +fromJson(json_str) ThreatAnalysis$
        +isCritical() bool
    }

    class Embedding {
        +string event_id
        +vector~double~ vector_data
        +int dimensions
        +uint64_t created_at
        +toJson() string
        +fromJson(json_str) Embedding$
        +cosineSimilarity(other) double
        +normalize() void
    }

    class EventContext {
        +vector~Event~ recent_events
        +int failed_login_count
        +set~string~ known_ips
        +double avg_threat_score
        +addEvent(event) void
        +getRecentFailures(user, minutes) int
        +isKnownIP(ip) bool
        +getSummary() string
    }

    class HttpClient {
        -httplib_Client* client
        -map~string,string~ default_headers
        +HttpClient(base_url)
        +post(path, body, headers) Response
        +setTimeout(ms) void
        +setRetryPolicy(retries, backoff) void
        -handleConnectionError(error) void
    }

    AIAnalyzer --> ThreatAnalysis : produces
    AIAnalyzer --> Embedding : produces
    AIAnalyzer --> EventContext : uses
    AIAnalyzer --> HttpClient : uses

    style AIAnalyzer fill:#E01F27,stroke:#1A1D21,color:#fff
    style ThreatAnalysis fill:#1A1D21,stroke:#E01F27,color:#fff
    style Embedding fill:#E01F27,stroke:#1A1D21,color:#fff
    style EventContext fill:#1A1D21,stroke:#E01F27,color:#fff
    style HttpClient fill:#E01F27,stroke:#1A1D21,color:#fff
```

### Storage Module

```mermaid
classDiagram
    class EventStore {
        -rocksdb_DB* db_
        -rocksdb_WriteOptions write_options
        -rocksdb_ReadOptions read_options
        -ColumnFamilyHandle* default_cf_
        -ColumnFamilyHandle* ai_analysis_cf_
        -ColumnFamilyHandle* embeddings_cf_
        -ColumnFamilyHandle* anomalies_cf_
        -string db_path_
        -bool read_only_
        +EventStore(db_path, read_only)
        +~EventStore()
        +open() bool
        +close() void
        +putEvent(event) bool
        +getEvent(event_id) optional~Event~
        +putAnalysis(analysis) bool
        +getAnalysis(event_id) optional~ThreatAnalysis~
        +putAnomaly(anomaly) bool
        +getAnomaly(event_id) optional~AnomalyResult~
        +putEmbedding(embedding) bool
        +getEmbedding(event_id) optional~Embedding~
        +getEventsByTimeRange(start, end, limit) vector~Event~
        +getAnomaliesByUser(user, limit) vector~AnomalyResult~
        +getHighScoreAnomalies(threshold, limit) vector~AnomalyResult~
        +getEventCount() uint64_t
        +getAnomalyCount() uint64_t
        -createIterator(cf) Iterator*
        -buildTimeOrderedKey(timestamp, event_id) string
    }

    class ColumnFamilyHandle {
        <<rocksdb>>
        +getName() string
        +GetID() uint32_t
    }

    class RocksIterator {
        <<rocksdb>>
        +SeekToFirst() void
        +SeekToLast() void
        +Seek(key) void
        +Next() void
        +Prev() void
        +Valid() bool
        +key() Slice
        +value() Slice
    }

    class DBOptions {
        <<rocksdb>>
        +create_if_missing : bool
        +create_missing_column_families : bool
        +max_open_files : int
        +write_buffer_size : size_t
        +max_background_jobs : int
    }

    class ColumnFamilyOptions {
        <<rocksdb>>
        +compression : CompressionType
        +block_cache_size : size_t
        +bloom_filter_bits : int
        +level0_file_num : int
    }

    EventStore --> ColumnFamilyHandle : manages
    EventStore --> RocksIterator : creates
    EventStore --> DBOptions : uses
    EventStore --> ColumnFamilyOptions : uses
    EventStore --> Event : stores
    EventStore --> ThreatAnalysis : stores
    EventStore --> AnomalyResult : stores
    EventStore --> Embedding : stores

    style EventStore fill:#E01F27,stroke:#1A1D21,color:#fff
    style ColumnFamilyHandle fill:#1A1D21,stroke:#E01F27,color:#fff
    style RocksIterator fill:#E01F27,stroke:#1A1D21,color:#fff
    style DBOptions fill:#1A1D21,stroke:#E01F27,color:#fff
    style ColumnFamilyOptions fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Metrics Module

```mermaid
classDiagram
    class Metrics {
        -Registry* registry_
        -Exposer* exposer_
        -Family~Counter~* events_processed_family_
        -Family~Counter~* events_failed_family_
        -Family~Counter~* anomalies_detected_family_
        -Family~Counter~* ai_analyses_family_
        -Family~Histogram~* processing_latency_family_
        -Family~Histogram~* anomaly_score_family_
        -map~string,Counter*~ counters_
        -map~string,Histogram*~ histograms_
        +Metrics(port)
        +~Metrics()
        +incrementEventsProcessed(user) void
        +incrementEventsFailed(error_type) void
        +incrementAnomaliesDetected(user, score_range) void
        +incrementAIAnalyses(severity) void
        +recordProcessingLatency(duration_ms) void
        +recordAnomalyScore(score) void
        -getOrCreateCounter(name, labels) Counter*
        -getOrCreateHistogram(name, labels, buckets) Histogram*
    }

    class Registry {
        <<prometheus>>
        +Register(family) void
        +Collect() vector~MetricFamily~
    }

    class Exposer {
        <<prometheus>>
        +Exposer(address)
        +RegisterCollectable(registry) void
    }

    class Counter {
        <<prometheus>>
        +Increment(value) void
        +Value() double
    }

    class Histogram {
        <<prometheus>>
        +Observe(value) void
        +Count() uint64_t
        +Sum() double
    }

    class Family~T~ {
        <<prometheus>>
        +Add(labels) T&
        +Remove(labels) void
    }

    Metrics --> Registry : uses
    Metrics --> Exposer : uses
    Metrics --> Counter : creates
    Metrics --> Histogram : creates
    Metrics --> Family : manages

    style Metrics fill:#E01F27,stroke:#1A1D21,color:#fff
    style Registry fill:#1A1D21,stroke:#E01F27,color:#fff
    style Exposer fill:#E01F27,stroke:#1A1D21,color:#fff
    style Counter fill:#1A1D21,stroke:#E01F27,color:#fff
    style Histogram fill:#1A1D21,stroke:#E01F27,color:#fff
```

## Query API - Java Classes

### Controller Layer

```mermaid
classDiagram
    class EventController {
        -QueryService queryService
        +EventController(queryService)
        +getLatestEvents(limit) ResponseEntity~List~SecurityEvent~~
        +getEventById(eventId) ResponseEntity~SecurityEvent~
        +getEventsByThreatScore(minScore, limit) ResponseEntity~List~SecurityEvent~~
        +getEventsByTimeRange(startTime, endTime, limit) ResponseEntity~List~SecurityEvent~~
        +getEventCount() ResponseEntity~Long~
    }

    class AnomalyController {
        -QueryService queryService
        +AnomalyController(queryService)
        +getLatestAnomalies(limit) ResponseEntity~List~AnomalyResult~~
        +getAnomalyByEventId(eventId) ResponseEntity~AnomalyResult~
        +getAnomaliesByUser(user, limit) ResponseEntity~List~AnomalyResult~~
        +getHighScoreAnomalies(threshold, limit) ResponseEntity~List~AnomalyResult~~
        +getAnomalyCount() ResponseEntity~Long~
        +getAnomaliesByTimeRange(startTime, endTime, limit) ResponseEntity~List~AnomalyResult~~
    }

    class AnalysisController {
        -QueryService queryService
        +AnalysisController(queryService)
        +getLatestAnalyses(limit) ResponseEntity~List~ThreatAnalysis~~
        +getAnalysisByEventId(eventId) ResponseEntity~ThreatAnalysis~
        +getAnalysesBySeverity(severity, limit) ResponseEntity~List~ThreatAnalysis~~
        +getAnalysisCount() ResponseEntity~Long~
    }

    class StatsController {
        -QueryService queryService
        +StatsController(queryService)
        +getStatsSummary() ResponseEntity~StatsSummary~
        +getHealthCheck() ResponseEntity~HealthStatus~
    }

    EventController --> QueryService : uses
    AnomalyController --> QueryService : uses
    AnalysisController --> QueryService : uses
    StatsController --> QueryService : uses

    style EventController fill:#E01F27,stroke:#1A1D21,color:#fff
    style AnomalyController fill:#1A1D21,stroke:#E01F27,color:#fff
    style AnalysisController fill:#E01F27,stroke:#1A1D21,color:#fff
    style StatsController fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Service Layer

```mermaid
classDiagram
    class QueryService {
        -RocksDB rocksDB
        -ColumnFamilyHandle defaultColumnFamily
        -ColumnFamilyHandle aiAnalysisColumnFamily
        -ColumnFamilyHandle anomaliesColumnFamily
        -ColumnFamilyHandle embeddingsColumnFamily
        -ObjectMapper objectMapper
        +QueryService(rocksDB, cf_default, cf_ai, cf_anomalies, cf_embeddings)
        +getLatestEvents(limit) List~SecurityEvent~
        +getEventById(eventId) SecurityEvent
        +getLatestAnomalies(limit) List~AnomalyResult~
        +getAnomalyByEventId(eventId) AnomalyResult
        +getAnomaliesByUser(user, limit) List~AnomalyResult~
        +getHighScoreAnomalies(threshold, limit) List~AnomalyResult~
        +getAnomaliesByTimeRange(startTime, endTime, limit) List~AnomalyResult~
        +getAnomalyCount() long
        +getLatestAnalyses(limit) List~ThreatAnalysis~
        +getAnalysisByEventId(eventId) ThreatAnalysis
        +getAnalysesBySeverity(severity, limit) List~ThreatAnalysis~
        +getAnalysisCount() long
        +getEventCount() long
        +getStatsSummary() StatsSummary
        +getEventsByThreatScore(minScore, limit) List~SecurityEvent~
        -parseJson(json, clazz) T
        -handleRocksDBException(exception) void
    }

    class RocksDB {
        <<org.rocksdb>>
        +get(cf, key) byte[]
        +put(cf, key, value) void
        +newIterator(cf) RocksIterator
        +openReadOnly(options, path, cfDescriptors, cfHandles) RocksDB$
        +open(options, path, cfDescriptors, cfHandles) RocksDB$
        +close() void
    }

    class RocksIterator {
        <<org.rocksdb>>
        +seekToFirst() void
        +seekToLast() void
        +seek(key) void
        +next() void
        +prev() void
        +isValid() boolean
        +key() byte[]
        +value() byte[]
        +close() void
    }

    class ColumnFamilyHandle {
        <<org.rocksdb>>
        +getName() byte[]
        +getID() int
    }

    class ObjectMapper {
        <<jackson>>
        +readValue(json, clazz) T
        +writeValueAsString(object) String
        +configure(feature, state) void
    }

    QueryService --> RocksDB : uses
    QueryService --> RocksIterator : creates
    QueryService --> ColumnFamilyHandle : uses
    QueryService --> ObjectMapper : uses

    style QueryService fill:#E01F27,stroke:#1A1D21,color:#fff
    style RocksDB fill:#1A1D21,stroke:#E01F27,color:#fff
    style RocksIterator fill:#E01F27,stroke:#1A1D21,color:#fff
    style ColumnFamilyHandle fill:#1A1D21,stroke:#E01F27,color:#fff
    style ObjectMapper fill:#E01F27,stroke:#1A1D21,color:#fff
```

### Configuration Layer

```mermaid
classDiagram
    class RocksDBConfig {
        -String dbPath
        -boolean readOnly
        -RocksDB db
        -List~ColumnFamilyHandle~ columnFamilyHandles
        +rocksDB() RocksDB @Bean
        +defaultColumnFamily() ColumnFamilyHandle @Bean
        +aiAnalysisColumnFamily() ColumnFamilyHandle @Bean
        +anomaliesColumnFamily() ColumnFamilyHandle @Bean
        +embeddingsColumnFamily() ColumnFamilyHandle @Bean
        +closeDB() void @PreDestroy
        -findColumnFamily(name) ColumnFamilyHandle
    }

    class SwaggerConfig {
        +openAPI() OpenAPI @Bean
        +customizeOpenAPI() OpenAPI
        -createInfo() Info
        -createServers() List~Server~
    }

    class ApplicationConfig {
        +objectMapper() ObjectMapper @Bean
        +restTemplate() RestTemplate @Bean
        +corsConfigurer() WebMvcConfigurer @Bean
    }

    class DBOptions {
        <<org.rocksdb>>
        +setCreateIfMissing(flag) DBOptions
        +setCreateMissingColumnFamilies(flag) DBOptions
        +setMaxOpenFiles(num) DBOptions
    }

    class ColumnFamilyDescriptor {
        <<org.rocksdb>>
        +ColumnFamilyDescriptor(name, options)
        +getName() byte[]
        +getOptions() ColumnFamilyOptions
    }

    class ColumnFamilyOptions {
        <<org.rocksdb>>
        +setCompressionType(type) ColumnFamilyOptions
        +setWriteBufferSize(size) ColumnFamilyOptions
        +optimizeForPointLookup(blockCacheSizeMb) ColumnFamilyOptions
    }

    RocksDBConfig --> DBOptions : uses
    RocksDBConfig --> ColumnFamilyDescriptor : creates
    RocksDBConfig --> ColumnFamilyOptions : uses
    RocksDBConfig --> ColumnFamilyHandle : manages

    style RocksDBConfig fill:#E01F27,stroke:#1A1D21,color:#fff
    style SwaggerConfig fill:#1A1D21,stroke:#E01F27,color:#fff
    style ApplicationConfig fill:#E01F27,stroke:#1A1D21,color:#fff
    style DBOptions fill:#1A1D21,stroke:#E01F27,color:#fff
```

### Model Layer

```mermaid
classDiagram
    class SecurityEvent {
        -String eventId
        -String user
        -Long timestamp
        -String type
        -String sourceIp
        -String geoLocation
        -Double threatScore
        -Map~String,Object~ metadata
        +getters/setters
        +toString() String
        +equals(other) boolean
        +hashCode() int
    }

    class AnomalyResult {
        -String eventId
        -String user
        -Long timestamp
        -Double anomalyScore
        -Double timeAnomaly
        -Double ipAnomaly
        -Double locationAnomaly
        -Double typeAnomaly
        -Double failureAnomaly
        -List~String~ reasons
        +getters/setters
        +isHighRisk() boolean
        +getRiskLevel() String
    }

    class ThreatAnalysis {
        -String eventId
        -String severity
        -Double confidence
        -List~String~ indicators
        -String summary
        -String recommendation
        -Long analyzedAt
        +getters/setters
        +isCritical() boolean
        +getSeverityLevel() int
    }

    class StatsSummary {
        -long totalEvents
        -long highThreatEvents
        -double averageThreatScore
        -long totalAnalyses
        -long totalAnomalies
        -double averageAnomalyScore
        +getters/setters
        +getHighThreatPercentage() double
        +toJson() String
    }

    style SecurityEvent fill:#E01F27,stroke:#1A1D21,color:#fff
    style AnomalyResult fill:#1A1D21,stroke:#E01F27,color:#fff
    style ThreatAnalysis fill:#E01F27,stroke:#1A1D21,color:#fff
    style StatsSummary fill:#1A1D21,stroke:#E01F27,color:#fff
```

## Package Dependencies

```mermaid
graph TB
    subgraph "Stream Processor C++"
        PKG1[streamguard::event]
        PKG2[streamguard::kafka]
        PKG3[streamguard::anomaly]
        PKG4[streamguard::ai]
        PKG5[streamguard::storage]
        PKG6[streamguard::metrics]
    end

    subgraph "Query API Java"
        PKG7[com.streamguard.queryapi.controller]
        PKG8[com.streamguard.queryapi.service]
        PKG9[com.streamguard.queryapi.model]
        PKG10[com.streamguard.queryapi.config]
    end

    subgraph "External Dependencies"
        EXT1[librdkafka]
        EXT2[RocksDB C++]
        EXT3[prometheus-cpp]
        EXT4[cpp-httplib]
        EXT5[nlohmann/json]
        EXT6[RocksDB Java]
        EXT7[Spring Boot]
        EXT8[Jackson]
    end

    PKG2 --> PKG1
    PKG2 --> EXT1
    PKG3 --> PKG1
    PKG4 --> PKG1
    PKG4 --> EXT4
    PKG5 --> PKG1
    PKG5 --> EXT2
    PKG6 --> EXT3

    PKG1 --> EXT5
    PKG5 --> EXT5

    PKG7 --> PKG8
    PKG7 --> PKG9
    PKG8 --> PKG9
    PKG8 --> PKG10
    PKG8 --> EXT6
    PKG10 --> EXT6
    PKG9 --> EXT8
    PKG7 --> EXT7

    style PKG1 fill:#E01F27,stroke:#1A1D21,color:#fff
    style PKG2 fill:#1A1D21,stroke:#E01F27,color:#fff
    style PKG3 fill:#E01F27,stroke:#1A1D21,color:#fff
    style PKG4 fill:#1A1D21,stroke:#E01F27,color:#fff
    style PKG5 fill:#E01F27,stroke:#1A1D21,color:#fff
    style PKG6 fill:#1A1D21,stroke:#E01F27,color:#fff
    style PKG7 fill:#E01F27,stroke:#1A1D21,color:#fff
    style PKG8 fill:#1A1D21,stroke:#E01F27,color:#fff
    style PKG9 fill:#E01F27,stroke:#1A1D21,color:#fff
    style PKG10 fill:#1A1D21,stroke:#E01F27,color:#fff
```

## Design Patterns Used

| Pattern | Component | Purpose |
|---------|-----------|---------|
| **Factory** | EventDeserializer | Create Event objects from JSON |
| **Singleton** | Metrics, EventStore | Single instance per application |
| **Strategy** | AnomalyDetector | Pluggable scoring algorithms |
| **Observer** | KafkaConsumer | Event-driven message processing |
| **Repository** | EventStore, QueryService | Data access abstraction |
| **Facade** | QueryService | Simplified RocksDB interface |
| **Builder** | Event, ThreatAnalysis | Complex object construction |
| **Iterator** | RocksIterator | Sequential data access |
| **Decorator** | HttpClient | Add retry/timeout behavior |
| **Command** | Metrics operations | Encapsulate metric updates |
