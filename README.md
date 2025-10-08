# StreamGuard: Real-Time Security Event Processing System

A high-throughput distributed system for processing and analyzing security events in real-time, featuring AI-powered anomaly detection and threat intelligence.

## 🎯 Project Overview

Built to demonstrate production-grade streaming systems architecture, combining CrowdStrike's tech stack (C++, Kafka, RocksDB) with modern AI capabilities.

## 🛠️ Tech Stack

- **C++17**: High-performance stream processor
- **Java 21**: Event generator and Query API
- **Apache Kafka**: Event streaming backbone
- **RocksDB**: Embedded state management
- **AI/ML**: Anomaly detection + LLM-powered threat narratives
- **Prometheus + Grafana**: Observability

## 🚀 Quick Start
```bash
# Start infrastructure
./scripts/run-local.sh

# Access points
# Grafana: http://localhost:3000 (admin/admin)
# Kafka UI: http://localhost:8090
# Prometheus: http://localhost:9090

📊 Key Features

✅ Processes 10,000+ events/second with sub-5ms latency
✅ Real-time anomaly detection
✅ LLM-powered threat narratives
✅ Full observability stack

📁 Project Structure
streamguard/
├── event-generator/     # Java event producer
├── stream-processor/    # C++ processing engine
├── query-api/          # Java REST API
├── monitoring/         # Prometheus + Grafana
└── docs/              # Documentation

📝 License
MIT License

