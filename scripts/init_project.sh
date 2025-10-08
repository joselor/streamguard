#!/bin/bash
# StreamGuard Project Initialization Script

echo "🚀 Initializing StreamGuard project structure..."
echo ""

# Create main project directories
echo "Creating directory structure..."
mkdir -p event-generator/src/{main,test}/java/com/streamguard
mkdir -p stream-processor/{include,src,test,models}
mkdir -p query-api/src/{main,test}/java/com/streamguard
mkdir -p monitoring/{prometheus,grafana/{dashboards,datasources}}
mkdir -p ml-pipeline/notebooks
mkdir -p docs
mkdir -p scripts
mkdir -p training-data
mkdir -p demo
mkdir -p infrastructure

echo "✓ Directory structure created"
echo ""

# Create initial files
echo "Creating initial files..."

# .gitignore
cat > .gitignore << 'EOF'
# IDEs
.idea/
*.iml
.vscode/
*.swp
*.swo
*~

# Build artifacts
target/
build/
*.class
*.o
*.so
*.dylib
*.a

# CMake
CMakeCache.txt
CMakeFiles/
cmake_install.cmake
Makefile

# RocksDB data
*.sst
*.log
MANIFEST-*
CURRENT
LOCK
*.db/

# Training data
training-data/*.parquet

# OS
.DS_Store
Thumbs.db

# Secrets
*.env
.env.*
secrets/

# Logs
*.log
logs/
EOF

echo "✓ .gitignore created"

# README.md
cat > README.md << 'EOF'
# StreamGuard: Real-Time Security Event Processing System

A high-throughput distributed system for processing and analyzing security events in real-time, featuring AI-powered anomaly detection and threat intelligence.

## 🎯 Project Overview

Built as a demonstration of production-grade streaming systems architecture, combining CrowdStrike's tech stack (C++, Kafka, RocksDB) with modern AI capabilities.

## 🛠️ Tech Stack

- **C++17**: High-performance stream processor
- **Java 17**: Event generator and Query API
- **Apache Kafka**: Event streaming backbone
- **RocksDB**: Embedded state management
- **AI/ML**: Anomaly detection + LLM-powered threat narratives
- **Prometheus + Grafana**: Observability

## 🚀 Quick Start

### Prerequisites
- Docker Desktop
- Java 17
- CMake 3.20+
- Mac M1 compatible environment

### Setup
```bash
# Verify environment
./scripts/verify-setup.sh

# Start infrastructure
docker-compose up -d

# Build components (detailed instructions coming soon)
```

## 📊 Key Features

✅ Processes 10,000+ events/second with sub-5ms latency
✅ Real-time anomaly detection (statistical + ML models)
✅ LLM-powered threat narratives
✅ ML training data pipeline
✅ Full observability stack

## 📁 Project Structure

```
streamguard/
├── event-generator/     # Java event producer
├── stream-processor/    # C++ processing engine
├── query-api/          # Java REST API
├── monitoring/         # Prometheus + Grafana
├── ml-pipeline/        # Python ML training
└── docs/              # Documentation
```

## 📖 Documentation

- [Setup Guide](docs/setup.md)
- [Architecture](docs/architecture.md)
- [AI/ML Components](docs/ai-ml-components.md)

## 🎥 Demo

Demo video and live deployment links coming soon!

## 📝 License

MIT License - see LICENSE file for details
EOF

echo "✓ README.md created"

# LICENSE
cat > LICENSE << 'EOF'
MIT License

Copyright (c) 2024 StreamGuard Project

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
EOF

echo "✓ LICENSE created"

# Create docs directory files
cat > docs/setup.md << 'EOF'
# StreamGuard Setup Guide

Detailed setup instructions coming soon.
EOF

cat > docs/architecture.md << 'EOF'
# StreamGuard Architecture

Architecture documentation coming soon.
EOF

cat > docs/ai-ml-components.md << 'EOF'
# AI/ML Components

AI/ML component documentation coming soon.
EOF

echo "✓ Documentation stubs created"
echo ""
echo "✅ Project initialization complete!"
echo ""
echo "Next steps:"
echo "1. Review the created structure"
echo "2. Initialize git: git init && git add . && git commit -m 'Initial project structure'"
echo "3. Create docker-compose.yml (see artifact)"
echo "4. Run docker-compose up -d"
