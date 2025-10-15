# StreamGuard Quick Start Guide

Get StreamGuard up and running in under 10 minutes.

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Docker | 20.10+ | Container runtime |
| Docker Compose | 2.0+ | Multi-container orchestration |
| CMake | 3.20+ | C++ build system |
| GCC/Clang | C++17 support | C++ compiler |
| Java | 17+ | Query API runtime |
| Maven | 3.8+ | Java build tool |
| Python | 3.11+ | Spark ML pipeline (optional) |

### Optional Tools

- `curl` or `httpie` for API testing
- `jq` for JSON processing
- Prometheus & Grafana for monitoring
- Apache Spark for ML pipeline (Python 3.11+ includes PySpark)

### System Requirements

- **CPU**: 4+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 20GB free space
- **OS**: Linux, macOS, or Windows (WSL2)

---

## Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/streamguard.git
cd streamguard
```

---

## Step 2: Configure Environment

Copy the environment template and configure:

```bash
cp .env.example .env
```

Edit `.env` to set your configuration:

```bash
# StreamGuard Environment Configuration

# RocksDB Database Path (both components use this path)
ROCKSDB_PATH=./data/events.db

# Kafka Configuration
KAFKA_BROKER=localhost:9092
KAFKA_TOPIC=security-events
KAFKA_GROUP_ID=streamguard-processor

# OpenAI API Configuration
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4o-mini

# Metrics & Monitoring
STREAM_PROCESSOR_METRICS_PORT=8080
QUERY_API_PORT=8081
```

**Important:** Set `OPENAI_API_KEY` to your actual OpenAI API key.

---

## Step 3: Start Infrastructure

Start Kafka and Zookeeper using Docker Compose:

```bash
docker-compose up -d
```

Verify services are running:

```bash
docker-compose ps
```

Expected output:
```
NAME                       IMAGE                         STATUS
streamguard-kafka          confluentinc/cp-kafka:7.5.0   Up
streamguard-zookeeper      confluentinc/cp-zookeeper     Up
streamguard-prometheus     prom/prometheus               Up
streamguard-grafana        grafana/grafana               Up
```

Wait 30 seconds for Kafka to be fully ready:

```bash
sleep 30
```

---

## Step 3: Build Stream Processor

```bash
cd stream-processor

# Create build directory
mkdir -p build
cd build

# Configure with CMake
cmake ..

# Build (use -j for parallel compilation)
make -j$(nproc)

# Verify build
./stream-processor --version
```

Expected output:
```
StreamGuard Stream Processor v1.0.0
```

---

## Step 4: Build Query API

```bash
cd ../../query-api

# Clean and package
mvn clean package -DskipTests

# Verify JAR file
ls -lh target/query-api-1.0.0.jar
```

---

## Step 5: Build Event Generator

```bash
cd ../event-generator

# Clean and package
mvn clean package -DskipTests

# Verify JAR file
ls -lh target/event-generator-1.0-SNAPSHOT.jar
```

Expected output:
```
-rw-r--r-- 1 user staff 16M Oct 14 15:02 target/event-generator-1.0-SNAPSHOT.jar
```

---

## Step 6: Configure OpenAI API Key

**Option A: Environment Variable**

```bash
export OPENAI_API_KEY="sk-..."
```

**Option B: Configuration File**

Create `stream-processor/build/config.json`:

```json
{
  "openai_api_key": "sk-...",
  "model": "gpt-4"
}
```

---

## Step 7: Start Stream Processor

```bash
cd stream-processor/build

# Create data directory
mkdir -p data

# Start processor
./stream-processor \
  --broker localhost:9092 \
  --topic security-events \
  --group streamguard-processor \
  --db ./data/events.db \
  --metrics-port 8080
```

Expected output:
```
[Main] StreamGuard starting...
[Main] Kafka broker: localhost:9092
[Main] Topic: security-events
[Main] Consumer group: streamguard-processor
[Main] Opening RocksDB at: ./data/events.db
[Main] Column families: default, ai_analysis, embeddings, anomalies
[Main] Metrics server started on port 8080
[Main] Connecting to Kafka...
[Main] Subscribed to topic: security-events
[Main] Ready to process events
```

---

## Step 8: Start Query API

Open a new terminal:

```bash
cd query-api

# Set RocksDB path
export ROCKSDB_PATH=/path/to/stream-processor/build/data/events.db

# Start API server
java -jar target/query-api-1.0.0.jar
```

Expected output:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

[RocksDBConfig] Opened RocksDB in read-only mode
[RocksDBConfig] Found anomalies column family
[QueryApiApplication] Started QueryApiApplication in 2.5 seconds
```

API available at: http://localhost:8081

---

## Step 9: Generate Test Events

Open a new terminal and start the event generator:

### Option A: Using Startup Script (Recommended)

```bash
# Use default rate (100 events/sec, unlimited duration)
./scripts/start-event-generator.sh

# Or with custom configuration
EVENT_RATE=1000 EVENT_DURATION=60 ./scripts/start-event-generator.sh
```

Expected output:
```
[Startup] Loading configuration from .env...
[Startup] Configuration:
  - Kafka broker: localhost:9092
  - Topic: security-events
  - Event rate: 100 events/sec
  - Duration: unlimited (Ctrl+C to stop)
[Startup] Starting event generator...

=== StreamGuard Event Generator Starting ===
Target rate: 100 events/second
Kafka topic: security-events
Duration: 0 seconds (0 = unlimited)
===========================================

Events sent: 1000 | Rate: 100.2 events/sec | Errors: 0
Events sent: 2000 | Rate: 99.8 events/sec | Errors: 0
...
```

### Option B: Manual Start

```bash
cd event-generator

# Build if not already built
mvn clean package

# Run with default settings (100 events/sec)
java -jar target/event-generator-1.0-SNAPSHOT.jar

# Or with custom rate and duration
java -jar target/event-generator-1.0-SNAPSHOT.jar \
  --rate 1000 \
  --duration 60 \
  --broker localhost:9092 \
  --topic security-events
```

**Event Generator Options:**
```
--rate <num>       Events per second (default: 100, max: 50000)
--broker <addr>    Kafka bootstrap servers (default: localhost:9092)
--topic <name>     Kafka topic (default: security-events)
--duration <sec>   Run duration in seconds (default: unlimited)
--help, -h         Show help message
```

---

## Step 10: Query Events

Query the API using curl:

```bash
# Get latest events
curl http://localhost:8081/api/events?limit=10 | jq

# Get anomalies
curl http://localhost:8081/api/anomalies?limit=10 | jq

# Get AI analyses
curl http://localhost:8081/api/analyses?limit=10 | jq

# Get statistics
curl http://localhost:8081/api/stats/summary | jq
```

---

## Step 11: View Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/metrics

# Filter for anomaly metrics
curl -s http://localhost:8080/metrics | grep anomaly
```

---

## Step 12: Run Spark ML Pipeline (Optional)

The Spark ML pipeline provides batch processing for deep feature engineering and ML-based anomaly detection.

### Setup Python Environment

```bash
cd ../spark-ml-pipeline

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install setuptools  # Required for Python 3.12+
```

### Run ML Pipeline

```bash
# Process events with default settings (up to 10K events)
python src/training_data_generator.py

# Or process specific number of events
python src/training_data_generator.py --max-events 5000

# Or process from specific Kafka offset
python src/training_data_generator.py --start-offset earliest --max-events 10000
```

Expected output:
```
[Step 1/5] Reading events from Kafka
Read 62330 Kafka records
Parsed 5000 valid security events

[Step 2/5] Extracting behavioral features
Total features extracted: 31 columns for 15 users

[Step 3/5] Preparing feature vectors
Selected 28 numeric features for ML

[Step 4/5] Detecting anomalies with ML
Training Isolation Forest with contamination=0.1
Detected 2 anomalies (13.33% of users)

[Step 5/5] Exporting training data to Parquet
Training data exported to: ./output/training_data

Pipeline Complete!
Total duration: 25.45 seconds
```

### Verify ML Results

```bash
# Check output files
ls -lh output/training_data/

# View anomaly report
cat output/training_data/anomaly_report.json | jq

# Read Parquet data with Python
python3 << 'EOF'
import pandas as pd

# Read partitioned data
df_anomalous = pd.read_parquet('output/training_data/is_anomaly=1/')
df_normal = pd.read_parquet('output/training_data/is_anomaly=0/')

print(f"Anomalous users: {len(df_anomalous)}")
print(f"Normal users: {len(df_normal)}")
print(f"Total features: {len(df_normal.columns)}")
print("\nTop anomalies:")
print(df_anomalous[['user', 'anomaly_score_normalized', 'total_events']].to_string(index=False))
EOF
```

Expected output:
```
Anomalous users: 2
Normal users: 13
Total features: 31

Top anomalies:
   user  anomaly_score_normalized  total_events
    bob                  1.000000           172
  frank                  0.952555           219
```

### Pipeline Configuration

Edit `config/spark_config.yaml` to customize:

```yaml
kafka:
  bootstrap_servers: "localhost:9092"
  topic: "security-events"
  group_id: "spark-ml-pipeline"

anomaly_detection:
  algorithm: "isolation_forest"
  contamination: 0.1    # Expect 10% anomalies
  n_estimators: 100     # Number of trees
  random_state: 42      # Reproducibility
```

---

## Testing the Full Pipeline

### Generate Realistic Test Data

```bash
# Option 1: Using startup script (recommended)
./scripts/start-event-generator.sh

# Option 2: With custom configuration
EVENT_RATE=1000 EVENT_DURATION=30 ./scripts/start-event-generator.sh

# Option 3: Manual start with specific parameters
cd event-generator
java -jar target/event-generator-1.0-SNAPSHOT.jar \
  --rate 1000 \
  --duration 30 \
  --broker localhost:9092 \
  --topic security-events
```

### Verify Processing

```bash
# Check processor logs
# (in stream-processor terminal)
# Should see: [Event] Processed event evt_...

# Check event count
curl http://localhost:8081/api/events/count

# Check anomaly count
curl http://localhost:8081/api/anomalies/count

# View high-score anomalies
curl 'http://localhost:8081/api/anomalies/high-score?threshold=0.7' | jq
```

---

## Swagger UI

Access interactive API documentation:

http://localhost:8081/swagger-ui.html

---

## Common Commands

### Check Kafka Topic

```bash
# List topics
docker exec streamguard-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# Describe topic
docker exec streamguard-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe --topic security-events

# View messages
docker exec streamguard-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic security-events \
  --from-beginning \
  --max-messages 10
```

### Check RocksDB

```bash
# List column families
cd stream-processor/build
./rocksdb_dump --db=./data/events.db --command=list_column_families

# Count keys in column family
./rocksdb_dump --db=./data/events.db --column_family=anomalies --command=count
```

### Monitor Performance

```bash
# CPU & Memory usage
top -p $(pgrep stream-processor)

# Disk I/O
iostat -x 5

# Network traffic
iftop
```

---

## Stopping Services

```bash
# Stop stream processor (Ctrl+C in terminal)
# Stop query API (Ctrl+C in terminal)

# Stop Docker containers
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Nuclear Deep Cleanup

If you need to completely reset StreamGuard to a clean state (useful for troubleshooting or starting fresh):

### Quick Cleanup Script

```bash
#!/bin/bash
# Nuclear cleanup script - removes ALL StreamGuard data and artifacts

echo "🧹 Starting StreamGuard Nuclear Cleanup..."
echo "⚠️  WARNING: This will delete ALL data, builds, and Docker volumes!"
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

# Stop all running processes
echo "[1/8] Stopping running processes..."
pkill -f stream-processor
pkill -f query-api
sleep 2

# Stop and remove Docker containers
echo "[2/8] Stopping Docker containers..."
docker-compose down -v 2>/dev/null || true

# Remove Docker volumes
echo "[3/8] Removing Docker volumes..."
docker volume rm streamguard_kafka-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-logs 2>/dev/null || true
docker volume rm streamguard_prometheus-data 2>/dev/null || true
docker volume rm streamguard_grafana-data 2>/dev/null || true
docker volume rm streamguard_spark-data 2>/dev/null || true

# Remove RocksDB database
echo "[4/8] Removing RocksDB database..."
rm -rf data/events.db
rm -rf stream-processor/build/data

# Remove C++ build artifacts
echo "[5/8] Removing C++ build artifacts..."
rm -rf stream-processor/build
rm -rf stream-processor/cmake-build-debug
rm -rf stream-processor/cmake-build-release

# Remove Java build artifacts
echo "[6/8] Removing Java build artifacts..."
rm -rf query-api/target
rm -rf event-generator/target

# Remove Spark output
echo "[7/8] Removing Spark ML pipeline output..."
rm -rf spark-ml-pipeline/output
rm -rf spark-ml-pipeline/venv
rm -rf spark-ml-pipeline/__pycache__
rm -rf spark-ml-pipeline/src/__pycache__

# Remove logs and temporary files
echo "[8/8] Removing logs and temporary files..."
rm -rf logs/
rm -f *.log

echo "✅ Nuclear cleanup complete!"
echo ""
echo "To start fresh:"
echo "  1. cp .env.example .env"
echo "  2. docker-compose up -d"
echo "  3. Build and start components"
```

### Save as Cleanup Script

Save the above as `scripts/nuclear-cleanup.sh`:

```bash
# Create the script
cat > scripts/nuclear-cleanup.sh << 'EOF'
#!/bin/bash
# Nuclear cleanup script - removes ALL StreamGuard data and artifacts

echo "🧹 Starting StreamGuard Nuclear Cleanup..."
echo "⚠️  WARNING: This will delete ALL data, builds, and Docker volumes!"
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

# Stop all running processes
echo "[1/8] Stopping running processes..."
pkill -f stream-processor
pkill -f query-api
sleep 2

# Stop and remove Docker containers
echo "[2/8] Stopping Docker containers..."
docker-compose down -v 2>/dev/null || true

# Remove Docker volumes
echo "[3/8] Removing Docker volumes..."
docker volume rm streamguard_kafka-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-logs 2>/dev/null || true
docker volume rm streamguard_prometheus-data 2>/dev/null || true
docker volume rm streamguard_grafana-data 2>/dev/null || true
docker volume rm streamguard_spark-data 2>/dev/null || true

# Remove RocksDB database
echo "[4/8] Removing RocksDB database..."
rm -rf data/events.db
rm -rf stream-processor/build/data

# Remove C++ build artifacts
echo "[5/8] Removing C++ build artifacts..."
rm -rf stream-processor/build
rm -rf stream-processor/cmake-build-debug
rm -rf stream-processor/cmake-build-release

# Remove Java build artifacts
echo "[6/8] Removing Java build artifacts..."
rm -rf query-api/target
rm -rf event-generator/target

# Remove Spark output
echo "[7/8] Removing Spark ML pipeline output..."
rm -rf spark-ml-pipeline/output
rm -rf spark-ml-pipeline/venv
rm -rf spark-ml-pipeline/__pycache__
rm -rf spark-ml-pipeline/src/__pycache__

# Remove logs and temporary files
echo "[8/8] Removing logs and temporary files..."
rm -rf logs/
rm -f *.log

echo "✅ Nuclear cleanup complete!"
echo ""
echo "To start fresh:"
echo "  1. cp .env.example .env"
echo "  2. docker-compose up -d"
echo "  3. Build and start components"
EOF

# Make executable
chmod +x scripts/nuclear-cleanup.sh
```

### Run the Cleanup

```bash
./scripts/nuclear-cleanup.sh
```

### Manual Cleanup Steps

If you prefer manual cleanup:

```bash
# 1. Stop all processes
pkill -f stream-processor
pkill -f query-api

# 2. Stop Docker containers and remove volumes
docker-compose down -v

# 3. Remove all Docker volumes
docker volume prune -f

# 4. Remove database
rm -rf data/events.db
rm -rf stream-processor/build/data

# 5. Remove build artifacts
rm -rf stream-processor/build
rm -rf query-api/target

# 6. Remove Spark output
rm -rf spark-ml-pipeline/output
rm -rf spark-ml-pipeline/venv

# 7. Start fresh
cp .env.example .env
docker-compose up -d
```

### What Gets Cleaned

The nuclear cleanup removes:

| Component | Location | What's Removed |
|-----------|----------|----------------|
| **RocksDB Database** | `data/events.db`, `stream-processor/build/data` | All stored events, analyses, anomalies |
| **C++ Builds** | `stream-processor/build`, `cmake-build-*` | Compiled binaries, object files |
| **Java Builds** | `query-api/target`, `event-generator/target` | JAR files, compiled classes |
| **Spark Output** | `spark-ml-pipeline/output` | Parquet files, ML models, reports |
| **Docker Volumes** | Named volumes | Kafka data, Zookeeper data, metrics |
| **Python Virtual Env** | `spark-ml-pipeline/venv` | Python packages |
| **Logs** | `logs/`, `*.log` | All log files |

**Note:** Your `.env` configuration file, `.junie/`, and `demo/` directories are preserved.

---

## Troubleshooting

### Issue: "Failed to connect to Kafka"

**Solution:**
```bash
# Check if Kafka is running
docker-compose ps

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
sleep 30
```

### Issue: "RocksDB: Corruption detected"

**Solution:**
```bash
# Backup corrupted database
mv data/events.db data/events.db.backup

# Create fresh database
mkdir -p data
./stream-processor --broker localhost:9092 --topic security-events --group fresh-start
```

### Issue: "Query API can't read database"

**Solution:**
```bash
# Verify ROCKSDB_PATH is correct
echo $ROCKSDB_PATH

# Check file permissions
ls -la $ROCKSDB_PATH

# Ensure stream processor has created the database
ls -la stream-processor/build/data/events.db
```

### Issue: "AI analysis not working"

**Solution:**
```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Test API key manually
curl https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "content-type: application/json" \
  -d '{"model":"gpt-4","max_tokens":100,"messages":[{"role":"user","content":"test"}]}'
```

---

## Next Steps

1. **Configure Monitoring**: Set up Prometheus and Grafana dashboards
2. **Production Deployment**: Review [Deployment Guide](DEPLOYMENT.md)
3. **API Integration**: Read [API Documentation](../api/API_REFERENCE.md)
4. **AI/ML Features**: Explore [AI/ML Guide](AI_ML.md)
5. **Performance Tuning**: See [Architecture Guide](ARCHITECTURE.md)

---

## Quick Reference

### Stream Processor Arguments

```bash
--broker <address>          # Kafka broker (default: localhost:9092)
--topic <name>              # Kafka topic (default: security-events)
--group <id>                # Consumer group ID (required)
--db <path>                 # RocksDB path (default: ./data/events.db)
--metrics-port <port>       # Metrics port (default: 8080)
--config <file>             # Config file (optional)
```

### Environment Variables

```bash
OPENAI_API_KEY              # OpenAI GPT-4 API key
ROCKSDB_PATH                # RocksDB database path (Query API)
SERVER_PORT                 # Query API port (default: 8081)
```

### API Endpoints

```
GET  /api/events                          # Latest events
GET  /api/events/{eventId}                # Event by ID
GET  /api/anomalies                       # Latest anomalies
GET  /api/anomalies/high-score            # High-score anomalies
GET  /api/anomalies/user/{user}           # Anomalies by user
GET  /api/analyses                        # Latest AI analyses
GET  /api/analyses/{eventId}              # Analysis by event ID
GET  /api/stats/summary                   # Statistics summary
```

### Default Ports

| Service | Port | Purpose |
|---------|------|---------|
| Zookeeper | 2181 | Kafka coordination |
| Kafka | 9092 | Message broker |
| Stream Processor Metrics | 8080 | Prometheus metrics |
| Query API | 8081 | REST API |
| Prometheus | 9090 | Metrics storage |
| Grafana | 3000 | Dashboards |
