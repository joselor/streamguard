# StreamGuard Architecture Documentation

**Version:** 2.0
**Last Updated:** October 9, 2025
**Status:** Sprint 1 Complete

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Diagrams](#architecture-diagrams)
3. [Component Design](#component-design)
4. [Data Flow](#data-flow)
5. [Technology Stack](#technology-stack)
6. [Scalability & Performance](#scalability--performance)
7. [Security Considerations](#security-considerations)
8. [Future Architecture](#future-architecture)

---

## System Overview

### Vision

StreamGuard is a high-performance, real-time security event processing platform designed to detect threats in high-volume event streams (50K+ events/second) with sub-100ms latency.

### Core Principles

1. **Performance First**: Every architectural decision prioritizes throughput and latency
2. **Horizontal Scalability**: System scales linearly by adding nodes
3. **Fault Tolerance**: No single point of failure, graceful degradation
4. **Observability**: Comprehensive metrics, logging, and tracing
5. **Simplicity**: Avoid complexity unless absolutely necessary

---

## Architecture Diagrams

### High-Level System Architecture

```mermaid
graph TB
    subgraph "Event Sources"
        ES[Security Events<br/>Simulated]
    end

    subgraph "Event Generation Layer"
        EG[Event Generator<br/>Java/Kafka Producer]
    end

    subgraph "Streaming Layer"
        K1[Kafka Broker 1]
        K2[Kafka Broker 2]
        K3[Kafka Broker 3]
        ZK[Zookeeper<br/>Coordination]
    end

    subgraph "Processing Layer"
        SP1[Stream Processor 1<br/>C++/RocksDB]
        SP2[Stream Processor 2<br/>C++/RocksDB]
        SP3[Stream Processor N<br/>C++/RocksDB]
    end

    subgraph "Query Layer (Future)"
        API[Query API<br/>Spring Boot REST]
    end

    subgraph "Monitoring"
        PROM[Prometheus<br/>Metrics]
        GRAF[Grafana<br/>Dashboards]
    end

    ES --> EG
    EG --> K1 & K2 & K3
    K1 & K2 & K3 -.-> ZK
    K1 & K2 & K3 --> SP1 & SP2 & SP3
    SP1 & SP2 & SP3 --> API
    SP1 & SP2 & SP3 --> PROM
    PROM --> GRAF

    style EG fill:#90EE90
    style SP1 fill:#87CEEB
    style SP2 fill:#87CEEB
    style SP3 fill:#87CEEB
    style K1 fill:#FFD700
    style K2 fill:#FFD700
    style K3 fill:#FFD700
```

### Data Flow Sequence Diagram

```mermaid
sequenceDiagram
    participant EG as Event Generator<br/>(Java)
    participant K as Kafka<br/>(Topic: security-events)
    participant SP as Stream Processor<br/>(C++)
    participant RDB as RocksDB<br/>(Embedded)
    participant API as Query API<br/>(Future)

    Note over EG: Generate Security Event
    EG->>EG: Create Event Object
    EG->>EG: Serialize to JSON
    EG->>K: Produce Event (async)
    K-->>EG: Ack (delivery confirmation)

    Note over K: Event Partitioning<br/>by source_ip

    K->>SP: Poll Events (batch)
    SP->>SP: Deserialize JSON
    SP->>SP: Validate Event

    Note over SP: Process Event
    SP->>SP: Extract threat score
    SP->>SP: Generate composite key<br/>type:timestamp:id

    SP->>RDB: Put Event (write)
    RDB-->>SP: Write Ack

    SP->>K: Commit Offset

    Note over API,RDB: Query Path (Future)
    API->>RDB: Range Query<br/>(type + time range)
    RDB-->>API: Event Results
```

### Component Interaction Diagram

```mermaid
graph LR
    subgraph "Event Generator"
        EF[EventFactory]
        EP[KafkaProducer]
        CLI1[CLI Parser]
    end

    subgraph "Stream Processor"
        KC[KafkaConsumer<br/>librdkafka++]
        ED[Event<br/>Deserializer]
        ES[EventStore<br/>RocksDB]
        SH[Signal Handler]
        CLI2[CLI Parser]
    end

    subgraph "Storage"
        RDB[(RocksDB<br/>LSM-Tree)]
        SST[SST Files]
        WAL[Write-Ahead Log]
        CACHE[LRU Cache<br/>256MB]
    end

    CLI1 --> EF
    EF --> EP
    EP -->|JSON Events| KC
    KC --> ED
    ED --> ES
    ES --> RDB
    RDB --> SST
    RDB --> WAL
    RDB --> CACHE
    SH -.->|Shutdown Signal| KC
    CLI2 --> KC

    style EF fill:#90EE90
    style KC fill:#87CEEB
    style ES fill:#87CEEB
    style RDB fill:#FFB6C1
```

### RocksDB Key Design

```mermaid
graph TD
    subgraph "Composite Key Structure"
        KEY["event_type:timestamp:event_id"]
        ET[Event Type<br/>auth_attempt]
        TS[Timestamp<br/>001760043114588<br/>Zero-padded 15 digits]
        ID[Event ID<br/>evt_abc123...]
    end

    KEY --> ET
    KEY --> TS
    KEY --> ID

    subgraph "Query Patterns"
        Q1[Range Query:<br/>All auth events<br/>last hour]
        Q2[Point Query:<br/>Get by event_id]
        Q3[Latest Query:<br/>Last N events<br/>of type X]
    end

    ET --> Q1
    TS --> Q1
    ID --> Q2
    ET --> Q3
    TS --> Q3

    style KEY fill:#FFD700
    style Q1 fill:#90EE90
    style Q2 fill:#90EE90
    style Q3 fill:#90EE90
```

### Deployment Architecture (Current - Docker Compose)

```mermaid
graph TB
    subgraph "Docker Host (Local Development)"
        subgraph "Kafka Infrastructure"
            ZK[Zookeeper<br/>:2181]
            K1[Kafka-1<br/>:9092]
            K2[Kafka-2<br/>:9093]
            K3[Kafka-3<br/>:9094]
        end

        subgraph "Application Containers (Future)"
            EG[event-generator<br/>Java JAR]
            SP[stream-processor<br/>C++ Binary]
        end

        subgraph "Monitoring Stack"
            PROM[Prometheus<br/>:9090]
            GRAF[Grafana<br/>:3000]
            KUI[Kafka UI<br/>:8090]
        end

        subgraph "Volumes"
            V1[kafka-data]
            V2[zookeeper-data]
            V3[prometheus-data]
            V4[grafana-data]
        end
    end

    ZK -.-> K1 & K2 & K3
    K1 & K2 & K3 --> V1
    ZK --> V2
    EG --> K1 & K2 & K3
    SP --> K1 & K2 & K3
    PROM --> V3
    GRAF --> V4
    PROM --> GRAF

    style EG fill:#90EE90
    style SP fill:#87CEEB
    style K1 fill:#FFD700
    style K2 fill:#FFD700
    style K3 fill:#FFD700
```

---

## Component Design

### 1. Event Generator (Java)

**Purpose**: Generate realistic security events for testing and simulation

**Key Classes**:
- `EventGenerator.java` - Main application class
- `EventFactory.java` - Event creation with realistic data
- `Event.java` - Event data model (5 types)

**Design Patterns**:
- **Builder Pattern**: Event construction
- **Factory Pattern**: Event type generation
- **Strategy Pattern**: Rate limiting algorithms

**Configuration**:
```java
// Command-line arguments
--broker localhost:9092    // Kafka bootstrap servers
--topic security-events     // Target topic
--rate 100                  // Events per second
--duration 60               // Run duration (0 = unlimited)
```

**Kafka Producer Configuration**:
- **Acks**: -1 (all replicas)
- **Compression**: gzip
- **Batch Size**: 32KB
- **Linger**: 10ms
- **Retries**: 3
- **Idempotence**: true

### 2. Stream Processor (C++)

**Purpose**: High-performance event consumption, processing, and storage

**Key Classes**:
- `KafkaConsumer` - librdkafka++ consumer wrapper
- `EventStore` - RocksDB storage abstraction
- `Event` - Event data structure with JSON serialization

**Design Patterns**:
- **RAII**: Resource management (RocksDB, Kafka)
- **Callback Pattern**: Event processing
- **Composite Key Pattern**: Time-series storage

**Threading Model**:
```
Main Thread
├── Signal Handling (atomic flag)
├── Kafka Consumer Loop
│   ├── Poll Events (batch)
│   ├── Deserialize JSON
│   ├── Callback Invocation
│   │   ├── Event Validation
│   │   ├── RocksDB Write
│   │   └── Metrics Update
│   └── Commit Offsets
```

**RocksDB Configuration**:
```cpp
// Optimized for time-series workload
options.compression = kSnappyCompression;
options.write_buffer_size = 64MB;
options.max_write_buffer_number = 3;
options.target_file_size_base = 64MB;

// Bloom filters for faster lookups
table_options.filter_policy = NewBloomFilterPolicy(10);
table_options.block_cache = NewLRUCache(256MB);
```

### 3. Kafka Infrastructure

**Topic Configuration**:
```yaml
Topic: security-events
Partitions: 4
Replication Factor: 1 (dev), 3 (prod)
Retention: 7 days
Cleanup Policy: delete
```

**Partitioning Strategy**:
- **Key**: `source_ip` (ensures events from same source go to same partition)
- **Benefits**: Ordering guarantees, session affinity
- **Trade-off**: Potential hot partitions for high-volume sources

**Consumer Group**:
- Group ID: `streamguard-processor`
- Offset Strategy: Latest
- Auto-commit: true (5 second interval)
- Session Timeout: 30 seconds

### 4. Storage Layer (RocksDB)

**LSM-Tree Architecture**:
```
Memory:
├── MemTable (active writes, 64MB)
└── Immutable MemTable (pending flush)

Disk:
├── Level 0 (SST files from MemTable flush)
├── Level 1 (64MB files)
├── Level 2 (640MB)
├── Level 3 (6.4GB)
└── ...
```

**Compaction Strategy**:
- **Type**: Universal compaction
- **Trigger**: Size-tiered
- **Goal**: Minimize read amplification

**Key Features**:
- **Embedded**: No network latency
- **Crash Recovery**: Write-Ahead Log (WAL)
- **Snapshots**: Point-in-time backups
- **Bloom Filters**: Reduce disk reads

---

## Data Flow

### Event Generation to Storage (Complete Path)

1. **Event Creation**:
   ```java
   Event event = EventFactory.createAuthAttempt();
   // Generates realistic data: IPs, users, threat scores
   ```

2. **Serialization**:
   ```java
   String json = objectMapper.writeValueAsString(event);
   // Converts to JSON for Kafka transmission
   ```

3. **Kafka Production**:
   ```java
   producer.send(new ProducerRecord<>("security-events", sourceIp, json));
   // Async send with callback for ack
   ```

4. **Kafka Storage & Partitioning**:
   - Event written to partition based on `source_ip` hash
   - Replicated to followers (in production)
   - Persisted to disk

5. **Consumer Poll**:
   ```cpp
   RdKafka::Message *msg = consumer->consume(1000);
   // Batch poll with 1 second timeout
   ```

6. **Deserialization**:
   ```cpp
   Event event = Event::fromJson(msg->payload());
   // nlohmann/json parsing
   ```

7. **Validation**:
   ```cpp
   if (!event.isValid()) {
       logError("Invalid event", event.event_id);
       return;
   }
   ```

8. **Storage**:
   ```cpp
   std::string key = makeKey(event);  // type:timestamp:id
   rocksdb::Status s = db_->Put(WriteOptions(), key, event.toJson());
   ```

9. **Offset Commit**:
   ```cpp
   consumer->commitAsync();
   // Acknowledge successful processing
   ```

### Query Path (Future - Sprint 4)

```
Client Request
└── Query API (Spring Boot)
    ├── Parse Query Parameters
    │   ├── Event Type
    │   ├── Time Range
    │   └── Limit
    ├── Generate RocksDB Key Range
    │   ├── Start Key: type:start_time:
    │   └── End Key: type:end_time:~
    ├── RocksDB Range Scan
    │   ├── Seek to Start Key
    │   ├── Iterate Until End Key
    │   └── Collect Results
    ├── Deserialize Events
    └── Return JSON Response
```

---

## Technology Stack

### Languages & Runtimes

| Component | Language | Version | Justification |
|-----------|----------|---------|---------------|
| Event Generator | Java | 17 LTS | Kafka client maturity, Spring Boot ecosystem |
| Stream Processor | C++ | 17 | Performance, low latency, RocksDB integration |
| Query API | Java | 17 LTS | Spring Boot rapid development |

### Core Libraries

**Java**:
- **Kafka**: `org.apache.kafka:kafka-clients:3.6.0`
- **JSON**: `com.fasterxml.jackson.core:jackson-databind:2.15.2`
- **Logging**: `org.slf4j:slf4j-api:2.0.9`
- **Testing**: `org.junit.jupiter:junit-jupiter:5.10.0`

**C++**:
- **Kafka**: `librdkafka++ 2.3.0` (Homebrew)
- **JSON**: `nlohmann/json 3.11.3` (Homebrew)
- **Storage**: `rocksdb 8.9.1` (Homebrew)
- **Testing**: `googletest 1.14.0` (Homebrew)

### Infrastructure

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Message Queue | Apache Kafka | 3.6+ | Event streaming backbone |
| Coordination | Zookeeper | 3.8+ | Kafka cluster coordination |
| Metrics | Prometheus | Latest | Time-series metrics collection |
| Visualization | Grafana | Latest | Dashboards and alerting |
| Containerization | Docker | 24+ | Development environment |
| Orchestration | Docker Compose | 2.23+ | Local multi-container apps |

### Build Tools

- **Java**: Maven 3.9+ (`pom.xml`)
- **C++**: CMake 3.20+ (`CMakeLists.txt`)
- **Containerization**: Docker & Docker Compose
- **Version Control**: Git + GitHub

---

## Scalability & Performance

### Horizontal Scaling Strategy

```mermaid
graph LR
    subgraph "Kafka Partitioning"
        P0[Partition 0<br/>source_ip % 4 == 0]
        P1[Partition 1<br/>source_ip % 4 == 1]
        P2[Partition 2<br/>source_ip % 4 == 2]
        P3[Partition 3<br/>source_ip % 4 == 3]
    end

    subgraph "Stream Processors"
        SP1[Processor 1<br/>→ P0]
        SP2[Processor 2<br/>→ P1]
        SP3[Processor 3<br/>→ P2]
        SP4[Processor 4<br/>→ P3]
    end

    subgraph "Storage"
        DB1[(RocksDB 1)]
        DB2[(RocksDB 2)]
        DB3[(RocksDB 3)]
        DB4[(RocksDB 4)]
    end

    P0 --> SP1 --> DB1
    P1 --> SP2 --> DB2
    P2 --> SP3 --> DB3
    P3 --> SP4 --> DB4

    style SP1 fill:#87CEEB
    style SP2 fill:#87CEEB
    style SP3 fill:#87CEEB
    style SP4 fill:#87CEEB
```

### Performance Targets

| Metric | Current | Target | Stretch Goal |
|--------|---------|--------|--------------|
| Throughput (per node) | ~100-200 e/s | 50,000 e/s | 100,000 e/s |
| Latency (p95) | ~50-100ms | <100ms | <20ms |
| Memory (per node) | ~200MB | <4GB | <2GB |
| CPU (per node) | ~10% | <80% | <50% |
| Storage Growth | ~1KB/event | Compacted | <500B/event |

### Performance Optimization Roadmap

**Sprint 2 - Multi-threading**:
- Parallel event processing
- Thread pool for RocksDB writes
- Lock-free data structures

**Sprint 3 - Protocol Optimization**:
- Binary serialization (Protobuf/Avro)
- Batch processing
- Zero-copy optimizations

**Sprint 4 - Caching Layer**:
- In-memory event cache
- Hot partition optimization
- Read-through cache for queries

**Sprint 5 - Advanced Optimizations**:
- SIMD vectorization
- Custom memory allocators
- Kernel bypass networking (DPDK)

### Capacity Planning

**Single Node Capacity** (at target performance):
```
Events per second: 50,000
Average event size: 500 bytes
Throughput: 25 MB/s
Daily storage: 2.16 TB (uncompressed)
                ~500 GB (compressed with Snappy)
```

**Cluster Scaling**:
- **10 nodes**: 500,000 events/sec, 5 TB/day
- **50 nodes**: 2.5M events/sec, 25 TB/day
- **100 nodes**: 5M events/sec, 50 TB/day

---

## Security Considerations

### Current Security Measures

1. **Network Isolation**:
   - Docker network isolation
   - Internal-only communication
   - No external exposure (dev environment)

2. **Data Validation**:
   - Event schema validation
   - Threat score range checks
   - IP address format validation

3. **Error Handling**:
   - No sensitive data in logs
   - Structured error messages
   - Graceful degradation

### Future Security Enhancements (Sprint 5)

1. **Authentication & Authorization**:
   - [ ] mTLS for inter-service communication
   - [ ] API key authentication for Query API
   - [ ] RBAC for data access

2. **Encryption**:
   - [ ] TLS for Kafka (in-transit)
   - [ ] RocksDB encryption at rest
   - [ ] Secrets management (Vault)

3. **Audit & Compliance**:
   - [ ] Access logging
   - [ ] Data retention policies
   - [ ] GDPR compliance mechanisms

4. **Threat Protection**:
   - [ ] Rate limiting
   - [ ] DDoS protection
   - [ ] Input sanitization

---

## Future Architecture

### Planned Enhancements

**Sprint 2 - Multi-threading**:
```mermaid
graph LR
    KC[Kafka Consumer] --> TQ[Task Queue]
    TQ --> T1[Worker Thread 1]
    TQ --> T2[Worker Thread 2]
    TQ --> T3[Worker Thread N]
    T1 & T2 & T3 --> RDB[(RocksDB)]
```

**Sprint 3 - ML Integration**:
```mermaid
graph LR
    SP[Stream Processor] --> ML[ML Model<br/>ONNX Runtime]
    ML --> TS[Threat Scorer]
    TS --> RDB[(RocksDB)]
    TS --> ALT[Alert System]
```

**Sprint 4 - Query API**:
```mermaid
graph TB
    CLIENT[Client] --> LB[Load Balancer]
    LB --> API1[Query API 1]
    LB --> API2[Query API 2]
    API1 & API2 --> CACHE[Redis Cache]
    API1 & API2 --> SP1[Stream Processor 1]
    API1 & API2 --> SP2[Stream Processor 2]
    SP1 & SP2 --> RDB1[(RocksDB)]
```

**Sprint 5 - Kubernetes Deployment**:
```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Kafka StatefulSet"
            K1[kafka-0]
            K2[kafka-1]
            K3[kafka-2]
        end

        subgraph "Processor Deployment"
            SP1[processor-pod-1]
            SP2[processor-pod-2]
            SPn[processor-pod-n]
        end

        subgraph "API Deployment"
            API1[api-pod-1]
            API2[api-pod-2]
        end

        subgraph "Monitoring"
            PROM[Prometheus]
            GRAF[Grafana]
        end
    end

    K1 & K2 & K3 --> SP1 & SP2 & SPn
    SP1 & SP2 & SPn --> API1 & API2
    SP1 & SP2 & SPn --> PROM
    PROM --> GRAF
```

---

## Architecture Decision Records (ADRs)

### ADR-001: Polyglot Architecture (Java + C++)

**Context**: Need high-performance stream processing + rapid API development

**Decision**: Java for generator/API, C++ for stream processor

**Consequences**:
- ✅ Optimal performance where needed
- ✅ Faster development for non-critical path
- ⚠️ Multiple build systems
- ⚠️ Cross-language complexity

### ADR-002: Embedded RocksDB vs External Database

**Context**: Need low-latency persistent storage for events

**Decision**: Embedded RocksDB in stream processor

**Consequences**:
- ✅ Zero network latency
- ✅ Simpler deployment
- ✅ Scales with processor
- ⚠️ Per-node backup complexity
- ⚠️ No shared storage

### ADR-003: Composite Key Design for Time-Series

**Context**: Need efficient time-range queries and point lookups

**Decision**: Use `event_type:timestamp:event_id` composite key

**Consequences**:
- ✅ Efficient range scans
- ✅ Lexicographic ordering
- ✅ No secondary indexes needed
- ⚠️ Key size overhead (~50 bytes)

### ADR-004: Kafka Partitioning by source_ip

**Context**: Need to scale processing horizontally

**Decision**: Partition events by `source_ip` hash

**Consequences**:
- ✅ Ordering per source
- ✅ Session affinity
- ✅ Load distribution
- ⚠️ Potential hot partitions
- ⚠️ Rebalancing on scale events

---

**Last Updated**: October 9, 2025 - Sprint 1 Complete
**Author**: Jose Ortuno
