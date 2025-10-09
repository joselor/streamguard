# StreamGuard - Complete Project Context & Handoff Document

**Document Version:** 1.0  
**Last Updated:** October 9, 2025  
**Project Status:** Sprint 1 - Day 2  
**Completed:** US-101, US-102 | **In Progress:** US-103

---

## Table of Contents

1. [Project Vision & Overview](#project-vision--overview)
2. [Architecture & Design Decisions](#architecture--design-decisions)
3. [Technology Stack & Rationale](#technology-stack--rationale)
4. [Development Environment](#development-environment)
5. [What We've Built (Completed Work)](#what-weve-built)
6. [Code Structure & Patterns](#code-structure--patterns)
7. [Design Principles & Guidelines](#design-principles--guidelines)
8. [Current Sprint (Sprint 1)](#current-sprint)
9. [Roadmap & Future Sprints](#roadmap--future-sprints)
10. [Key Decisions & Rationale](#key-decisions--rationale)
11. [Testing Strategy](#testing-strategy)
12. [Performance Targets](#performance-targets)
13. [Quick Reference](#quick-reference)

---

## Project Vision & Overview

### What is StreamGuard?

StreamGuard is a **real-time AI-powered security event processing platform** designed to detect threats in high-volume security event streams (50K+ events/second). It combines streaming data processing, machine learning, and time-series analysis to identify security threats with sub-second latency.

### Core Problem Statement

Traditional security event processing systems struggle with:
- **Volume**: Can't handle 50K+ events/second in real-time
- **Latency**: Detection takes seconds or minutes, not milliseconds
- **Accuracy**: Too many false positives, alert fatigue
- **Scalability**: Can't scale horizontally efficiently
- **Cost**: Expensive cloud solutions for high-volume processing

### Solution Approach

StreamGuard solves these problems through:
1. **High-performance C++ stream processor** for sub-100ms latency
2. **Real-time ML inference** for threat detection
3. **Time-series analysis** using RocksDB for pattern detection
4. **Horizontal scalability** via Kafka partitioning
5. **RESTful Query API** for real-time and historical queries

### Success Metrics

- **Throughput**: 50,000+ events/second per node
- **Latency**: <100ms end-to-end processing time (p95)
- **Detection Accuracy**: >95% true positive rate, <5% false positive rate
- **Availability**: 99.9% uptime
- **Scalability**: Linear scaling up to 10 nodes

---

## Architecture & Design Decisions

### High-Level Architecture

```
┌─────────────────┐
│  Event Sources  │  (Simulated security events)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Event Generator │  (Java - US-103)
│   (Java/Kafka)  │  Produces events to Kafka
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Kafka Cluster  │  (Event streaming backbone)
│   (3 brokers)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│Stream Processor │  (C++ - US-104, US-105)
│  (C++/RocksDB)  │  Consumes, processes, stores
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Query API     │  (Java - US-301)
│  (Spring Boot)  │  REST API for queries
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   Monitoring    │  (Prometheus + Grafana)
│  & Dashboards   │
└─────────────────┘
```

### Component Responsibilities

#### 1. Event Generator (Java)
- **Purpose**: Generate realistic security events for testing
- **Language**: Java 17 (chosen for Kafka client maturity)
- **Key Features**:
  - Generates 5 event types: auth, network, file, process, DNS
  - Configurable rate (1K-50K events/sec)
  - Realistic data distributions
  - Multiple threat scenarios

#### 2. Stream Processor (C++)
- **Purpose**: High-performance event processing and threat detection
- **Language**: C++17 (chosen for raw performance)
- **Key Features**:
  - Multi-threaded Kafka consumer
  - RocksDB for time-series storage
  - ML model inference
  - Sub-100ms latency target

#### 3. Query API (Java)
- **Purpose**: RESTful API for querying events and threats
- **Language**: Java 17 + Spring Boot (rapid development)
- **Key Features**:
  - Real-time event queries
  - Historical analysis
  - Threat detection queries
  - Metrics and statistics

#### 4. Infrastructure
- **Kafka**: Event streaming (3 brokers for HA)
- **RocksDB**: Embedded time-series storage
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards

### Key Architectural Decisions

#### Decision 1: Polyglot Architecture (Java + C++)

**Decision**: Use Java for event generation and API, C++ for stream processing

**Rationale**:
- **Java for Generator/API**: 
  - Excellent Kafka client library
  - Spring Boot for rapid API development
  - Large ecosystem for JSON, HTTP, testing
  - Team familiarity
- **C++ for Processor**: 
  - Raw performance needed for 50K events/sec
  - Direct memory control
  - Low latency (sub-100ms requirement)
  - Excellent RocksDB integration

**Trade-offs**:
- ✅ Optimal performance where needed
- ✅ Rapid development where appropriate
- ⚠️ Multiple build systems (Maven + CMake)
- ⚠️ Need expertise in both languages

#### Decision 2: Kafka as Event Backbone

**Decision**: Use Apache Kafka for event streaming

**Rationale**:
- Industry standard for event streaming
- Excellent performance (millions of events/sec)
- Built-in durability and replication
- Horizontal scalability via partitioning
- Rich ecosystem and tooling

**Alternatives Considered**:
- **RabbitMQ**: Lower throughput, not designed for streaming
- **AWS Kinesis**: Vendor lock-in, higher cost, less control
- **Redis Streams**: Less mature, limited durability

#### Decision 3: RocksDB for Storage

**Decision**: Use RocksDB embedded in C++ processor

**Rationale**:
- Embedded (no network overhead)
- Optimized for time-series workloads
- Excellent write performance
- Low latency reads
- Used by production systems (MySQL, Cassandra internals)

**Alternatives Considered**:
- **PostgreSQL/TimescaleDB**: Network latency unacceptable
- **Cassandra**: Overkill for single-node, deployment complexity
- **InfluxDB**: Separate service, network overhead

#### Decision 4: Event Schema Design

**Decision**: Unified event schema with type discrimination

**Rationale**:
- Single schema simplifies processing pipeline
- Type-specific metadata in flexible structure
- Easy to extend with new event types
- Efficient serialization (JSON)

**Schema Design**:
```json
{
  "event_id": "evt_XXXXXXXXXXXX",
  "timestamp": 1704067200000,
  "event_type": "auth_attempt|network_connection|file_access|process_execution|dns_query",
  "source_ip": "192.168.1.100",
  "destination_ip": "10.0.0.5",
  "user": "alice",
  "status": "success|failed|blocked|pending",
  "threat_score": 0.85,
  "metadata": { /* type-specific fields */ }
}
```

---

## Technology Stack & Rationale

### Core Technologies

| Technology | Version | Purpose | Why Chosen |
|------------|---------|---------|------------|
| **Java** | 17 (LTS) | Event Generator, Query API | Mature ecosystem, Kafka clients, Spring Boot |
| **C++** | 17 | Stream Processor | Performance, low latency, memory control |
| **Apache Kafka** | 3.6+ | Event Streaming | Industry standard, high throughput, durability |
| **RocksDB** | 8.x | Time-series Storage | Embedded, high performance, LSM-tree design |
| **Spring Boot** | 3.x | Query API Framework | Rapid development, rich ecosystem |
| **Docker** | Latest | Containerization | Consistent environments, easy deployment |
| **Prometheus** | Latest | Metrics Collection | Industry standard, pull-based, rich ecosystem |
| **Grafana** | Latest | Visualization | Beautiful dashboards, Prometheus integration |

### Build Tools

| Tool | Purpose | Configuration |
|------|---------|---------------|
| **Maven** | Java build tool | `pom.xml` files in Java modules |
| **CMake** | C++ build system | `CMakeLists.txt` in stream-processor |
| **Docker Compose** | Local dev environment | `docker-compose.yml` |

### Development Environment

| Component | Details |
|-----------|---------|
| **OS** | macOS (M1/ARM64) |
| **IDE** | IntelliJ IDEA + CLion (JetBrains suite) |
| **Package Manager** | Homebrew |
| **Git** | Version control |
| **GitHub** | Remote repository, issue tracking |

### Key Libraries

#### Java Dependencies
```xml
<!-- Kafka Client -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.0</version>
</dependency>

<!-- Jackson for JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- Spring Boot (for Query API) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

#### C++ Dependencies
```cmake
# nlohmann/json for JSON parsing
find_package(nlohmann_json 3.2.0 REQUIRED)

# RocksDB (to be added in US-105)
find_library(ROCKSDB_LIB rocksdb)

# librdkafka (to be added in US-104)
find_library(RDKAFKA_LIB rdkafka++)

# Google Test for testing
find_package(GTest QUIET)
```

---

## Development Environment

### Prerequisites Installed

```bash
# Homebrew packages (M1 Mac)
brew install cmake
brew install nlohmann-json
brew install rocksdb
brew install librdkafka
brew install maven
brew install openjdk@17

# Docker Desktop for Mac (M1/ARM64)
# Already installed and running
```

### Project Structure

```
streamguard/
├── .git/                           # Git repository
├── .gitignore                      # Git ignore rules
├── README.md                       # Project overview
├── docker-compose.yml              # Local dev infrastructure
│
├── docs/                           # Documentation
│   ├── architecture.md             # Architecture documentation
│   ├── event-schema.json           # Event schema definition
│   ├── event-schema-documentation.md  # Schema docs
│   ├── sample-events.json          # Sample events for testing
│   └── ISSUES.md                   # Issue tracking (if not using GitHub)
│
├── event-generator/                # Java event generator (US-103)
│   ├── pom.xml                     # Maven configuration
│   ├── Dockerfile                  # Docker image definition
│   └── src/
│       ├── main/java/com/streamguard/
│       │   └── model/              # Event models (COMPLETED US-102)
│       │       ├── Event.java
│       │       ├── EventType.java
│       │       ├── EventStatus.java
│       │       ├── EventMetadata.java
│       │       ├── AuthEvent.java
│       │       ├── NetworkEvent.java
│       │       ├── FileEvent.java
│       │       ├── ProcessEvent.java
│       │       └── DnsEvent.java
│       └── test/java/com/streamguard/
│           └── model/
│               └── EventSerializationTest.java  # 14 tests passing
│
├── stream-processor/               # C++ stream processor (US-104, US-105)
│   ├── CMakeLists.txt              # CMake build configuration
│   ├── Dockerfile                  # Docker image definition
│   ├── include/                    # Header files
│   │   └── event.h                 # Event structures (COMPLETED US-102)
│   ├── src/                        # Source files
│   │   └── event.cpp               # Event implementation (COMPLETED US-102)
│   ├── test/                       # Unit tests
│   │   └── event-test.cpp          # 6 tests passing
│   └── build/                      # CMake build directory (gitignored)
│
├── query-api/                      # Java REST API (US-301+)
│   ├── pom.xml                     # Maven configuration
│   ├── Dockerfile                  # Docker image definition
│   └── src/
│       └── main/java/com/streamguard/
│           └── api/                # API controllers (future)
│
└── scripts/                        # Utility scripts
    ├── setup-dev-env.sh            # Development environment setup
    └── verify-setup.sh             # Verification script
```

### Docker Compose Services

```yaml
services:
  # Kafka Infrastructure
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]
    
  kafka-1, kafka-2, kafka-3:
    image: confluentinc/cp-kafka:7.5.0
    3 broker cluster for high availability
    
  # Monitoring
  prometheus:
    image: prom/prometheus
    ports: ["9090:9090"]
    
  grafana:
    image: grafana/grafana
    ports: ["3000:3000"]
```

### Build Commands

```bash
# Java (Event Generator)
cd event-generator
mvn clean package
mvn test

# C++ (Stream Processor)
cd stream-processor
mkdir -p build && cd build
cmake ..
make
./event-tests

# Docker Compose (Infrastructure)
docker-compose up -d
docker-compose ps
docker-compose down
```

---

## What We've Built

### ✅ Completed: US-101 - Development Environment Setup

**Status**: COMPLETE (Closed #101)  
**Date**: October 8, 2025  
**Time Spent**: ~2 hours

**Accomplishments**:
1. ✅ Complete project structure created
2. ✅ Docker Compose configuration for Kafka + monitoring
3. ✅ CMake build system for C++ processor
4. ✅ Maven configuration for Java components
5. ✅ Development environment verified on Mac M1
6. ✅ All dependencies installed and tested
7. ✅ Documentation created

**Key Files Created**:
- `docker-compose.yml` - Infrastructure definition
- `stream-processor/CMakeLists.txt` - C++ build config
- `event-generator/pom.xml` - Java build config
- `README.md` - Project documentation
- `docs/architecture.md` - Architecture docs

**Verification**:
```bash
✅ Docker Compose starts all services
✅ Kafka brokers are healthy
✅ Prometheus collecting metrics
✅ Grafana accessible at localhost:3000
✅ Maven builds successfully
✅ CMake configures successfully
```

### ✅ Completed: US-102 - Event Data Model

**Status**: COMPLETE (Closed #102)  
**Date**: October 9, 2025  
**Time Spent**: ~1.5 hours

**Accomplishments**:
1. ✅ Unified event schema defined (5 event types)
2. ✅ Java POJOs with Jackson serialization
3. ✅ C++ structs with nlohmann/json support
4. ✅ Sample event fixtures created
5. ✅ Comprehensive schema documentation
6. ✅ Full test coverage (20 tests passing)

**Event Types Implemented**:
1. **auth_attempt** - Authentication events
2. **network_connection** - Network connections
3. **file_access** - File system access
4. **process_execution** - Process execution
5. **dns_query** - DNS resolution requests

**Key Files Created**:

*Java Implementation*:
- `event-generator/src/main/java/com/streamguard/model/Event.java`
- `event-generator/src/main/java/com/streamguard/model/EventType.java`
- `event-generator/src/main/java/com/streamguard/model/EventStatus.java`
- `event-generator/src/main/java/com/streamguard/model/EventMetadata.java`
- `event-generator/src/main/java/com/streamguard/model/AuthEvent.java`
- `event-generator/src/main/java/com/streamguard/model/NetworkEvent.java`
- `event-generator/src/main/java/com/streamguard/model/FileEvent.java`
- `event-generator/src/main/java/com/streamguard/model/ProcessEvent.java`
- `event-generator/src/main/java/com/streamguard/model/DnsEvent.java`
- `event-generator/src/test/java/com/streamguard/model/EventSerializationTest.java`

*C++ Implementation*:
- `stream-processor/include/event.h`
- `stream-processor/src/event.cpp`
- `stream-processor/test/event-test.cpp`

*Documentation*:
- `docs/event-schema.json`
- `docs/event-schema-documentation.md`
- `docs/sample-events.json`

**Test Results**:
```bash
Java Tests:  14/14 passed ✅
C++ Tests:   6/6 passed ✅
Total:       20/20 passed ✅
```

**Event Schema Example**:
```json
{
  "event_id": "evt_a1b2c3d4e5f6",
  "timestamp": 1704067200000,
  "event_type": "auth_attempt",
  "source_ip": "192.168.1.100",
  "destination_ip": "10.0.0.5",
  "user": "alice",
  "status": "success",
  "threat_score": 0.05,
  "metadata": {
    "user_agent": "Mozilla/5.0",
    "geo_location": "US-MN-Minneapolis"
  }
}
```

---

## Code Structure & Patterns

### Java Code Patterns

#### 1. Event Model (Builder Pattern)

```java
// Using Builder pattern for clean object construction
Event event = new Event.Builder()
    .eventId("evt_test12345678")
    .timestamp(System.currentTimeMillis())
    .eventType(EventType.AUTH_ATTEMPT)
    .sourceIp("192.168.1.100")
    .user("alice")
    .status(EventStatus.SUCCESS)
    .threatScore(0.05)
    .build();
```

#### 2. Serialization (Jackson)

```java
// Jackson annotations for JSON serialization
@JsonProperty("event_id")
private String eventId;

@JsonProperty("event_type")
private EventType eventType;

// Serialization
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(event);
Event parsed = mapper.readValue(json, Event.class);
```

#### 3. Validation

```java
// Every event has validation logic
public boolean isValid() {
    if (!eventId.matches("^evt_[a-zA-Z0-9]{12}$")) {
        return false;
    }
    if (threatScore < 0.0 || threatScore > 1.0) {
        return false;
    }
    return true;
}
```

### C++ Code Patterns

#### 1. Event Structures

```cpp
// Modern C++17 patterns
namespace streamguard {
    enum class EventType {
        AUTH_ATTEMPT,
        NETWORK_CONNECTION,
        // ...
    };
    
    struct Event {
        std::string event_id;
        uint64_t timestamp;
        EventType event_type;
        // ...
        
        bool isValid() const;
        std::string toJson() const;
        static Event fromJson(const std::string& json);
    };
}
```

#### 2. JSON Serialization (nlohmann/json)

```cpp
// Custom to_json / from_json functions
void to_json(nlohmann::json& j, const Event& event) {
    j = nlohmann::json{
        {"event_id", event.event_id},
        {"timestamp", event.timestamp},
        {"event_type", eventTypeToString(event.event_type)},
        // ...
    };
}

void from_json(const nlohmann::json& j, Event& event) {
    j.at("event_id").get_to(event.event_id);
    // ...
}
```

#### 3. Error Handling

```cpp
// RAII and exception-based error handling
try {
    Event event = Event::fromJson(jsonString);
    if (!event.isValid()) {
        throw std::invalid_argument("Invalid event");
    }
} catch (const nlohmann::json::exception& e) {
    // Handle JSON parsing error
}
```

### Naming Conventions

**Java**:
- Classes: `PascalCase` (Event, EventType)
- Methods: `camelCase` (isValid, getEventId)
- Constants: `UPPER_SNAKE_CASE` (MAX_RETRIES)
- Packages: `lowercase.dot.separated` (com.streamguard.model)

**C++**:
- Classes/Structs: `PascalCase` (Event, EventMetadata)
- Functions: `camelCase` (isValid, toJson)
- Enums: `PascalCase` with enum class
- Namespaces: `lowercase` (streamguard)
- Files: `snake_case.cpp` (event.cpp, kafka_consumer.cpp)

---

## Design Principles & Guidelines

### 1. Performance First

**Principle**: Every design decision considers performance impact

**Guidelines**:
- Minimize memory allocations in hot paths
- Use appropriate data structures (vectors over lists)
- Profile before optimizing
- Set concrete performance targets
- Measure everything

**Examples**:
- C++ chosen for stream processor (performance critical)
- RocksDB embedded (avoid network latency)
- Multi-threaded consumer (parallel processing)

### 2. Type Safety

**Principle**: Use strong typing to catch errors at compile time

**Guidelines**:
- Use `enum class` in C++ (not plain enums)
- Use Java enums for fixed sets
- Validate inputs at boundaries
- Use builders to enforce constraints

**Examples**:
```cpp
// GOOD: enum class prevents implicit conversions
enum class EventType { AUTH_ATTEMPT, NETWORK_CONNECTION };

// BAD: plain enum allows implicit int conversion
enum EventType { AUTH_ATTEMPT, NETWORK_CONNECTION };
```

### 3. Fail Fast

**Principle**: Detect and report errors as early as possible

**Guidelines**:
- Validate input immediately
- Use assertions in development
- Throw meaningful exceptions
- Log errors with context

**Examples**:
```java
public Event(String eventId, ...) {
    if (!eventId.matches("^evt_[a-zA-Z0-9]{12}$")) {
        throw new IllegalArgumentException("Invalid event_id format");
    }
    this.eventId = eventId;
}
```

### 4. Testability

**Principle**: Write code that's easy to test

**Guidelines**:
- Keep functions small and focused
- Inject dependencies
- Use interfaces/abstractions
- Write tests alongside code
- Aim for >80% coverage

**Current State**:
- Java: 14 tests covering serialization
- C++: 6 tests covering serialization
- Target: Add integration tests in future sprints

### 5. Documentation

**Principle**: Code should be self-documenting, but complex logic needs comments

**Guidelines**:
- Use descriptive names
- Comment "why" not "what"
- Document public APIs
- Keep docs close to code
- Update docs with code changes

### 6. Incremental Development

**Principle**: Build incrementally, validate frequently

**Guidelines**:
- Complete one user story at a time
- Test after each change
- Commit working code frequently
- Keep main branch deployable

**Workflow**:
1. Implement feature
2. Write tests
3. Verify locally
4. Commit with descriptive message
5. Move to next feature

---

## Current Sprint

### Sprint 1: Foundation & Infrastructure

**Duration**: Week 1 (October 8-12, 2025)  
**Goal**: Build foundation for real-time event processing

**Status**: Day 2 (2/5 user stories complete)

### Completed User Stories

✅ **US-101**: Development Environment Setup (Day 1)  
✅ **US-102**: Event Data Model (Day 2)

### In Progress

🚧 **US-103**: Event Generator Implementation (Starting now with Claude Code!)

### Remaining This Sprint

📋 **US-104**: Basic C++ Kafka Consumer  
📋 **US-105**: RocksDB Integration  
📋 **US-106**: End-to-End Pipeline Test

### Sprint 1 Timeline

```
Day 1 (Oct 8):  US-101 ✅ (Environment Setup)
Day 2 (Oct 9):  US-102 ✅ (Event Model)
                US-103 🚧 (Event Generator) <- YOU ARE HERE
Day 3 (Oct 10): US-104 (Kafka Consumer)
Day 4 (Oct 11): US-105 (RocksDB Integration)
Day 5 (Oct 12): US-106 (End-to-End Test)
```

---

## Roadmap & Future Sprints

### Sprint 2: Multi-threaded Processing (Week 2)
- US-201: Multi-threaded consumer
- US-202: Event filtering
- US-203: Basic aggregations
- US-204: Performance benchmarking
- US-205: Load testing

### Sprint 3: ML Integration (Week 3)
- US-301: Basic ML model integration
- US-302: Threat score calculation
- US-303: Pattern detection
- US-304: Anomaly detection
- US-305: Model performance testing

### Sprint 4: Query API (Week 4)
- US-401: Spring Boot API setup
- US-402: Real-time event queries
- US-403: Historical queries
- US-404: Aggregation endpoints
- US-405: API documentation

### Sprint 5: Production Readiness (Week 5-6)
- US-501: Comprehensive monitoring
- US-502: Alerting system
- US-503: Error handling & retry logic
- US-504: Graceful degradation
- US-505: Documentation completion
- US-506: Performance optimization
- US-507: Security hardening
- US-508: Production deployment

---

## Key Decisions & Rationale

### Technology Decisions

#### Why Java 17?
- LTS release (long-term support)
- Modern language features (records, pattern matching)
- Excellent Kafka client library
- Spring Boot ecosystem
- Team familiarity

#### Why C++17?
- Performance requirements (<100ms latency)
- Direct memory control
- Efficient multi-threading
- Modern C++ features (auto, lambdas, smart pointers)
- RocksDB written in C++

#### Why Not Go/Rust?
**Go**:
- ✅ Good concurrency, but GC pauses unacceptable for latency target
- ✅ Simple deployment, but need raw performance

**Rust**:
- ✅ Excellent performance and safety
- ⚠️ Steeper learning curve
- ⚠️ Less mature ecosystem for Kafka/RocksDB
- ⚠️ Team would need ramp-up time

**Decision**: C++ offers best balance of performance and team expertise

#### Why Not Kubernetes Initially?
- Docker Compose sufficient for development
- Avoid complexity until needed
- Kubernetes planned for Sprint 5 (production deployment)
- Focus on core functionality first

### Design Decisions

#### Why Unified Event Schema?
**Alternative**: Different schemas per event type

**Decision**: Unified schema with type discrimination

**Rationale**:
- Simpler processing pipeline (one consumer, one processor)
- Easier to add new event types
- Consistent tooling and monitoring
- Flexible metadata structure

**Trade-off**:
- ⚠️ Some metadata fields unused for certain types
- ✅ But storage is cheap, simplicity is valuable

#### Why Embed RocksDB vs Separate Database?
**Alternatives Considered**:
- PostgreSQL with TimescaleDB
- Cassandra cluster
- InfluxDB

**Decision**: Embedded RocksDB

**Rationale**:
- Zero network latency
- Simple deployment (no separate service)
- Excellent write performance for time-series
- Lower operational complexity
- Scales with processor (add nodes = add storage)

**Trade-off**:
- ⚠️ Storage limited to node disk
- ⚠️ Backup/recovery per-node
- ✅ But acceptable for 50K events/sec target

---

## Testing Strategy

### Unit Tests

**Java**:
- JUnit 5 for all unit tests
- Mockito for mocking (when needed)
- Target: >80% code coverage
- Run on every build: `mvn test`

**C++**:
- Google Test for unit tests
- Target: >80% code coverage
- Run on every build: `./event-tests`

### Integration Tests

**Planned for Sprint 1-2**:
- Kafka producer/consumer integration
- RocksDB read/write integration
- End-to-end pipeline test
- Use Docker Compose for test environment

### Performance Tests

**Planned for Sprint 2**:
- Load testing with realistic event rates
- Latency measurements (p50, p95, p99)
- Throughput benchmarks
- Memory profiling
- CPU profiling

### Test Data

**Sample Events**:
- Located in `docs/sample-events.json`
- Covers all 5 event types
- Includes edge cases (failed auth, blocked process, etc.)
- Realistic data distributions

---

## Performance Targets

### Throughput
- **Minimum**: 10,000 events/second per node
- **Target**: 50,000 events/second per node
- **Stretch**: 100,000 events/second per node

### Latency
- **Maximum**: <100ms end-to-end (p95)
- **Target**: <50ms end-to-end (p95)
- **Stretch**: <20ms end-to-end (p95)

### Resource Usage
- **CPU**: <80% utilization at target throughput
- **Memory**: <4GB RSS per processor node
- **Disk**: RocksDB compaction doesn't impact latency
- **Network**: <1Gbps per node

### Scalability
- **Target**: Linear scaling up to 10 nodes
- **Partitioning**: Events partitioned by source_ip
- **Rebalancing**: <1s consumer rebalancing time

### Reliability
- **Uptime**: 99.9% availability target
- **Data Loss**: Zero message loss (Kafka durability)
- **Recovery**: <10s recovery time after failure

---

## Quick Reference

### Common Commands

```bash
# Start infrastructure
docker-compose up -d

# Stop infrastructure
docker-compose down

# Build Java
cd event-generator && mvn clean package

# Run Java tests
cd event-generator && mvn test

# Build C++
cd stream-processor && mkdir -p build && cd build && cmake .. && make

# Run C++ tests
cd stream-processor/build && ./event-tests

# View Kafka topics
docker exec -it kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

# Produce test event
echo '{"event_id":"evt_test12345678",...}' | \
  docker exec -i kafka-1 kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic security-events

# Consume events
docker exec -it kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic security-events \
  --from-beginning

# View Prometheus metrics
open http://localhost:9090

# View Grafana dashboards
open http://localhost:3000
```

### Project URLs

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Kafka Broker 1**: localhost:9092
- **Kafka Broker 2**: localhost:9093
- **Kafka Broker 3**: localhost:9094

### Important Files

```
streamguard/
├── docker-compose.yml              # Infrastructure definition
├── README.md                       # Project overview
├── docs/
│   ├── architecture.md             # Architecture docs
│   ├── event-schema-documentation.md  # Schema reference
│   └── sample-events.json          # Test data
├── event-generator/
│   ├── pom.xml                     # Java dependencies
│   └── src/main/java/com/streamguard/model/Event.java
├── stream-processor/
│   ├── CMakeLists.txt              # C++ build config
│   ├── include/event.h             # Event header
│   └── src/event.cpp               # Event implementation
└── query-api/
    └── pom.xml                     # API dependencies
```

### Event Schema Summary

**5 Event Types**:
1. `auth_attempt` - Login/logout attempts
2. `network_connection` - Network connections
3. `file_access` - File system access
4. `process_execution` - Process launches
5. `dns_query` - DNS lookups

**Required Fields**:
- `event_id`: Format `evt_XXXXXXXXXXXX`
- `timestamp`: Unix milliseconds
- `event_type`: One of 5 types
- `source_ip`: IPv4 address
- `user`: Username
- `status`: success|failed|blocked|pending
- `threat_score`: 0.0 to 1.0

### Git Workflow

```bash
# Check status
git status

# Add files
git add <files>

# Commit with issue reference
git commit -m "feat: Complete US-XXX - <Title> (#XXX)

<Description>

Closes #XXX"

# Push
git push origin main

# View history
git log --oneline -10
```

---

## Next Steps (For Claude Code)

### Immediate: US-103 - Event Generator

**Goal**: Implement Java application that generates realistic security events

**Tasks**:
1. Create `EventGenerator.java` main class
2. Implement Kafka producer configuration
3. Create `EventFactory.java` for generating realistic events
4. Add command-line argument parsing
5. Implement rate limiting
6. Add graceful shutdown handling
7. Create Dockerfile
8. Write unit tests
9. Update documentation

**Expected Output**:
- Configurable event generation (1K-50K events/sec)
- All 5 event types generated with realistic distributions
- Kafka producer sending to `security-events` topic
- Command-line interface: `java -jar event-generator.jar --rate 10000`
- Docker image: `streamguard/event-generator:latest`

**Estimated Time**: 1.5-2 hours with Claude Code

### Context for Claude Code

You (Claude Code) are now working on a high-performance security event processing platform. You have:

✅ **Working event data model** in both Java and C++  
✅ **Full development environment** with Kafka, Docker, Maven, CMake  
✅ **Clear architecture** and design patterns  
✅ **Comprehensive tests** (20/20 passing)  

Your next task is to build the Event Generator (US-103) that will produce realistic security events to Kafka for testing the entire pipeline.

**Key Points**:
- Follow existing code patterns (Builder pattern, Jackson annotations)
- Use the Event models already created
- Write tests for core functionality
- Follow naming conventions
- Consider performance (target: 50K events/sec)
- Add proper logging and error handling

Good luck! 🚀

---

**END OF HANDOFF DOCUMENT**

*This document will be updated as the project progresses.*