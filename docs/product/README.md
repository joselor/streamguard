<div align="center">

# 🛡️ StreamGuard Documentation

**Real-Time Security Event Stream Processing Demo**

<img src="https://img.shields.io/badge/Built_for-CrowdStrike-E01F27?style=for-the-badge" alt="CrowdStrike"/>
<img src="https://img.shields.io/badge/C++-17-00599C?style=for-the-badge&logo=cplusplus" alt="C++17"/>
<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk" alt="Java 17"/>
<img src="https://img.shields.io/badge/Kafka-Latest-231F20?style=for-the-badge&logo=apache-kafka" alt="Kafka"/>
<img src="https://img.shields.io/badge/RocksDB-8.9-4A90E2?style=for-the-badge" alt="RocksDB"/>

**Demo Project | Lambda Architecture | AI-Powered Threat Detection**

[Architecture](./guides/ARCHITECTURE.md) • [Quick Start](./guides/QUICK_START.md) • [API Docs](./api/API_REFERENCE.md) • [Troubleshooting](./guides/TROUBLESHOOTING.md)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [What This Is (and Isn't)](#-what-this-is-and-isnt)
- [Key Features](#-key-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Quick Start](#-quick-start)
- [Components](#-components)
- [AI/ML Capabilities](#-aiml-capabilities)
- [Monitoring & Observability](#-monitoring--observability)
- [Performance](#-performance)
- [Documentation](#-documentation)

---

## 🎯 Overview

**StreamGuard** is a demonstration project showcasing real-time security event stream processing with AI-powered threat analysis and behavioral anomaly detection. Built in 2 weeks to demonstrate proficiency with CrowdStrike's technology stack.

### 💡 Built to Demonstrate

- **Real-time processing** of security events (~10K events/sec)
- **AI-powered threat analysis** using OpenAI GPT-4o-mini (selective, cost-conscious)
- **Statistical anomaly detection** with probabilistic baselines
- **Lambda Architecture** with real-time and batch processing layers
- **Production-ready patterns** (monitoring, graceful shutdown, error handling)

### 🎯 Demo Objectives

This project demonstrates proficiency in:

1. ✅ **Stream Processing**: High-throughput event ingestion via Kafka
2. ✅ **Multi-language Architecture**: C++ (performance) + Java (API)
3. ✅ **AI Integration**: Selective OpenAI GPT-4o-mini threat analysis
4. ✅ **Real-time Analytics**: Statistical anomaly detection (no ML training)
5. ✅ **Data Storage**: Time-series optimized RocksDB with column families
6. ✅ **Observability**: Prometheus metrics + Grafana dashboards
7. ✅ **Production Patterns**: Signal handling, error handling, scalability

---

## 🔍 What This Is (and Isn't)

### What It Is ✅

- **Working demonstration** of distributed streaming architecture
- **Technology showcase** for CrowdStrike job application
- **Learning project** completed in 2 weeks
- **Portfolio piece** demonstrating architectural thinking
- **Honest effort** to build something functional and well-documented

### What It's NOT ❌

- **NOT production-ready** enterprise software
- **NOT optimized** for millions of events/second
- **NOT security-hardened** for real-world deployment
- **NOT feature-complete** - intentionally scoped for demo

---

## ✨ Key Features

### 🚀 High-Performance Stream Processing

- **C++17** stream processor with Kafka consumer
- **Sub-5ms** P99 event processing latency
- **Graceful shutdown** with signal handling (SIGINT/SIGTERM)
- **Persistent storage** in RocksDB with column families

### 🤖 AI-Powered Threat Analysis (Selective)

- **OpenAI GPT-4o-mini** integration for intelligent threat assessment
- **Cost-conscious** design: Only analyzes 3-5% of events
- **Interactive opt-in**: User must explicitly enable AI at startup
- **Conditional triggering**: threat_score >= 0.7 OR anomaly detected
- **Context-aware** analysis with severity classification
- **Graceful degradation**: System works without AI

### 📊 Statistical Anomaly Detection

- **Probabilistic baselines** per user (no ML training required)
- **5-dimensional scoring**:
  - Time anomaly (25%): Unusual hours
  - IP anomaly (30%): New/rare source IPs
  - Location anomaly (20%): Geographic anomalies
  - Type anomaly (15%): Unusual event patterns
  - Failure anomaly (10%): Failed attempt spikes
- **Continuous learning** with 100-event baseline window
- **Real-time alerts** with configurable thresholds

### 🔍 REST Query API

- **Spring Boot 3.2** RESTful API
- **Swagger/OpenAPI** documentation
- **Multi-dimensional queries**:
  - Events by time range, user, type, threat score
  - Anomalies by user, score, time range
  - AI analyses by severity, event
- **Read-only RocksDB** access via Java bindings

### 📈 Monitoring & Observability

- **Prometheus metrics** on all components
- **Grafana dashboards** for real-time visualization
- **Custom metrics**:
  - Events processed/sec by type
  - Threat detection rates by severity
  - Anomaly score distribution
  - Processing latency histograms
  - Storage size tracking

---

## 🏗️ Architecture

### System Overview

```
┌────────────────────────────────────────────────────────────┐
│              StreamGuard Platform (Demo)                   │
│                 Lambda Architecture                         │
└────────────────────────────────────────────────────────────┘

┌──────────────┐       ┌─────────────┐       ┌──────────────┐
│    Event     │──────▶│   Apache    │◀─────▶│   ZooKeeper  │
│  Generator   │       │   Kafka     │       │              │
│   (Java)     │       │             │       └──────────────┘
└──────────────┘       └──────┬──────┘
                              │
                ┌─────────────┴──────────────┐
                │                            │
                ▼                            ▼
     ┌────────────────────┐       ┌──────────────────────┐
     │  SPEED LAYER       │       │  BATCH LAYER         │
     │  Stream Processor  │       │  Spark ML Pipeline   │
     │  (C++)             │       │  (PySpark)           │
     │                    │       │                      │
     │ • Kafka Consumer   │       │ • Historical Data    │
     │ • Anomaly Detector │       │ • Feature Generation │
     │ • AI Analyzer      │       │ • Training Data      │
     │ • RocksDB Storage  │       │                      │
     └──────┬─────────────┘       └──────────────────────┘
            │
            ▼
     ┌──────────────────────────────────┐
     │       SERVING LAYER              │
     │                                  │
     │  • Query API (Java/Spring Boot)  │
     │  • RocksDB Reader                │
     │  • REST Endpoints                │
     │  • Swagger UI                    │
     └──────────────────────────────────┘
            │
            ▼
     ┌──────────────────────────────────┐
     │       MONITORING                 │
     │                                  │
     │  • Prometheus (Metrics)          │
     │  • Grafana (Dashboards)          │
     │  • Kafka UI (Management)         │
     └──────────────────────────────────┘
```

### Data Flow

For detailed data flow visualization, see [DATA_FLOW_ANIMATION.md](./diagrams/DATA_FLOW_ANIMATION.md)

**Key Flow:**
1. **Event Generation** → Java producer sends security events to Kafka
2. **Stream Processing** → C++ processor consumes, analyzes, stores in RocksDB
3. **Anomaly Detection** → Statistical scoring identifies behavioral anomalies
4. **AI Analysis** (Optional) → High-threat events analyzed by GPT-4o-mini
5. **Query Layer** → Spring Boot API serves data from RocksDB
6. **Monitoring** → Prometheus scrapes metrics, Grafana visualizes

---

## 🛠️ Technology Stack

### Stream Processing Layer

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | C++17 | High-performance processing |
| **Message Broker** | Apache Kafka | Event streaming |
| **Kafka Client** | librdkafka++ | C++ Kafka consumer |
| **Storage** | RocksDB | Embedded time-series database |
| **JSON** | nlohmann/json | Event serialization |
| **Metrics** | prometheus-cpp | Performance monitoring |

### Query API Layer

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | Java 17 | Enterprise API platform |
| **Framework** | Spring Boot 3.2 | RESTful API |
| **Database** | RocksDB Java | Direct DB access |
| **Documentation** | Swagger/OpenAPI | API docs |
| **Build** | Maven | Dependency management |

### AI Layer

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **AI Service** | OpenAI GPT-4o-mini | Threat analysis |
| **HTTP Client** | libcurl (C++) | API integration |
| **Trigger Logic** | Custom C++ | Selective analysis (3-5%) |

### Monitoring

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Metrics** | Prometheus | Time-series metrics |
| **Visualization** | Grafana | Dashboards |
| **Logging** | stdout/stderr | Structured logging |

### Batch Processing (Optional)

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Engine** | Apache Spark 3.5 | Batch processing |
| **Language** | PySpark | ML feature engineering |
| **Storage** | Parquet | Training data export |

---

## 🚀 Quick Start

### Prerequisites

```bash
# macOS (M1/M2/M3 ARM64)
./scripts/install_deps.sh  # Automated installation

# Or manual:
brew install cmake rocksdb librdkafka nlohmann-json prometheus-cpp maven openjdk@17
```

### Using Automated Scripts (Recommended)

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Start components (3 terminals)
./scripts/start-event-generator.sh    # Terminal 1: Generates events
./scripts/start-stream-processor.sh   # Terminal 2: C++ processor (prompts for AI)
./scripts/start-query-api.sh          # Terminal 3: REST API

# 3. Verify
curl http://localhost:8081/api/events/recent?limit=5 | jq
open http://localhost:3000  # Grafana (admin/admin)
```

### Manual Build & Run

See [QUICK_START.md](./guides/QUICK_START.md) for detailed manual instructions.

---

## 📦 Components

### 1. Stream Processor (C++)

High-performance event processor with real-time anomaly detection.

**Location**: `stream-processor/`

**Key Features**:
- Kafka consumer with graceful shutdown
- RocksDB storage with 3 column families
- Statistical anomaly detection
- Selective AI threat analysis (opt-in)
- Prometheus metrics export

**Starting**:
```bash
./scripts/start-stream-processor.sh
# Prompts: "Enable AI-powered threat analysis? (y/n):"
```

[Component Details →](../../stream-processor/README.md)

### 2. Query API (Java)

RESTful API for querying security events, anomalies, and AI analyses.

**Location**: `query-api/`

**Key Endpoints**:
- `GET /api/events/recent` - Latest events
- `GET /api/anomalies/recent` - Latest anomalies
- `GET /api/analyses/severity/{severity}` - AI analyses by severity
- `GET /api/stats/summary` - System statistics

[API Documentation →](./api/API_REFERENCE.md) | [Swagger UI](http://localhost:8081/swagger-ui.html)

### 3. Event Generator (Java)

Synthetic security event generator for testing.

**Location**: `event-generator/`

**Features**:
- Configurable event rate
- Realistic security event patterns
- Simulates normal and anomalous behavior

---

## 🤖 AI/ML Capabilities

### Selective AI Threat Analysis

**Cost-Conscious Design:**
- Only analyzes 3-5% of events (95%+ cost savings)
- Trigger conditions: `threat_score >= 0.7 OR anomaly detected`
- Interactive opt-in at startup (default: disabled)
- Graceful degradation if API unavailable

**Model**: OpenAI GPT-4o-mini

**Example Output**:
```json
{
  "event_id": "evt_123",
  "severity": "HIGH",
  "confidence": 0.92,
  "summary": "Multiple failed login attempts from suspicious IP...",
  "indicators": [
    "Brute force pattern detected",
    "Non-working hours access",
    "Geographic anomaly"
  ],
  "recommendation": "Block source IP, reset credentials, enable MFA"
}
```

### Statistical Anomaly Detection

**Algorithm**: Probabilistic baseline tracking (no ML training)

**Baseline Establishment**: 100 events per user

**Scoring**:
- Combined score from 5 weighted factors
- Threshold: 0.7 for alerts
- Real-time updates as baselines evolve

[Full AI/ML Documentation →](./guides/AI_ML.md)

---

## 📈 Monitoring & Observability

### Prometheus Metrics

**Stream Processor** (`:8080/metrics`):
- `streamguard_events_processed_total{event_type}` - Events counter
- `streamguard_threats_detected_total{severity}` - Threats counter
- `streamguard_anomalies_detected_total{user,score_range}` - Anomalies
- `streamguard_processing_latency_seconds` - Latency histogram
- `streamguard_rocksdb_size_bytes` - Database size

**Query API** (`:8081/actuator/prometheus`):
- Spring Boot Actuator metrics
- HTTP request rates and latencies
- JVM metrics (heap, GC, threads)

### Grafana Dashboards

Access: http://localhost:3000 (admin/admin)

1. **StreamGuard - Performance**: Throughput, latency, processing rates
2. **StreamGuard - Threats**: Threat detection by severity, timeline
3. **StreamGuard - Pipeline**: End-to-end pipeline health

---

## ⚡ Performance

### Demo Performance Characteristics

| Metric | Value | Notes |
|--------|-------|-------|
| **Throughput** | ~10,000 events/sec | Single processor instance |
| **Latency (P50)** | <1ms | Event processing |
| **Latency (P99)** | <5ms | End-to-end |
| **Anomaly Detection** | <1ms | Statistical scoring |
| **AI Analysis** | ~800ms | When enabled (3-5% of events) |
| **Storage** | ~500 bytes/event | RocksDB compressed |
| **Memory** | ~200MB | Stream processor |

### Scalability

- **Horizontal**: Multiple processor instances via Kafka partitions
- **Vertical**: Multi-threaded processing
- **Storage**: RocksDB auto-compaction, column family isolation

**Production Considerations**:
- Add comprehensive test suite
- Implement security hardening
- Configure cluster deployment
- Set up disaster recovery

---

## 📖 Documentation

### Getting Started

- [Quick Start Guide](./guides/QUICK_START.md) - 10-minute setup
- [Architecture Overview](./guides/ARCHITECTURE.md) - System design deep-dive
- [API Reference](./api/API_REFERENCE.md) - Complete REST API docs

### Deep Dives

- [AI/ML Components](./guides/AI_ML.md) - Anomaly detection & AI analysis
- [Deployment Guide](./guides/DEPLOYMENT.md) - Docker, Kubernetes, AWS
- [Troubleshooting](./guides/TROUBLESHOOTING.md) - Common issues & solutions

### Visual Documentation

- [Component Diagram](./diagrams/COMPONENT_DIAGRAM.md) - Architecture visualization
- [Data Flow Animation](./diagrams/DATA_FLOW_ANIMATION.md) - ByteByGo-style diagrams
- [Class Diagrams](./diagrams/CLASS_DIAGRAMS.md) - UML class structure

### Project History

- [Sprint Documentation](../sprints/) - Sprint 1-7 handoff documents
- [Development Guides](../development/) - Testing, IDE setup, schemas
- [Integrations](../integrations/) - Spark ML pipeline integration

---

## 🎯 What This Demonstrates

### Technical Skills Demonstrated

✅ **Rapid Learning**: Mastered RocksDB, Kafka, C++ integration in 2 weeks
✅ **Polyglot Development**: C++, Java, Docker, shell scripting
✅ **System Design**: Architectural decisions with trade-off analysis
✅ **Modern Practices**: Containerization, metrics, monitoring
✅ **Problem Solving**: Signal handling fix (Sprint 6), metrics bug (Sprint 7)

### Leadership & Communication

✅ **Documentation-First**: Comprehensive guides with diagrams
✅ **Decision Transparency**: Documented all major design choices
✅ **User-Focused**: Clear setup, troubleshooting, API docs
✅ **Growth Mindset**: Honest about scope and limitations
✅ **Sprint Methodology**: 7 sprints with handoff documents

### Domain Knowledge

✅ **Security Concepts**: Threat patterns, anomaly detection
✅ **Performance Awareness**: Latency optimization, throughput
✅ **Production Thinking**: Monitoring, error handling, graceful degradation
✅ **Cost Consciousness**: Selective AI to reduce operational costs

---

## 🤝 Contributing & Feedback

This is a demonstration project for a CrowdStrike job application.

**Feedback Welcome On:**
- Architecture decisions and trade-offs
- Code quality and best practices
- Documentation clarity
- System design choices

**Areas for Enhancement** (if this were production):
- Comprehensive test suite (unit, integration, E2E)
- Security hardening (auth, encryption, secrets)
- Advanced ML models beyond statistical scoring
- Horizontal scaling implementation
- Disaster recovery (replication, backups)

---

## 📝 Project Information

**Purpose**: Technical demonstration for CrowdStrike job application
**Timeline**: 2 weeks (7 sprints)
**Status**: Demo-ready

**Author**: Jose Ortuno
**Target Role**: Senior Engineering Manager - Streaming Search at CrowdStrike

[Back to Main README](../../README.md)

---

<div align="center">

### 🛡️ StreamGuard Demo Project

**Showcasing**: Stream Processing • AI Integration • Real-Time Analytics • Production Patterns

Built with honesty and transparency for the CrowdStrike Team

</div>
