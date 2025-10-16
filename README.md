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
[![Apache Spark](https://img.shields.io/badge/Apache_Spark-3.5-E25A1C?style=for-the-badge&logo=apache-spark)](https://spark.apache.org/)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python)](https://python.org/)

**Lambda Architecture | Real-Time + Batch Processing | AI-Powered Threat Detection**

[Architecture](docs/final/guides/ARCHITECTURE.md) • [Quick Start](docs/final/guides/QUICK_START.md) • [E2E Testing](docs/END_TO_END_TESTING.md) • [Spark Integration](docs/SPARK_INTEGRATION.md) • [API Docs](docs/final/api/API_REFERENCE.md)

</div>

---

> A high-throughput distributed system implementing **Lambda Architecture** for processing and analyzing security events with both real-time and batch capabilities, featuring AI-powered threat analysis and ML-based anomaly detection.

# StreamGuard: Real-Time Security Event Processing System

**A demonstration project built in 2 weeks to showcase distributed systems expertise for a CrowdStrike job application.**

---

## 📋 About This Project

StreamGuard is a **proof-of-concept demonstration project** built to showcase my ability to quickly master and deliver working software using CrowdStrike's technology stack.

### What It Is ✅

- **Working demonstration** of distributed streaming architecture
- **Technology showcase** featuring C++, Java, Kafka, RocksDB, Prometheus, Grafana
- **AI integration example** with statistical anomaly detection and LLM-powered threat analysis
- **Learning project** completed in 2 weeks to prove rapid skill acquisition
- **Portfolio piece** demonstrating architectural thinking and polyglot development

### What It's NOT ❌

- **NOT production-ready** enterprise software
- **NOT optimized** for millions of events/second (demo processes ~10K events/sec)
- **NOT security-hardened** for real-world deployment
- **NOT feature-complete** - intentionally scoped for job application demo

### Project Goal 🎯

**Primary objective:** Demonstrate my ability to:
1. Quickly master CrowdStrike's core technologies (C++, Kafka, RocksDB)
2. Design and implement distributed systems architecture
3. Integrate modern AI capabilities practically
4. Deliver working, documented software under time constraints
5. Communicate technical decisions clearly

---

## 🏗️ Architecture Overview

StreamGuard implements a streaming event processing pipeline with AI-powered anomaly detection:

```
Event Generator (Java)
    ↓
Apache Kafka (distributed messaging)
    ↓
Stream Processor (C++ for performance)
    ↓
RocksDB (embedded storage)
    ↓
Query API (Java/Spring Boot)
    ↓
AI Analysis (OpenAI GPT-4o-mini)
```

**Key Design Decisions:**
- **C++ for processing**: Performance + RocksDB native integration
- **Java for generator/API**: Rapid development + mature Kafka client
- **Statistical anomaly detection**: Simple, explainable, no training data required
- **AI for narrative generation**: Shows modern capability integration

---

## 🛠️ Technology Stack

### Core Technologies (from CrowdStrike job description)
- **C++17**: High-performance stream processor with RocksDB integration
- **Java 17**: Event generator and REST API (Spring Boot)
- **Apache Kafka**: Distributed event streaming backbone
- **RocksDB**: Embedded key-value storage for state management
- **Docker**: Container orchestration for local development
- **Git**: Version control with clean commit history

### Observability & AI
- **Prometheus**: Metrics collection and monitoring
- **Grafana**: Real-time visualization dashboards
- **OpenAI GPT-4o-mini**: Selective AI-powered threat analysis (opt-in, high-threat events only)
- **Statistical Models**: 5-dimensional anomaly scoring

### Build Tools
- **CMake 3.20+**: Modern C++ build system
- **Maven 3.8+**: Java dependency management
- **Docker Compose**: Multi-container orchestration

---

## 🚀 Quick Start

### Prerequisites

```bash
# Required
- Docker Desktop (for Kafka, monitoring stack)
- Java 17 (for event generator and API)
- CMake 3.20+ (for C++ processor)
- Mac M1 compatible environment

# Verify installation
docker --version
java -version
cmake --version
```

### Setup & Run

```bash
# 1. Clone repository
git clone https://github.com/joselor/streamguard.git
cd streamguard

# 2. Start infrastructure (Kafka, Prometheus, Grafana)
docker-compose up -d

# 3. Build and start components (using Sprint 5 scripts)
./scripts/start-event-generator.sh    # Generates test events
./scripts/start-stream-processor.sh   # Processes events in C++
                                       # Note: Will prompt to enable AI analysis (default: no)
./scripts/start-query-api.sh          # REST API for queries

# 4. Verify system is working
curl "http://localhost:8081/api/events/recent?limit=5" | jq
curl "http://localhost:8081/api/anomalies/recent?limit=2" | jq

# 5. Access monitoring
open http://localhost:3000  # Grafana (admin/admin)
open http://localhost:8090  # Kafka UI
```

For detailed setup instructions, see [docs/final/guides/QUICK_START.md](docs/final/guides/QUICK_START.md)

---

## 📊 Key Features Demonstrated

### Distributed Systems
✅ **Kafka streaming** - Producer/consumer patterns, topic management  
✅ **Stateful processing** - RocksDB for embedded storage  
✅ **Polyglot architecture** - C++ for performance, Java for rapid development

### Anomaly Detection
✅ **Statistical scoring** - 5-dimensional behavioral baseline tracking  
✅ **Real-time detection** - Sub-millisecond anomaly identification  
✅ **Configurable thresholds** - Tunable sensitivity for different scenarios

### AI Integration
✅ **Selective AI analysis** - Opt-in GPT-4o-mini for high-threat/anomalous events only
✅ **Cost-conscious design** - Analyzes only 3-5% of events (threat_score >= 0.7 OR anomaly)
✅ **Interactive startup** - Prompts user to enable AI (default: disabled)
✅ **Contextual insights** - Natural language security explanations
✅ **Graceful degradation** - System works without AI if disabled or unavailable

### Observability
✅ **Prometheus metrics** - Throughput, latency, anomaly rates  
✅ **Grafana dashboards** - Real-time visualization  
✅ **Comprehensive logging** - Structured logging for debugging

### Configuration Management (Sprint 5)
✅ **Single source of truth** - `.env` file for all configuration  
✅ **Automated startup** - Scripts with path validation  
✅ **Zero-config deployment** - Just run and go

---

## 📁 Project Structure

```
streamguard/
├── event-generator/        # Java event producer
│   ├── src/main/java/     # Event generation logic
│   └── pom.xml            # Maven dependencies
├── stream-processor/       # C++ processing engine
│   ├── include/           # Header files
│   ├── src/               # Implementation
│   ├── CMakeLists.txt     # Build configuration
│   └── build/             # Build artifacts
├── query-api/             # Java/Spring Boot REST API
│   └── src/main/java/     # API controllers
├── scripts/               # Automation scripts (Sprint 5)
│   ├── start-event-generator.sh
│   ├── start-stream-processor.sh
│   └── start-query-api.sh
├── docs/                  # Comprehensive documentation
│   ├── final/             # Sprint 3 documentation
│   └── PROJECT_HANDOFF_SPRINT5.md
├── docker-compose.yml     # Infrastructure definition
└── .env.example          # Configuration template
```

---

## 📖 Documentation

### Getting Started
- [Quick Start Guide](docs/final/guides/QUICK_START.md) - 10-minute setup
- [Architecture Overview](docs/final/guides/ARCHITECTURE.md) - System design deep-dive
- [AI/ML Components](docs/final/guides/AI_ML.md) - Anomaly detection explained

### Reference
- [API Reference](docs/final/api/API_REFERENCE.md) - Complete REST API documentation
- [Deployment Guide](docs/final/guides/DEPLOYMENT.md) - Docker, Kubernetes, AWS
- [Troubleshooting](docs/final/guides/TROUBLESHOOTING.md) - Common issues & solutions

### Diagrams
- [Component Diagram](docs/final/diagrams/COMPONENT_DIAGRAM.md) - Architecture visualization
- [Data Flow](docs/final/diagrams/DATA_FLOW_ANIMATION.md) - ByteByGo-style animation
- [Class Diagrams](docs/final/diagrams/CLASS_DIAGRAMS.md) - UML class structure

---

## 🎯 What This Demonstrates

### Technical Skills
✅ **Rapid learning**: Mastered RocksDB, Kafka, and C++ integration in 2 weeks  
✅ **Polyglot development**: Comfortable with C++, Java, and modern tooling  
✅ **System design**: Architectural decisions with clear trade-off analysis  
✅ **Modern practices**: Docker, CI/CD-ready, comprehensive documentation

### Leadership & Communication
✅ **Documentation-first**: 10+ detailed guides with diagrams  
✅ **Decision transparency**: Documented all major design choices  
✅ **User-focused**: Clear setup instructions, troubleshooting guides  
✅ **Growth mindset**: Open about what was learned and what could improve

### Domain Knowledge
✅ **Security concepts**: Understanding of threat patterns and anomaly detection  
✅ **Performance awareness**: Latency optimization, throughput considerations  
✅ **Production thinking**: Monitoring, error handling, graceful degradation

---

## 🔧 Performance Characteristics

**Current Demo Performance:**
- **Throughput**: ~10,000 events/second (single processor instance)
- **Latency**: Sub-5ms P99 end-to-end processing time
- **Anomaly Detection**: <1ms statistical scoring
- **Storage**: Efficient time-series key design in RocksDB

**Production Scaling Considerations:**
- Horizontal scaling via Kafka partitions (multiple processor instances)
- Kubernetes deployment for orchestration
- Cloud-native configuration management
- Comprehensive testing (unit, integration, load)

---

## 🤝 Contributing & Feedback

This is a demonstration project for a job application. However, feedback is welcome!

**Areas for Production Enhancement** (if this were real):
- Comprehensive test suite (unit, integration, load tests)
- Security hardening (authentication, encryption, secrets management)
- Horizontal scaling implementation
- Advanced ML models (beyond statistical scoring)
- Disaster recovery (replication, backups)

---

## 📜 License

This project is for demonstration purposes as part of a job application to CrowdStrike.

All rights reserved - Jose Ortuno, 2025

---

## 👤 Author

**Jose Ortuno** - Senior Solutions Architect  
Applying for: Senior Engineering Manager - Streaming Search at CrowdStrike

**Connect:**
- LinkedIn: [linkedin.com/in/jose-ortuno](https://linkedin.com/in/jose-ortuno)
- GitHub: [github.com/joselor](https://github.com/joselor)
- Email: [your-email@example.com]

---

## 🙏 Acknowledgments

**Technologies Used:**
- Apache Kafka - Distributed streaming platform
- RocksDB - Embedded storage engine
- OpenAI GPT-4o-mini - Selective AI-powered threat analysis
- Spring Boot - Java API framework
- Prometheus & Grafana - Observability stack

**Inspiration:**
- CrowdStrike's approach to security event processing
- Modern streaming architectures (Kafka, Flink, Spark)
- AI-augmented security operations

---

## 📝 Project Timeline

**Sprint 1** (Oct 8-9, 2025): Foundation ✅
- Event generation, Kafka integration, C++ processor, RocksDB storage

**Sprint 2-3** (Oct 10-14, 2025): Features & Monitoring ✅  
- Anomaly detection, AI integration, Prometheus, Grafana, REST API

**Sprint 4** (Oct 14, 2025): Lambda Architecture ✅  
- Batch processing layer, comprehensive state management

**Sprint 5** (Oct 14, 2025): Configuration Management ✅  
- Zero-config deployment, automated startup scripts

**Demo Prep** (Oct 15-16, 2025): Documentation & Demo 📅  
- Video recording, live demo practice, final polish

---

**Last Updated**: October 15, 2025  
**Project Status**: Demo-ready 🚀  
**Next Step**: Record demonstration video

---

> **Note to Reviewers**: This README reflects the honest scope of a 2-week demonstration project. It showcases my ability to quickly deliver working software with unfamiliar technologies, not a claim of production-grade enterprise software. Questions and feedback welcome!