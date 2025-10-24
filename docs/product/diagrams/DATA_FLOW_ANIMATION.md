# StreamGuard Data Flow Animation

## Real-Time Event Processing Flow

This document provides an animated, step-by-step visualization of how data flows through StreamGuard, from initial event ingestion to final query results.

## Phase 1: Event Ingestion

```mermaid
sequenceDiagram
    autonumber
    participant Source as Security Event Source<br/>(Login, Network, App)
    participant Kafka as Kafka Topic<br/>security-events
    participant Consumer as Kafka Consumer<br/>librdkafka
    participant Deserializer as Event Deserializer<br/>JSON Parser

    rect rgb(255, 240, 240)
        Note over Source,Deserializer: PHASE 1: Event Ingestion (1-5ms)
        Source->>Kafka: Publish security event<br/>(user, timestamp, type, ip, ...)
        Note right of Kafka: Event stored in partition<br/>Offset=12345
        Consumer->>Kafka: Poll for new messages<br/>(batch size: 100)
        Kafka-->>Consumer: Return event batch
        Note right of Consumer: Acknowledge offset<br/>Group=stream-processor
        Consumer->>Deserializer: Raw JSON bytes
        Deserializer->>Deserializer: Parse & validate schema
        Note right of Deserializer: Event object created<br/>Ready for processing
    end
```

**Key Metrics:**
- Latency: 1-5ms
- Throughput: 10,000+ events/sec
- Batch Size: 100 events

## Phase 2: Parallel Analysis Pipeline

```mermaid
flowchart TB
    subgraph Input["Event Validated ✓"]
        E[Security Event<br/>user=alice<br/>ip=10.0.1.50<br/>type=LOGIN_FAILED]
        style E fill:#E01F27,stroke:#1A1D21,color:#fff
    end

    subgraph Pipeline["Parallel Analysis (10-50ms)"]
        direction LR

        subgraph Anomaly["Anomaly Detection"]
            direction TB
            A1[Load user baseline<br/>alice=127 events]
            A2[Calculate probabilities<br/>Time=0.15<br/>IP=0.02<br/>Location=0.08]
            A3[Compute composite score<br/>0.73 - MEDIUM]
            A4[Store anomaly result<br/>Column Family=anomalies]
            style A1 fill:#1A1D21,stroke:#E01F27,color:#fff
            style A2 fill:#1A1D21,stroke:#E01F27,color:#fff
            style A3 fill:#E01F27,stroke:#1A1D21,color:#fff
            style A4 fill:#1A1D21,stroke:#E01F27,color:#fff
            A1 --> A2 --> A3 --> A4
        end

        subgraph AI["AI Threat Analysis (SELECTIVE)"]
            direction TB
            B0{Check Trigger<br/>threat >= 0.7<br/>OR anomaly?}
            B1[Prepare event context<br/>Recent failures=3<br/>Known IPs=5]
            B2[Call OpenAI API<br/>Model=GPT-4o-mini]
            B3[Parse AI response<br/>Severity=MEDIUM<br/>Confidence=0.85]
            B4[Store analysis<br/>Column Family=ai_analysis]
            SKIP[Skip AI Analysis<br/>Cost savings]
            style B0 fill:#E01F27,stroke:#1A1D21,color:#fff
            style B1 fill:#1A1D21,stroke:#E01F27,color:#fff
            style B2 fill:#E01F27,stroke:#1A1D21,color:#fff
            style B3 fill:#1A1D21,stroke:#E01F27,color:#fff
            style B4 fill:#1A1D21,stroke:#E01F27,color:#fff
            style SKIP fill:#2A2D31,stroke:#E01F27,color:#fff
            B0 -->|Yes + AI enabled| B1
            B0 -->|No or AI disabled| SKIP
            B1 --> B2 --> B3 --> B4
        end
    end

    subgraph Storage["RocksDB Storage"]
        direction TB
        S1[Store raw event<br/>CF=default<br/>Key=timestamp_event_id]
        style S1 fill:#2A2D31,stroke:#E01F27,color:#fff
    end

    subgraph Metrics["Metrics Export"]
        M1[Increment counters<br/>events_processed++<br/>anomalies_detected++]
        M2[Record histograms<br/>anomaly_score=0.73<br/>processing_time=45ms]
        style M1 fill:#E01F27,stroke:#1A1D21,color:#fff
        style M2 fill:#E01F27,stroke:#1A1D21,color:#fff
        M1 --> M2
    end

    E --> Anomaly
    E --> AI
    E --> Storage

    Anomaly --> Metrics
    AI --> Metrics
    Storage --> Metrics
```

**Processing Characteristics:**
- All analysis engines run in parallel
- **Selective AI**: Only 3-5% of events analyzed (high-threat or anomalous)
- Independent failure handling (if AI API fails, anomaly detection continues)
- Non-blocking storage operations
- Real-time metrics updates

## Phase 3: Data Storage Architecture

```mermaid
graph TB
    subgraph RocksDB["RocksDB Database File"]
        direction TB

        subgraph CF1[Column Family_default]
            E1["Key = 1696723200000_evt_001<br/>Value = raw event JSON"]
            E2["Key = 1696723201000_evt_002<br/>Value = raw event JSON"]
            E3["Key = 1696723202000_evt_003<br/>Value = raw event JSON"]
            style E1 fill:#2A2D31,stroke:#E01F27,color:#fff
            style E2 fill:#2A2D31,stroke:#E01F27,color:#fff
            style E3 fill:#2A2D31,stroke:#E01F27,color:#fff
        end

        subgraph CF2[Column Family_ai_analysis]
            A1["Key = evt_001<br/>Value = severity MEDIUM"]
            A2["Key = evt_002<br/>Value = severity HIGH"]
            A3["Key = evt_003<br/>Value = severity LOW"]
            style A1 fill:#2A2D31,stroke:#E01F27,color:#fff
            style A2 fill:#2A2D31,stroke:#E01F27,color:#fff
            style A3 fill:#2A2D31,stroke:#E01F27,color:#fff
        end

        subgraph CF3[Column Family_anomalies]
            AN1["Key = 1696723200000_evt_001<br/>Value = anomaly_score 0.73"]
            AN2["Key = 1696723201000_evt_002<br/>Value = anomaly_score 0.91"]
            AN3["Key = 1696723202000_evt_003<br/>Value = anomaly_score 0.15"]
            style AN1 fill:#2A2D31,stroke:#E01F27,color:#fff
            style AN2 fill:#2A2D31,stroke:#E01F27,color:#fff
            style AN3 fill:#2A2D31,stroke:#E01F27,color:#fff
        end
    end

    Note1[Time-ordered keys enable<br/>efficient range queries]
    Note2[Column family isolation<br/>prevents data mixing]
    Note3[Embedded database<br/>no network overhead]

    style Note1 fill:#E01F27,stroke:#1A1D21,color:#fff
    style Note2 fill:#E01F27,stroke:#1A1D21,color:#fff
    style Note3 fill:#E01F27,stroke:#1A1D21,color:#fff
```

**Storage Benefits:**
- Zero network latency (embedded database)
- Column family isolation (separate LSM trees)
- Time-ordered keys for efficient range scans
- Compression reduces disk usage by 60-70%
- SST file compaction in background

## Phase 4: Query Execution

```mermaid
sequenceDiagram
    autonumber
    participant Client as API Client<br/>(curl, Web UI)
    participant Controller as REST Controller<br/>Spring Boot
    participant Service as Query Service<br/>RocksDB Java
    participant DB as RocksDB Database
    participant Cache as Block Cache<br/>(Memory)

    rect rgb(255, 240, 240)
        Note over Client,Cache: PHASE 4: Query Execution (5-100ms)
        Client->>Controller: GET /api/anomalies/high-score?threshold=0.7
        Note right of Client: HTTP Request<br/>Headers = Accept application/json

        Controller->>Controller: Validate parameters<br/>threshold in [0.0, 1.0]
        Controller->>Service: getHighScoreAnomalies(0.7, 100)

        Service->>DB: Create iterator on anomalies CF
        Note right of Service: Iterator positioned<br/>at last entry

        loop Scan backwards until limit or threshold
            DB->>Cache: Check block cache for SST block
            alt Cache HIT
                Cache-->>DB: Return cached block
                Note right of Cache: Zero disk I/O<br/>Sub-millisecond access
            else Cache MISS
                DB->>DB: Read SST file from disk
                DB->>Cache: Store block in cache
                Note right of Cache: Future requests cached
            end

            DB-->>Service: Return anomaly record
            Service->>Service: Parse JSON<br/>Check anomaly_score >= 0.7

            alt Score meets threshold
                Service->>Service: Add to results list
            else Score below threshold
                Service->>Service: Skip record
            end
        end

        Service-->>Controller: List<AnomalyResult> (filtered)
        Controller->>Controller: Serialize to JSON<br/>Apply pagination headers
        Controller-->>Client: HTTP 200 OK<br/>[(anomaly_score=0.91, ...), ...]
    end
```

**Query Performance:**
- Indexed lookups: <10ms
- Range scans: <100ms for 10K records
- Block cache hit ratio: >90% in production
- Zero deserialization overhead (lazy parsing)

## Phase 5: Monitoring & Observability

```mermaid
flowchart LR
    subgraph StreamProcessor["Stream Processor"]
        direction TB
        P1[Process Events]
        P2[Update Metrics<br/>in Memory]
        P3[Expose HTTP Endpoint<br/>localhost:8080/metrics]
        style P1 fill:#E01F27,stroke:#1A1D21,color:#fff
        style P2 fill:#1A1D21,stroke:#E01F27,color:#fff
        style P3 fill:#E01F27,stroke:#1A1D21,color:#fff
        P1 --> P2 --> P3
    end

    subgraph Prometheus["Prometheus Server"]
        direction TB
        PR1[Scrape every 15s]
        PR2[Store time-series data]
        PR3[Evaluate alert rules]
        style PR1 fill:#1A1D21,stroke:#E01F27,color:#fff
        style PR2 fill:#E01F27,stroke:#1A1D21,color:#fff
        style PR3 fill:#1A1D21,stroke:#E01F27,color:#fff
        PR1 --> PR2 --> PR3
    end

    subgraph Grafana["Grafana Dashboard"]
        direction TB
        G1[Query Prometheus]
        G2[Render visualizations<br/>Graphs, Gauges, Tables]
        G3[Display alerts<br/>Anomaly spike detected!]
        style G1 fill:#E01F27,stroke:#1A1D21,color:#fff
        style G2 fill:#1A1D21,stroke:#E01F27,color:#fff
        style G3 fill:#E01F27,stroke:#1A1D21,color:#fff
        G1 --> G2 --> G3
    end

    P3 -->|HTTP GET| PR1
    PR2 -->|PromQL| G1

    Alert[Alert Manager<br/>Send notifications<br/>Email, Slack, PagerDuty]
    style Alert fill:#E01F27,stroke:#1A1D21,color:#fff
    PR3 -.->|Trigger| Alert
```

**Available Metrics:**
- `streamguard_events_processed_total` - Counter by user
- `streamguard_events_failed_total` - Counter by error type
- `streamguard_anomalies_detected_total` - Counter by score_range
- `streamguard_anomaly_score` - Histogram (0.0-1.0)
- `streamguard_ai_analyses_total` - Counter by severity
- `streamguard_processing_latency_seconds` - Histogram

## Complete End-to-End Flow

```mermaid
timeline
    title Security Event Journey (0-100ms)
    section Ingestion
        0ms : Event arrives in Kafka
        2ms : Consumer polls and receives event
        3ms : JSON deserialized and validated
    section Analysis
        5ms : Anomaly detection starts
        8ms : Check AI trigger (threat >= 0.7 OR anomaly)
        10ms : Anomaly score computed (0.73)
        12ms : AI analysis API call sent (if triggered)
        45ms : AI analysis received (MEDIUM severity)
    section Storage
        48ms : All results written to RocksDB
        50ms : Write confirmed, offsets committed
        52ms : Metrics updated in Prometheus format
    section Query
        1000ms : Client queries /api/anomalies/high-score
        1005ms : Iterator created on anomalies CF
        1015ms : Results filtered and returned
        1020ms : HTTP response sent to client
```

## Failure Scenarios & Recovery

### Scenario 1: AI API Timeout

```mermaid
flowchart TD
    A[Event Processing] --> B{AI API Available?}
    B -->|Yes| C[Normal AI Analysis]
    B -->|No/Timeout| D[Log Error]
    D --> E[Continue with Anomaly Detection]
    E --> F[Store Event without AI Analysis]
    F --> G[Increment Error Metric]
    G --> H[Processing Continues]

    C --> I[Store Complete Event]

    style A fill:#E01F27,stroke:#1A1D21,color:#fff
    style D fill:#1A1D21,stroke:#E01F27,color:#fff
    style E fill:#E01F27,stroke:#1A1D21,color:#fff
    style F fill:#1A1D21,stroke:#E01F27,color:#fff
    style I fill:#E01F27,stroke:#1A1D21,color:#fff
```

### Scenario 2: RocksDB Write Failure

```mermaid
flowchart TD
    A[Attempt Write] --> B{Write Success?}
    B -->|Yes| C[Commit Kafka Offset]
    B -->|No| D[Log Error + Stack Trace]
    D --> E[Increment Failure Metric]
    E --> F{Retry Count < 3?}
    F -->|Yes| G[Exponential Backoff]
    F -->|No| H[Skip Event]
    G --> A
    H --> I[Alert: Data Loss Possible]

    style A fill:#E01F27,stroke:#1A1D21,color:#fff
    style D fill:#1A1D21,stroke:#E01F27,color:#fff
    style I fill:#E01F27,stroke:#1A1D21,color:#fff
```

## Performance Optimization Points

### 1. Kafka Consumer Optimization
- **Batch Processing**: Fetch 100 events per poll
- **Parallel Processing**: Thread pool for analysis
- **Offset Management**: Commit after successful writes

### 2. RocksDB Configuration
- **Block Cache**: 512MB for hot data
- **Bloom Filters**: Reduce unnecessary disk reads
- **Compaction**: Background LSM tree optimization
- **Compression**: LZ4 for balance of speed/size

### 3. AI API Optimization
- **Selective Triggering**: Only analyze 3-5% of events (95%+ cost reduction)
- **Opt-in at Startup**: User must explicitly enable AI analysis
- **Connection Pooling**: Reuse HTTP connections
- **Timeout Configuration**: 5s timeout prevents blocking
- **Retry Strategy**: Exponential backoff with jitter
- **Circuit Breaker**: Stop calling if failure rate > 50%

### 4. Query Performance
- **Column Family Isolation**: Separate LSM trees
- **Time-Ordered Keys**: Efficient range scans
- **Result Limiting**: Prevent memory exhaustion
- **Lazy Parsing**: Parse JSON only when needed

## Data Volume Projections

| Time Period | Events Processed | Storage Used | Anomalies Detected | AI Analyses (Selective) |
|-------------|------------------|--------------|-------------------|------------------------|
| 1 hour | 36M | ~18GB | ~360K (1%) | ~1.4M (4%) |
| 1 day | 864M | ~432GB | ~8.6M | ~34M (4%) |
| 1 week | 6.05B | ~3TB | ~60M | ~242M (4%) |
| 1 month | 25.9B | ~13TB | ~259M | ~1.04B (4%) |

**Assumptions:**
- 10,000 events/second sustained
- 500 bytes per event (compressed)
- 1% anomaly detection rate
- **4% AI analysis rate** (selective: threat >= 0.7 OR anomaly, opt-in)

## Conclusion

StreamGuard's architecture demonstrates several key design principles:

1. **Parallel Processing**: Independent analysis engines maximize throughput
2. **Fault Tolerance**: Component failures don't cascade
3. **Observability**: Every stage emits metrics
4. **Storage Efficiency**: Column families and compression optimize disk usage
5. **Query Performance**: Time-ordered keys enable fast range scans
6. **Scalability**: Horizontal scaling via Kafka consumer groups

The complete journey from event ingestion to query results takes 50-100ms at p95, meeting real-time security monitoring requirements.
