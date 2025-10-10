# StreamGuard Setup Guide

**Version:** 2.0
**Last Updated:** October 9, 2025
**Platform:** macOS (M1/ARM64), adaptable to Linux/Windows

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Building Components](#building-components)
5. [Running the Pipeline](#running-the-pipeline)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)
8. [Development Workflow](#development-workflow)

---

## Quick Start

**Get up and running in 5 minutes:**

```bash
# 1. Clone repository
git clone https://github.com/joselor/streamguard.git
cd streamguard

# 2. Start infrastructure
docker-compose up -d

# 3. Build components
cd event-generator && mvn clean package && cd ..
cd stream-processor && mkdir -p build && cd build && cmake .. && make && cd ../..

# 4. Run end-to-end test
./test-e2e.sh

# 5. View results
cat test-data/e2e-test-report.md
```

---

## Prerequisites

### Required Software

| Software | Version | macOS Installation | Purpose |
|----------|---------|-------------------|---------|
| **Docker Desktop** | 24+ | [Download](https://www.docker.com/products/docker-desktop) | Kafka infrastructure |
| **Java JDK** | 17 LTS | `brew install openjdk@17` | Event generator |
| **Maven** | 3.9+ | `brew install maven` | Java build tool |
| **CMake** | 3.20+ | `brew install cmake` | C++ build system |
| **C++ Compiler** | Clang 14+ | Xcode Command Line Tools | C++ compilation |
| **Git** | 2.30+ | `brew install git` | Version control |

### Required Libraries (macOS/Homebrew)

```bash
# Install all dependencies
brew install nlohmann-json rocksdb librdkafka
```

**Library Details:**
- **nlohmann-json** (3.11.3+): C++ JSON parsing
- **RocksDB** (8.9.1+): Embedded key-value storage
- **librdkafka** (2.3.0+): C++ Kafka client

### System Requirements

**Minimum**:
- **CPU**: 2 cores (4 recommended)
- **RAM**: 8GB (16GB recommended)
- **Disk**: 20GB free space
- **OS**: macOS 12+ (Monterey), Linux (Ubuntu 20.04+), Windows 10+ (WSL2)

**For Production**:
- **CPU**: 8+ cores
- **RAM**: 32GB+
- **Disk**: 500GB+ SSD
- **Network**: 1Gbps+

---

## Installation

### Step 1: Install Homebrew (macOS)

```bash
# Install Homebrew if not present
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Verify installation
brew --version
```

### Step 2: Install Java 17

```bash
# Install OpenJDK 17
brew install openjdk@17

# Link Java (add to ~/.zshrc or ~/.bash_profile)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java -version  # Should show version 17.x.x
```

### Step 3: Install Development Tools

```bash
# Install build tools
brew install cmake maven git

# Install C++ libraries
brew install nlohmann-json rocksdb librdkafka

# Verify installations
cmake --version    # Should be 3.20+
mvn --version      # Should be 3.9+
```

### Step 4: Install Docker Desktop

1. Download from [docker.com](https://www.docker.com/products/docker-desktop)
2. Install and start Docker Desktop
3. Verify:
```bash
docker --version         # Should be 24+
docker-compose --version # Should be 2.23+
```

4. Configure Docker (optional):
   - **Memory**: Allocate 4GB+ (Preferences → Resources)
   - **CPUs**: Allocate 2+ cores
   - **Disk**: 20GB+ image size

### Step 5: Clone Repository

```bash
# Clone the repository
git clone https://github.com/joselor/streamguard.git
cd streamguard

# Verify project structure
ls -la
# Should see: docker-compose.yml, event-generator/, stream-processor/, etc.
```

---

## Building Components

### Build Java Event Generator

```bash
cd event-generator

# Clean and build
mvn clean package

# Verify JAR created
ls -lh target/event-generator-1.0-SNAPSHOT.jar

# Run tests
mvn test

# Expected output: All tests passing ✅
```

**Troubleshooting Maven**:
- **Issue**: `JAVA_HOME not set`
  - **Fix**: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- **Issue**: `dependency not found`
  - **Fix**: `mvn clean install -U` (force update)

### Build C++ Stream Processor

```bash
cd stream-processor

# Create build directory
mkdir -p build
cd build

# Configure with CMake
cmake ..

# Build (use all CPU cores)
make -j$(sysctl -n hw.ncpu)

# Verify binary created
ls -lh stream-processor

# Run tests (if Google Test is installed)
./event-tests
```

**CMake Configuration Output**:
```
-- StreamGuard Stream Processor Configuration:
--   C++ Standard: 17
--   Build Type: Release
--   Architecture: arm64
--   Compiler: AppleClang 15.0
--   nlohmann_json: TRUE
--   librdkafka: /opt/homebrew/lib/librdkafka++.dylib
--   RocksDB: TRUE
```

**Troubleshooting CMake**:
- **Issue**: `nlohmann_json not found`
  - **Fix**: `brew install nlohmann-json`
- **Issue**: `RocksDB not found`
  - **Fix**: `brew install rocksdb`
- **Issue**: `librdkafka not found`
  - **Fix**: `brew install librdkafka`

---

## Running the Pipeline

### Step 1: Start Infrastructure

```bash
# Start all Docker services
docker-compose up -d

# Verify services are running
docker-compose ps

# Expected output:
# streamguard-zookeeper    Up      2181/tcp
# streamguard-kafka        Up      9092/tcp
# streamguard-prometheus   Up      9090/tcp
# streamguard-grafana      Up      3000/tcp
```

**Wait for Kafka to be ready** (30 seconds):
```bash
# Check Kafka logs
docker logs streamguard-kafka | grep "started (kafka.server.KafkaServer)"

# Or use this wait script
timeout 60 bash -c 'until docker exec streamguard-kafka kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; do sleep 1; done'
```

### Step 2: Run Event Generator

```bash
# From project root
cd event-generator

# Run with default settings (100 events/sec, unlimited duration)
java -jar target/event-generator-1.0-SNAPSHOT.jar

# Run with custom settings
java -jar target/event-generator-1.0-SNAPSHOT.jar \
  --broker localhost:9092 \
  --topic security-events \
  --rate 1000 \
  --duration 60

# Monitor output
# [INFO] Events sent: 978 | Rate: 97.8 events/sec | Errors: 0
```

**Generator Options**:
- `--broker <addr>`: Kafka bootstrap servers (default: localhost:9092)
- `--topic <name>`: Kafka topic (default: security-events)
- `--rate <num>`: Events per second (default: 100)
- `--duration <sec>`: Run duration in seconds (default: 0 = unlimited)

### Step 3: Run Stream Processor

```bash
# From project root
cd stream-processor/build

# Run with default settings
./stream-processor

# Run with custom settings
./stream-processor \
  --broker localhost:9092 \
  --topic security-events \
  --group streamguard-processor \
  --db ./data/events.db

# Monitor output
# [Processor] Stored event: id=evt_abc123, type=auth_attempt, user=alice
```

**Processor Options**:
- `--broker <addr>`: Kafka bootstrap servers (default: localhost:9092)
- `--topic <name>`: Kafka topic (default: security-events)
- `--group <id>`: Consumer group ID (default: streamguard-processor)
- `--db <path>`: RocksDB database path (default: ./data/events.db)

### Step 4: Monitor the Pipeline

**View Kafka Topics**:
```bash
# List topics
docker exec streamguard-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# Describe security-events topic
docker exec streamguard-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic security-events
```

**Consume Events (Console)**:
```bash
# Read all events from beginning
docker exec streamguard-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic security-events \
  --from-beginning \
  --max-messages 10
```

**View Prometheus Metrics**:
```bash
# Open Prometheus UI
open http://localhost:9090

# Query examples:
# - kafka_server_brokertopicmetrics_messagesinpersec
# - process_cpu_seconds_total
```

**View Grafana Dashboards**:
```bash
# Open Grafana
open http://localhost:3000

# Login: admin / admin
# Add Prometheus datasource: http://prometheus:9090
```

---

## Testing

### Unit Tests

**Java Tests**:
```bash
cd event-generator
mvn test

# Run specific test
mvn test -Dtest=EventSerializationTest

# With coverage (requires jacoco plugin)
mvn clean test jacoco:report
```

**C++ Tests**:
```bash
cd stream-processor/build

# Run all tests
./event-tests

# Run with verbose output
./event-tests --gtest_verbose

# Run specific test
./event-tests --gtest_filter=EventSerializationTest.*
```

### Integration Tests

**Manual Integration Test**:
```bash
# 1. Start infrastructure
docker-compose up -d && sleep 30

# 2. Create topic
docker exec streamguard-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic security-events \
  --partitions 4 --replication-factor 1

# 3. Start processor (in terminal 1)
cd stream-processor/build
./stream-processor --db /tmp/test.db

# 4. Generate events (in terminal 2)
cd event-generator
java -jar target/event-generator-1.0-SNAPSHOT.jar --duration 10

# 5. Verify events stored
ls -lh /tmp/test.db/
```

### End-to-End Test

**Automated E2E Test**:
```bash
# From project root
./test-e2e.sh

# View detailed report
cat test-data/e2e-test-report.md

# Check logs
cat test-data/generator.log
cat test-data/processor.log
```

**Expected Output**:
```
========================================
  StreamGuard End-to-End Pipeline Test
========================================

[Step 1/8] Checking Docker infrastructure...
✓ Kafka is running
✓ Kafka is accessible

[Step 2/8] Building components...
✓ Event generator already built
✓ Stream processor already built

...

========================================
  ✅ End-to-End Test PASSED
========================================

📄 View detailed report: ./test-data/e2e-test-report.md
```

### Load Testing (Future)

**Benchmark Script** (planned for Sprint 2):
```bash
# Generate high-volume events
java -jar event-generator.jar --rate 10000 --duration 300

# Monitor processor performance
htop  # CPU/memory usage
iostat 1  # Disk I/O
```

---

## Troubleshooting

### Common Issues

#### 1. Kafka Not Starting

**Symptoms**: `docker-compose up` fails, Kafka container exits

**Diagnosis**:
```bash
# Check logs
docker logs streamguard-kafka

# Common errors:
# - Port 9092 already in use
# - Insufficient memory
# - Zookeeper not reachable
```

**Solutions**:
```bash
# Solution 1: Kill processes on port 9092
lsof -ti:9092 | xargs kill -9

# Solution 2: Increase Docker memory
# Docker Desktop → Preferences → Resources → Memory: 4GB+

# Solution 3: Restart Zookeeper
docker-compose restart zookeeper
sleep 10
docker-compose restart kafka
```

#### 2. Maven Build Failures

**Symptoms**: `mvn package` fails with dependency errors

**Diagnosis**:
```bash
# Check Java version
java -version  # Must be 17.x

# Check Maven settings
mvn --version
cat ~/.m2/settings.xml
```

**Solutions**:
```bash
# Solution 1: Update dependencies
mvn clean install -U

# Solution 2: Clear local repository
rm -rf ~/.m2/repository
mvn clean package

# Solution 3: Use specific Java version
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
mvn clean package
```

#### 3. CMake Configuration Fails

**Symptoms**: `cmake ..` fails with "library not found"

**Diagnosis**:
```bash
# Check installed libraries
brew list | grep -E '(json|rocksdb|rdkafka)'

# Check library paths
ls /opt/homebrew/lib/lib*.dylib
```

**Solutions**:
```bash
# Solution 1: Reinstall libraries
brew reinstall nlohmann-json rocksdb librdkafka

# Solution 2: Explicit paths
cmake .. \
  -DCMAKE_PREFIX_PATH=/opt/homebrew \
  -DROCKSDB_ROOT=/opt/homebrew

# Solution 3: Clean build
rm -rf build
mkdir build && cd build
cmake ..
```

#### 4. Stream Processor Can't Connect to Kafka

**Symptoms**: Consumer fails with "Broker transport failure"

**Diagnosis**:
```bash
# Check Kafka is accessible
nc -zv localhost 9092

# Check consumer group
docker exec streamguard-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

**Solutions**:
```bash
# Solution 1: Verify Kafka is ready
docker exec streamguard-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# Solution 2: Use correct broker address
./stream-processor --broker localhost:9092

# Solution 3: Reset consumer group (if stuck)
docker exec streamguard-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group streamguard-processor \
  --reset-offsets --to-earliest \
  --topic security-events --execute
```

#### 5. RocksDB Write Failures

**Symptoms**: "Failed to put event" in processor logs

**Diagnosis**:
```bash
# Check disk space
df -h

# Check permissions
ls -la ./data/

# Check RocksDB files
ls -la ./data/events.db/
```

**Solutions**:
```bash
# Solution 1: Ensure directory exists
mkdir -p ./data

# Solution 2: Fix permissions
chmod 755 ./data

# Solution 3: Clean corrupted DB
rm -rf ./data/events.db
./stream-processor
```

### Performance Issues

#### Slow Event Processing

**Diagnosis**:
```bash
# Check CPU usage
top -pid $(pgrep stream-processor)

# Check memory
ps aux | grep stream-processor

# Check disk I/O
iostat -x 1 10
```

**Optimizations**:
1. **Increase Kafka batch size**: Edit producer config
2. **Add more partitions**: Scale topic partitioning
3. **Tune RocksDB**: Adjust write buffer size
4. **Enable compression**: Use Snappy for events

### Debugging Tips

**Enable Debug Logging (Java)**:
```bash
# Add to pom.xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
</dependency>

# Run with debug
java -Dlogback.configurationFile=logback.xml -jar event-generator.jar
```

**Enable Debug Logging (C++)**:
```cpp
// In main.cpp
#define DEBUG 1
// Add verbose logging
std::cout << "[DEBUG] Event: " << event.toJson() << std::endl;
```

**Profile C++ Performance**:
```bash
# macOS Instruments
instruments -t "Time Profiler" ./stream-processor

# Linux perf
perf record ./stream-processor
perf report
```

---

## Development Workflow

### Daily Development

```bash
# 1. Pull latest changes
git pull origin main

# 2. Start infrastructure (if not running)
docker-compose up -d

# 3. Make code changes
# ... edit files ...

# 4. Build changed component
cd event-generator && mvn package && cd ..
# OR
cd stream-processor/build && make && cd ../..

# 5. Test changes
mvn test  # Java
./event-tests  # C++

# 6. Run integration test
./test-e2e.sh

# 7. Commit changes
git add .
git commit -m "feat: Description of changes"
git push origin main
```

### Code Style

**Java**:
- Follow Google Java Style Guide
- Use `mvn spotless:apply` for formatting
- Max line length: 100 characters

**C++**:
- Follow Google C++ Style Guide
- Use `clang-format -i *.cpp *.h`
- Max line length: 100 characters

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/US-XXX-description

# Make changes and commit
git add .
git commit -m "feat: US-XXX - Description"

# Push branch
git push origin feature/US-XXX-description

# Create pull request on GitHub
gh pr create --title "US-XXX: Description" --body "..."

# Merge after review
gh pr merge --squash
```

### Running Specific Scenarios

**High-Rate Testing**:
```bash
# Generate 10K events/sec for 60 seconds
java -jar event-generator.jar --rate 10000 --duration 60

# Monitor processor
htop  # Watch CPU usage
```

**Failure Testing**:
```bash
# Stop Kafka mid-processing
docker stop streamguard-kafka

# Resume
docker start streamguard-kafka

# Verify recovery
./stream-processor  # Should resume from last offset
```

**Query Testing** (when EventStore CLI is added):
```bash
# Query last 100 auth events
./query-tool --type auth_attempt --limit 100

# Query time range
./query-tool --type network_connection \
  --start "2025-10-09 00:00:00" \
  --end "2025-10-09 23:59:59"
```

---

## Next Steps

**After Setup**:
1. ✅ Verify all components build successfully
2. ✅ Run end-to-end test (`./test-e2e.sh`)
3. ✅ Explore Kafka UI at http://localhost:8090
4. ✅ Check Prometheus metrics at http://localhost:9090
5. ✅ View Grafana dashboards at http://localhost:3000

**Start Development**:
- Review [Architecture Documentation](architecture.md)
- Read [Project Handoff](project_handoff.md)
- Check current sprint tasks on GitHub Issues
- Join the team Slack/Discord for questions

**Contribute**:
- Pick an issue from the backlog
- Create feature branch
- Follow development workflow above
- Submit pull request for review

---

## Resources

**Documentation**:
- [Architecture Guide](architecture.md)
- [Project Handoff](project_handoff.md)
- [Event Schema](event-schema-documentation.md)

**External Resources**:
- [Apache Kafka Docs](https://kafka.apache.org/documentation/)
- [RocksDB Wiki](https://github.com/facebook/rocksdb/wiki)
- [librdkafka Docs](https://github.com/confluentinc/librdkafka)
- [nlohmann/json Docs](https://json.nlohmann.me/)

**Tools**:
- [Kafka UI](http://localhost:8090) - Topic management
- [Prometheus](http://localhost:9090) - Metrics
- [Grafana](http://localhost:3000) - Dashboards

---

**Last Updated**: October 9, 2025 - Sprint 1 Complete
**Author**: Jose Ortuno

For questions or issues, create a GitHub issue or contact Jose Ortuno.
