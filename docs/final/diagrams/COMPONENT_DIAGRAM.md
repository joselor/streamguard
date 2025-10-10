# StreamGuard Component Architecture

## System Component Diagram

This diagram shows the complete architecture of StreamGuard, highlighting the key components and their interactions.

```mermaid
graph TB
    subgraph "Data Sources"
        K[Kafka Cluster<br/>Security Events]
        style K fill:#E01F27,stroke:#1A1D21,color:#fff
    end

    subgraph "Stream Processing Layer"
        SP[Stream Processor<br/>C++17]
        style SP fill:#E01F27,stroke:#1A1D21,color:#fff

        subgraph "Processing Components"
            KC[Kafka Consumer<br/>rdkafka]
            ED[Event Deserializer<br/>JSON Parser]
            VLD[Event Validator<br/>Schema Checker]
            style KC fill:#1A1D21,stroke:#E01F27,color:#fff
            style ED fill:#1A1D21,stroke:#E01F27,color:#fff
            style VLD fill:#1A1D21,stroke:#E01F27,color:#fff
        end

        subgraph "Analysis Engines"
            AD[Anomaly Detector<br/>Statistical Analysis]
            AIA[AI Analyzer<br/>Claude API]
            EMB[Embeddings Generator<br/>Vector Similarity]
            style AD fill:#E01F27,stroke:#1A1D21,color:#fff
            style AIA fill:#E01F27,stroke:#1A1D21,color:#fff
            style EMB fill:#E01F27,stroke:#1A1D21,color:#fff
        end

        subgraph "Storage Layer"
            ES[Event Store<br/>RocksDB]
            style ES fill:#1A1D21,stroke:#E01F27,color:#fff

            CF1[default CF<br/>Security Events]
            CF2[ai_analysis CF<br/>Threat Analysis]
            CF3[embeddings CF<br/>Vector Data]
            CF4[anomalies CF<br/>Anomaly Results]
            style CF1 fill:#2A2D31,stroke:#E01F27,color:#fff
            style CF2 fill:#2A2D31,stroke:#E01F27,color:#fff
            style CF3 fill:#2A2D31,stroke:#E01F27,color:#fff
            style CF4 fill:#2A2D31,stroke:#E01F27,color:#fff
        end

        subgraph "Metrics & Monitoring"
            PM[Prometheus Exporter<br/>Metrics Server]
            style PM fill:#E01F27,stroke:#1A1D21,color:#fff
        end
    end

    subgraph "Query & API Layer"
        QA[Query API<br/>Spring Boot 3.2]
        style QA fill:#E01F27,stroke:#1A1D21,color:#fff

        subgraph "REST Controllers"
            EC[Event Controller]
            AC[Analysis Controller]
            ANC[Anomaly Controller]
            SC[Stats Controller]
            style EC fill:#1A1D21,stroke:#E01F27,color:#fff
            style AC fill:#1A1D21,stroke:#E01F27,color:#fff
            style ANC fill:#1A1D21,stroke:#E01F27,color:#fff
            style SC fill:#1A1D21,stroke:#E01F27,color:#fff
        end

        subgraph "Service Layer"
            QS[Query Service<br/>RocksDB Java]
            style QS fill:#2A2D31,stroke:#E01F27,color:#fff
        end
    end

    subgraph "Monitoring Stack"
        PROM[Prometheus<br/>Metrics Storage]
        GRAF[Grafana<br/>Dashboards]
        style PROM fill:#E01F27,stroke:#1A1D21,color:#fff
        style GRAF fill:#E01F27,stroke:#1A1D21,color:#fff
    end

    subgraph "External Services"
        CLAUDE[Anthropic Claude<br/>AI Analysis]
        style CLAUDE fill:#1A1D21,stroke:#E01F27,color:#fff
    end

    subgraph "Clients"
        CLI[CLI Tools<br/>curl/httpie]
        WEB[Web Dashboard<br/>Browser]
        API[API Clients<br/>Python/Java]
        style CLI fill:#E01F27,stroke:#1A1D21,color:#fff
        style WEB fill:#E01F27,stroke:#1A1D21,color:#fff
        style API fill:#E01F27,stroke:#1A1D21,color:#fff
    end

    %% Data Flow
    K -->|Security Events| KC
    KC --> ED
    ED --> VLD
    VLD -->|Valid Events| AD
    VLD -->|Valid Events| AIA
    VLD -->|Valid Events| EMB

    AD -->|Anomaly Results| ES
    AIA -->|Threat Analysis| ES
    EMB -->|Vector Data| ES
    VLD -->|Raw Events| ES

    ES --> CF1
    ES --> CF2
    ES --> CF3
    ES --> CF4

    ES -->|Read Access| QS

    QS --> EC
    QS --> AC
    QS --> ANC
    QS --> SC

    EC --> CLI
    AC --> CLI
    ANC --> CLI
    SC --> CLI

    EC --> WEB
    AC --> WEB
    ANC --> WEB
    SC --> WEB

    EC --> API
    AC --> API
    ANC --> API
    SC --> API

    PM -->|Scrape| PROM
    PROM --> GRAF

    AIA -->|API Calls| CLAUDE

    %% Metrics Flow
    AD -.->|Metrics| PM
    AIA -.->|Metrics| PM
    KC -.->|Metrics| PM
    ES -.->|Metrics| PM
```

## Component Descriptions

### Data Sources
- **Kafka Cluster**: Message broker receiving security events from various sources (authentication systems, network monitors, application logs)

### Stream Processing Layer

#### Processing Components
- **Kafka Consumer**: High-performance consumer using librdkafka, handles message acknowledgment and offset management
- **Event Deserializer**: JSON parsing and deserialization using nlohmann/json
- **Event Validator**: Schema validation and data quality checks

#### Analysis Engines
- **Anomaly Detector**: Statistical baseline tracking for behavioral anomaly detection
  - Per-user baseline learning (100 events minimum)
  - 5-dimensional scoring (time, IP, location, event type, failure rate)
  - Weighted composite scoring with configurable threshold

- **AI Analyzer**: Integration with Anthropic Claude for threat intelligence
  - Natural language threat assessment
  - Severity classification (LOW, MEDIUM, HIGH, CRITICAL)
  - Recommended actions and mitigation strategies

- **Embeddings Generator**: Vector representation of security events
  - Semantic similarity search capability
  - Pattern matching across event corpus

#### Storage Layer
- **Event Store**: RocksDB embedded database with column family isolation
  - **default CF**: Raw security events with time-ordered keys
  - **ai_analysis CF**: AI threat analysis results indexed by event_id
  - **embeddings CF**: Vector embeddings for similarity search
  - **anomalies CF**: Anomaly detection results with time-ordered keys

#### Metrics & Monitoring
- **Prometheus Exporter**: HTTP metrics endpoint on configurable port
  - Event processing metrics (total, failed, rate)
  - Anomaly detection metrics (count, scores)
  - AI analysis metrics (total, by severity)
  - System performance metrics (latency, throughput)

### Query & API Layer

#### REST Controllers
- **Event Controller**: CRUD operations for security events
  - Latest events, by ID, by threat score, by time range
- **Analysis Controller**: AI threat analysis queries
  - Latest analyses, by event ID, by severity
- **Anomaly Controller**: Anomaly detection queries
  - Latest anomalies, by user, by score threshold, by time range
- **Stats Controller**: Aggregate statistics and summaries

#### Service Layer
- **Query Service**: RocksDB Java bindings for direct database access
  - No JNI complexity
  - Iterator-based range queries
  - Column family isolation

### Monitoring Stack
- **Prometheus**: Time-series metrics storage and alerting
- **Grafana**: Real-time dashboards and visualization

### External Services
- **Anthropic Claude**: AI-powered threat analysis via REST API

### Clients
- **CLI Tools**: Command-line access via curl, httpie
- **Web Dashboard**: Browser-based UI (future enhancement)
- **API Clients**: Programmatic access from Python, Java, etc.

## Data Flow Patterns

### Event Processing Flow
1. Security events arrive in Kafka topic
2. Stream Processor consumes events via Kafka Consumer
3. Events are deserialized and validated
4. Valid events are processed by all analysis engines in parallel:
   - Anomaly Detector checks for behavioral anomalies
   - AI Analyzer performs threat assessment
   - Embeddings Generator creates vector representations
5. All results stored in RocksDB column families
6. Metrics exported to Prometheus

### Query Flow
1. Client sends HTTP request to Query API
2. Controller routes to appropriate endpoint
3. Query Service reads from RocksDB using Java bindings
4. Results serialized to JSON and returned
5. No JNI overhead, direct database access

### Monitoring Flow
1. Stream Processor exports metrics via HTTP endpoint
2. Prometheus scrapes metrics at configured interval
3. Grafana queries Prometheus for visualization
4. Alerts triggered on threshold violations

## Deployment Architecture

```mermaid
graph LR
    subgraph "Production Environment"
        subgraph "Docker Compose Stack"
            K[Kafka<br/>Port 9092]
            Z[Zookeeper<br/>Port 2181]
            SP[Stream Processor<br/>Metrics: 8080]
            QA[Query API<br/>Port 8081]
            P[Prometheus<br/>Port 9090]
            G[Grafana<br/>Port 3000]

            style K fill:#E01F27,stroke:#1A1D21,color:#fff
            style Z fill:#1A1D21,stroke:#E01F27,color:#fff
            style SP fill:#E01F27,stroke:#1A1D21,color:#fff
            style QA fill:#E01F27,stroke:#1A1D21,color:#fff
            style P fill:#1A1D21,stroke:#E01F27,color:#fff
            style G fill:#1A1D21,stroke:#E01F27,color:#fff
        end

        subgraph "Persistent Storage"
            RDB[(RocksDB<br/>Volume Mount)]
            style RDB fill:#2A2D31,stroke:#E01F27,color:#fff
        end
    end

    Z --> K
    K --> SP
    SP --> RDB
    QA --> RDB
    SP --> P
    P --> G
```

## Technology Stack by Component

| Component | Primary Technology | Key Libraries |
|-----------|-------------------|---------------|
| Stream Processor | C++17 | librdkafka, RocksDB, prometheus-cpp, nlohmann/json, cpp-httplib |
| Query API | Java 17 / Spring Boot 3.2 | RocksDB Java, Spring Web, Swagger/OpenAPI |
| Message Broker | Apache Kafka 3.6 | Zookeeper |
| Storage | RocksDB 8.9 | Column families, Iterators |
| Monitoring | Prometheus 2.48 | Grafana 10.2 |
| AI Analysis | Anthropic Claude | Claude 3.5 Sonnet via REST API |

## Scalability Considerations

### Horizontal Scaling
- **Stream Processor**: Multiple instances with Kafka consumer group
- **Query API**: Load-balanced REST API instances
- **Kafka**: Multi-broker cluster with replication

### Vertical Scaling
- **RocksDB**: SSD-optimized for high IOPS
- **Memory**: Configurable RocksDB block cache
- **CPU**: Multi-threaded processing pipeline

### Performance Characteristics
- **Throughput**: 10,000+ events/second per processor instance
- **Latency**: <50ms p95 for anomaly detection
- **Storage**: 1M events ≈ 500MB compressed
- **Query Performance**: <10ms for indexed lookups, <100ms for range scans

## Security Features

- **Authentication**: API key validation (configurable)
- **Authorization**: Role-based access control (future)
- **Encryption**: TLS for Kafka and API endpoints
- **Data Privacy**: Sensitive field masking in logs
- **Audit Trail**: All queries logged with timestamps
