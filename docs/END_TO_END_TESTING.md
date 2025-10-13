# StreamGuard End-to-End Testing Guide

**Complete manual testing procedure for the entire StreamGuard platform**

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Test Environment Setup](#test-environment-setup)
4. [Complete System Test](#complete-system-test)
5. [Individual Component Tests](#individual-component-tests)
6. [Troubleshooting](#troubleshooting)
7. [Test Validation Checklist](#test-validation-checklist)

---

## Overview

This guide provides step-by-step instructions to manually test the complete StreamGuard platform, including:

- **Speed Layer**: Real-time C++ stream processor
- **Batch Layer**: Apache Spark ML pipeline
- **Serving Layer**: Java Query API
- **Infrastructure**: Kafka, Zookeeper, Prometheus, Grafana

**Estimated Time**: 30-45 minutes for complete test

---

## Prerequisites

### Required Software

```bash
# Verify installations
java --version        # Java 17+
mvn --version        # Maven 3.9+
python3 --version    # Python 3.11+
docker --version     # Docker Desktop
cmake --version      # CMake 3.20+
```

### macOS ARM64 Dependencies

```bash
brew install librdkafka rocksdb prometheus-cpp curl nlohmann-json cmake pkg-config
```

### Environment Variables

```bash
# Set these in your shell profile
export CLAUDE_API_KEY="your-api-key"  # For AI analysis (optional)
export JAVA_HOME="/path/to/jdk-17"
```

---

## Test Environment Setup

### Step 1: Clone and Navigate

```bash
cd /path/to/streamguard
git status  # Verify you're on the main branch
```

### Step 2: Start Infrastructure Services

```bash
# Start all infrastructure
docker-compose up -d zookeeper kafka prometheus grafana

# Verify services are running
docker ps

# Expected output:
# streamguard-zookeeper    Up
# streamguard-kafka        Up (healthy)
# streamguard-prometheus   Up
# streamguard-grafana      Up

# Wait for Kafka to be ready (30 seconds)
sleep 30
```

### Step 3: Verify Infrastructure Health

```bash
# Check Kafka is responsive
docker exec streamguard-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check Prometheus
curl -s http://localhost:9090/-/healthy

# Check Grafana
curl -s http://localhost:3000/api/health
```

---

## Complete System Test

### Test 1: Speed Layer (Real-Time Processing)

#### Build C++ Stream Processor

```bash
cd stream-processor
mkdir -p build && cd build

# Configure with CMake
cmake ..

# Build
make stream-processor

# Verify binary exists
ls -lh stream-processor

# Expected: ~500KB executable
```

#### Run Stream Processor

```bash
# Terminal 1: Start stream processor
./stream-processor \
  --broker localhost:9092 \
  --topic security-events \
  --db ./data/events.db \
  --metrics-port 8080

# Expected output:
# [INFO] Kafka consumer initialized
# [INFO] RocksDB opened successfully
# [INFO] Anomaly detector initialized
# [INFO] Prometheus metrics server started on port 8080
# [INFO] Waiting for events...
```

#### Generate Test Events

```bash
# Terminal 2: Generate events
cd ../../event-generator

# Build if needed
mvn clean package -q

# Generate 5000 events over 50 seconds
java -jar target/event-generator-1.0-SNAPSHOT.jar \
  --broker localhost:9092 \
  --topic security-events \
  --rate 100 \
  --duration 50

# Expected output:
# [INFO] Events sent: 979 | Rate: 97.9 events/sec
# [INFO] Events sent: 1978 | Rate: 99.9 events/sec
# [INFO] Total events sent: 5000
# [INFO] Success rate: 100.00%
```

#### Verify Stream Processor Output

```bash
# Terminal 1 should show:
# [INFO] Processed 100 events (avg latency: 0.8ms)
# [INFO] Detected anomaly for user 'bob' (score: 0.82)
# [INFO] AI analysis requested for event evt_xxx

# Check metrics
curl http://localhost:8080/metrics | grep streamguard_events_processed_total

# Expected:
# streamguard_events_processed_total{event_type="auth_attempt"} 1234
# streamguard_events_processed_total{event_type="file_access"} 2345
```

### Test 2: Batch Layer (Spark ML Pipeline)

#### Setup Python Environment

```bash
cd ../../spark-ml-pipeline

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
pip install setuptools  # For Python 3.12+
```

#### Run ML Pipeline

```bash
# Run with max 3000 events
python src/training_data_generator.py --max-events 3000

# Expected output:
# [Step 1/5] Reading events from Kafka
# Read 62330 Kafka records
# Parsed 3000 valid security events
#
# [Step 2/5] Extracting behavioral features
# Total features extracted: 31 columns for 15 users
#
# [Step 3/5] Preparing feature vectors
# Selected 28 numeric features for ML
#
# [Step 4/5] Detecting anomalies with ML
# Training Isolation Forest with contamination=0.1
# Detected 2 anomalies (13.33% of users)
#
# [Step 5/5] Exporting training data to Parquet
# Training data exported to: ./output/training_data
#
# Pipeline Complete!
# Total duration: 20.32 seconds
```

#### Verify Spark Output

```bash
# Check output files
ls -lh output/training_data/

# Expected:
# _SUCCESS
# anomaly_report.json
# is_anomaly=0/  (normal users)
# is_anomaly=1/  (anomalous users)

# View anomaly report
cat output/training_data/anomaly_report.json | python3 -m json.tool

# Expected JSON with:
# {
#   "total_users": 15,
#   "anomalous_users": 2,
#   "anomaly_rate": 0.133,
#   "top_anomalies": [...]
# }
```

#### Validate Parquet Data

```bash
# Read with pandas
python3 << 'EOF'
import pandas as pd

# Read partitioned data
df_anomalous = pd.read_parquet('output/training_data/is_anomaly=1/')
df_normal = pd.read_parquet('output/training_data/is_anomaly=0/')

print(f"Anomalous users: {len(df_anomalous)}")
print(f"Normal users: {len(df_normal)}")
print(f"Total features: {len(df_normal.columns)}")
print("\nTop anomalies:")
print(df_anomalous[['user', 'anomaly_score_normalized', 'total_events']])
EOF

# Expected:
# Anomalous users: 2
# Normal users: 13
# Total features: 31
```

### Test 3: Serving Layer (Query API)

#### Build Query API

```bash
cd ../query-api

# Build with Maven
mvn clean package -DskipTests

# Verify JAR
ls -lh target/query-api-*.jar

# Expected: ~40MB JAR file
```

#### Run Query API

```bash
# Terminal 3: Start Query API
ROCKSDB_PATH=../stream-processor/build/data/events.db \
java -jar target/query-api-1.0.0.jar

# Expected output:
# INFO  c.s.q.QueryApiApplication - Starting QueryApiApplication
# INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8081
# INFO  c.s.q.QueryApiApplication - Started QueryApiApplication
```

#### Test API Endpoints

```bash
# Terminal 4: Test endpoints

# 1. Health check
curl http://localhost:8081/actuator/health

# Expected: {"status":"UP"}

# 2. Get latest events
curl http://localhost:8081/api/events?limit=5 | python3 -m json.tool

# Expected: Array of 5 events with full details

# 3. Get anomalies
curl http://localhost:8081/api/anomalies?limit=10 | python3 -m json.tool

# Expected: Array of anomaly records

# 4. Get statistics
curl http://localhost:8081/api/stats | python3 -m json.tool

# Expected:
# {
#   "totalEvents": 5000,
#   "uniqueUsers": 15,
#   "avgThreatScore": 0.35,
#   "anomalyCount": 150
# }

# 5. Access Swagger UI
open http://localhost:8081/swagger-ui.html
# Should open interactive API documentation
```

### Test 4: Monitoring Stack

#### Verify Prometheus

```bash
# Check Prometheus targets
open http://localhost:9090/targets

# Should show:
# - stream-processor (UP) - localhost:8080
# - query-api (UP) - localhost:8081

# Query metrics
curl 'http://localhost:9090/api/v1/query?query=streamguard_events_processed_total'

# Expected: JSON with metric values
```

#### Verify Grafana

```bash
# Open Grafana
open http://localhost:3000

# Login: admin / admin

# Navigate to dashboards:
# - StreamGuard Real-Time Processing
# - StreamGuard Anomaly Detection
# - StreamGuard System Health

# Verify data is flowing (should see charts with data)
```

---

## Individual Component Tests

### Component Test: Event Generator

```bash
cd event-generator

# Test 1: Generate 100 events
java -jar target/event-generator-1.0-SNAPSHOT.jar \
  --broker localhost:9092 \
  --topic security-events \
  --rate 10 \
  --duration 10

# Expected: 100 events generated

# Test 2: Verify event structure
docker exec streamguard-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic security-events \
  --from-beginning \
  --max-messages 1

# Expected: Valid JSON event with all fields
```

### Component Test: Stream Processor

```bash
cd stream-processor/build

# Test 1: Basic functionality
./stream-processor --help

# Expected: Usage information

# Test 2: Configuration validation
./stream-processor \
  --broker localhost:9092 \
  --topic security-events \
  --db /tmp/test.db \
  --metrics-port 8080 &

PROC_PID=$!
sleep 5

# Verify metrics endpoint
curl http://localhost:8080/metrics | grep streamguard

# Expected: Prometheus metrics

# Cleanup
kill $PROC_PID
rm -rf /tmp/test.db
```

### Component Test: Spark Pipeline

```bash
cd spark-ml-pipeline

# Test 1: Configuration validation
python src/training_data_generator.py --help

# Expected: Usage information

# Test 2: Feature extraction
python << 'EOF'
from kafka_reader import KafkaEventReader
from feature_extractor import FeatureExtractor
from pyspark.sql import SparkSession

spark = SparkSession.builder.master("local[1]").getOrCreate()
reader = KafkaEventReader(spark)
extractor = FeatureExtractor(spark)

# Test with small dataset
df = reader.read_batch(max_events=100)
features = extractor.extract_user_features(df)

print(f"✅ Extracted features for {features.count()} users")
spark.stop()
EOF

# Expected: "✅ Extracted features for N users"
```

### Component Test: Query API

```bash
cd query-api

# Test 1: Unit tests
mvn test

# Expected: All tests pass

# Test 2: Integration test
mvn verify

# Expected: Build success
```

---

## Troubleshooting

### Issue: Kafka not starting

```bash
# Check logs
docker logs streamguard-kafka

# Common fix: Remove old data
docker-compose down -v
docker-compose up -d kafka

# Wait 30 seconds for startup
sleep 30
```

### Issue: Stream processor crashes

```bash
# Check dependencies
brew list | grep -E "librdkafka|rocksdb|prometheus"

# Reinstall if missing
brew reinstall librdkafka rocksdb prometheus-cpp

# Rebuild
cd stream-processor/build
rm -rf *
cmake ..
make
```

### Issue: Spark pipeline fails

```bash
# Check Python version
python3 --version  # Must be 3.11+

# Reinstall dependencies
pip install --force-reinstall -r requirements.txt
pip install setuptools

# Check Kafka connectivity
python3 -c "from kafka import KafkaConsumer; KafkaConsumer(bootstrap_servers='localhost:9092')"
```

### Issue: Query API can't read RocksDB

```bash
# Verify RocksDB path exists
ls -lh ../stream-processor/build/data/events.db

# Check permissions
chmod -R 755 ../stream-processor/build/data/

# Set correct path
export ROCKSDB_PATH=$(pwd)/../stream-processor/build/data/events.db
java -jar target/query-api-1.0.0.jar
```

### Issue: No data in Grafana

```bash
# Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets | python3 -m json.tool

# Restart Prometheus
docker restart streamguard-prometheus

# Re-import dashboards
# Grafana UI → Dashboards → Import → monitoring/grafana/dashboards/*.json
```

---

## Test Validation Checklist

### Infrastructure ✅

- [ ] Zookeeper running
- [ ] Kafka healthy
- [ ] Prometheus collecting metrics
- [ ] Grafana accessible

### Speed Layer ✅

- [ ] Stream processor binary builds
- [ ] Consumes events from Kafka
- [ ] Detects anomalies
- [ ] Stores events in RocksDB
- [ ] Exports Prometheus metrics
- [ ] Requests AI analysis for high-threat events

### Batch Layer ✅

- [ ] Python environment setup
- [ ] Reads events from Kafka
- [ ] Extracts 28+ features
- [ ] ML anomaly detection works
- [ ] Exports Parquet files
- [ ] Generates anomaly report

### Serving Layer ✅

- [ ] Query API builds
- [ ] Reads from RocksDB
- [ ] All REST endpoints respond
- [ ] Swagger UI accessible
- [ ] Returns valid JSON

### Monitoring ✅

- [ ] Prometheus targets UP
- [ ] Metrics visible in Prometheus UI
- [ ] Grafana dashboards load
- [ ] Data flowing to dashboards

### End-to-End ✅

- [ ] Events flow: Generator → Kafka → Processor → RocksDB
- [ ] Batch processing: Kafka → Spark → Parquet
- [ ] Queries work: API → RocksDB → JSON
- [ ] Monitoring: All components → Prometheus → Grafana

---

## Performance Benchmarks

Expected performance for a complete test run:

| Component | Metric | Expected Value |
|-----------|--------|----------------|
| **Event Generator** | Rate | 100 events/sec |
| **Stream Processor** | Latency | <1ms per event |
| **Stream Processor** | Throughput | 10K+ events/sec |
| **Spark Pipeline** | Duration (3K events) | 20-30 seconds |
| **Spark Pipeline** | Features extracted | 28+ numeric features |
| **Query API** | Response time | <100ms (P95) |
| **RocksDB** | Read latency | <1ms |

---

## Test Report Template

Use this template to document your test results:

```
# StreamGuard E2E Test Report

**Date**: [DATE]
**Tester**: [NAME]
**Environment**: [macOS ARM64 / Linux x64 / etc]

## Test Summary
- Infrastructure: [PASS/FAIL]
- Speed Layer: [PASS/FAIL]
- Batch Layer: [PASS/FAIL]
- Serving Layer: [PASS/FAIL]
- Monitoring: [PASS/FAIL]

## Metrics
- Events Generated: [NUMBER]
- Events Processed: [NUMBER]
- Anomalies Detected: [NUMBER]
- Pipeline Duration: [SECONDS]
- API Response Time: [MS]

## Issues Encountered
[List any issues and resolutions]

## Notes
[Additional observations]
```

---

## Quick Test Script

For rapid validation, use this condensed test:

```bash
#!/bin/bash
# quick-test.sh - 5-minute StreamGuard validation

set -e

echo "🚀 StreamGuard Quick Test"

# 1. Start infrastructure
docker-compose up -d kafka
sleep 30

# 2. Generate 1000 events
cd event-generator
java -jar target/*.jar --rate 100 --duration 10 --broker localhost:9092 --topic security-events &

# 3. Run Spark pipeline
cd ../spark-ml-pipeline
source venv/bin/activate
python src/training_data_generator.py --max-events 1000

# 4. Verify output
if [ -f "output/training_data/_SUCCESS" ]; then
    echo "✅ Test PASSED"
    cat output/training_data/anomaly_report.json
else
    echo "❌ Test FAILED"
    exit 1
fi
```

---

## Next Steps

After successful testing:

1. **Production Deployment**: See `docs/final/guides/DEPLOYMENT.md`
2. **Performance Tuning**: See `docs/SPARK_INTEGRATION.md`
3. **Monitoring Setup**: Configure Grafana alerts
4. **CI/CD**: Set up automated testing pipeline

---

**Document Version**: 1.0
**Last Updated**: October 12, 2025
**Author**: Jose Ortuno

**Questions?** See `docs/final/guides/TROUBLESHOOTING.md`