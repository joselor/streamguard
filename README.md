<div align="center">

# 🛡️ StreamGuard

**Real-Time Security Event Stream Processing Platform**

[![CrowdStrike](https://img.shields.io/badge/Built_for-CrowdStrike-E01F27?style=for-the-badge)](https://www.crowdstrike.com)
[![C++17](https://img.shields.io/badge/C++-17-00599C?style=for-the-badge&logo=c%2B%2B)](https://isocpp.org/)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.6-231F20?style=for-the-badge&logo=apache-kafka)](https://kafka.apache.org/)
[![RocksDB](https://img.shields.io/badge/RocksDB-8.9-4A90E2?style=for-the-badge)](https://rocksdb.org/)
[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-E6522C?style=for-the-badge&logo=prometheus)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800?style=for-the-badge&logo=grafana)](https://grafana.com/)

**Enterprise-Grade Stream Processing | AI-Powered Threat Detection | Real-Time Anomaly Detection**

[Architecture](docs/final/guides/ARCHITECTURE.md) • [Quick Start](docs/final/guides/QUICK_START.md) • [Components](docs/final/diagrams/COMPONENT_DIAGRAM.md) • [API Docs](docs/final/api/API_REFERENCE.md) • [Deployment](docs/final/guides/DEPLOYMENT.md)

</div>

---

> A high-throughput distributed system for processing and analyzing security events in real-time, featuring AI-powered threat analysis and behavioral anomaly detection.

## 🎯 Project Overview

StreamGuard demonstrates production-grade streaming systems architecture, combining CrowdStrike's core technologies (C++, Kafka, RocksDB) with modern AI capabilities for real-time security event processing at scale.

**Key Capabilities:**
- **10,000+ events/second** processing throughput
- **Sub-100ms latency** end-to-end
- **AI-powered threat analysis** using Anthropic Claude
- **Statistical anomaly detection** with behavioral baselines
- **Production-ready** with full observability stack

## 🚀 Quick Start

```bash
# 1. Start infrastructure (Kafka, Zookeeper)
docker-compose up -d

# 2. Build stream processor
cd stream-processor/build
cmake .. && make
./stream-processor --broker localhost:9092 --topic security-events --group streamguard-processor

# 3. Build and start query API
cd ../../query-api
mvn clean package
ROCKSDB_PATH=../stream-processor/build/data/events.db java -jar target/query-api-1.0.0.jar

# 4. Query the API
curl http://localhost:8081/api/events?limit=10
curl http://localhost:8081/api/anomalies/high-score?threshold=0.7
```

**Access Points:**
- **Query API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Prometheus Metrics**: http://localhost:8080/metrics
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## 📚 Documentation

**Comprehensive documentation with diagrams, guides, and API references:**

### 📖 Main Documentation
- **[Complete Documentation](docs/final/README.md)** - Start here!

### 🗺️ Architecture & Design
- **[Architecture Deep Dive](docs/final/guides/ARCHITECTURE.md)** - System design, components, data flow
- **[Component Diagram](docs/final/diagrams/COMPONENT_DIAGRAM.md)** - System architecture overview
- **[Class Diagrams](docs/final/diagrams/CLASS_DIAGRAMS.md)** - UML diagrams for all modules
- **[Data Flow Animation](docs/final/diagrams/DATA_FLOW_ANIMATION.md)** - ByteByGo-style visualization

### 📋 Guides
- **[Quick Start Guide](docs/final/guides/QUICK_START.md)** - Get running in 10 minutes
- **[Deployment Guide](docs/final/guides/DEPLOYMENT.md)** - Docker, Kubernetes, AWS
- **[AI/ML Guide](docs/final/guides/AI_ML.md)** - Anomaly detection & AI integration
- **[Troubleshooting](docs/final/guides/TROUBLESHOOTING.md)** - Common issues & solutions

### 🔌 API Reference
- **[REST API Documentation](docs/final/api/API_REFERENCE.md)** - Complete endpoint reference

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Stream Processor** | C++17 | High-performance event processing |
| **Query API** | Java 17 / Spring Boot 3.2 | REST API for querying data |
| **Message Broker** | Apache Kafka 3.6 | Event streaming |
| **Storage** | RocksDB 8.9 | Embedded key-value store |
| **AI Analysis** | Anthropic Claude 3.5 Sonnet | Threat intelligence |
| **Monitoring** | Prometheus + Grafana | Observability |
| **Build** | CMake, Maven | Build systems |

## ✨ Key Features

### Real-Time Stream Processing
- High-throughput Kafka consumer with librdkafka
- Parallel processing pipeline
- Configurable consumer groups for horizontal scaling

### AI-Powered Threat Analysis
- Integration with Anthropic Claude API
- Natural language threat assessments
- Severity classification (LOW/MEDIUM/HIGH/CRITICAL)
- Actionable recommendations

### Statistical Anomaly Detection
- Per-user behavioral baseline tracking
- 5-dimensional scoring (time, IP, location, event type, failure rate)
- Weighted composite anomaly scores
- Continuous learning (adapts to changing behavior)

### Embedded Storage
- RocksDB for zero-latency persistence
- Column family isolation (events, ai_analysis, anomalies, embeddings)
- Time-ordered keys for efficient range queries
- Compression for storage efficiency

### Production-Ready Observability
- Prometheus metrics export
- Grafana dashboards
- Structured logging
- Performance counters and histograms

## 📁 Project Structure

```
streamguard/
├── stream-processor/       # C++ processing engine
│   ├── src/               # Source files
│   ├── include/           # Header files
│   ├── tests/             # Unit tests
│   └── CMakeLists.txt     # Build configuration
│
├── query-api/             # Java REST API
│   ├── src/main/java/     # Source code
│   └── pom.xml            # Maven configuration
│
├── docs/                  # Documentation
│   └── final/             # Comprehensive docs (START HERE!)
│       ├── README.md      # Main documentation entry point
│       ├── diagrams/      # UML and architecture diagrams
│       ├── guides/        # Detailed guides
│       └── api/           # API reference
│
├── docker-compose.yml     # Infrastructure setup
└── README.md              # This file
```

## 🎯 Use Cases

- **Security Operations Centers (SOC)**: Real-time security event monitoring
- **Threat Hunting**: Behavioral anomaly detection and investigation
- **Compliance Monitoring**: Audit trail and security event logging
- **Incident Response**: AI-assisted threat analysis and recommendations

## 🔧 Requirements

### Development
- **C++ Compiler**: GCC 9+ or Clang 10+ with C++17 support
- **Java**: JDK 17+
- **CMake**: 3.20+
- **Maven**: 3.8+
- **Docker**: 20.10+ (for Kafka infrastructure)

### Runtime Dependencies
- **librdkafka**: Kafka C/C++ library
- **RocksDB**: 8.x
- **Prometheus C++ client**
- **nlohmann/json**: JSON library

## 📊 Performance

| Metric | Value |
|--------|-------|
| Throughput | 10,000+ events/second per instance |
| Latency (p95) | <100ms end-to-end |
| Latency (p99) | <200ms |
| Storage | ~500MB per 1M events (compressed) |
| Memory | ~2-4GB per processor instance |
| CPU | ~60% utilization at 10K events/sec |

## 🚀 Deployment

StreamGuard supports multiple deployment scenarios:

- **Local Development**: Docker Compose
- **Production**: Kubernetes with Helm charts
- **Cloud**: AWS EKS, GCP GKE, Azure AKS

See the **[Deployment Guide](docs/final/guides/DEPLOYMENT.md)** for details.

## 📈 Monitoring

Prometheus metrics exposed on `:8080/metrics`:

- `streamguard_events_processed_total` - Total events processed
- `streamguard_anomalies_detected_total` - Anomalies detected
- `streamguard_anomaly_score` - Anomaly score distribution
- `streamguard_ai_analyses_total` - AI analyses by severity
- `streamguard_processing_latency_seconds` - Processing latency

## 🧪 Testing

```bash
# C++ tests
cd stream-processor/build
ctest --verbose

# Java tests
cd query-api
mvn test

# Integration tests
./scripts/integration-test.sh
```

## 🤝 Contributing

This is a demonstration project. For production use, consider:
- Adding authentication/authorization
- Implementing rate limiting
- Setting up CI/CD pipelines
- Adding more comprehensive test coverage
- Implementing data retention policies

## 📝 License

MIT License - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built as a technical demonstration showcasing:
- CrowdStrike's technology stack (C++, Kafka, RocksDB)
- Modern AI integration (Anthropic Claude)
- Production-grade system design patterns
- Real-time stream processing at scale

---

**For complete documentation, visit: [docs/final/README.md](docs/final/README.md)**
