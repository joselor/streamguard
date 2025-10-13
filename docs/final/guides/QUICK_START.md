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

## Step 2: Start Infrastructure

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
NAME                IMAGE               STATUS
streamguard-kafka   wurstmeister/kafka  Up
streamguard-zk      zookeeper:3.8       Up
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

## Step 5: Configure Anthropic API Key

**Option A: Environment Variable**

```bash
export ANTHROPIC_API_KEY="sk-ant-api03-..."
```

**Option B: Configuration File**

Create `stream-processor/build/config.json`:

```json
{
  "anthropic_api_key": "sk-ant-api03-...",
  "model": "claude-3-5-sonnet-20241022"
}
```

---

## Step 6: Start Stream Processor

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

## Step 7: Start Query API

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

## Step 8: Send Test Events

Open a new terminal and send sample security events:

```bash
# Create sample event
cat > event.json << 'EOF'
{
  "event_id": "evt_$(date +%s)_001",
  "user": "alice",
  "timestamp": $(date +%s)000,
  "type": "LOGIN_SUCCESS",
  "source_ip": "10.0.1.100",
  "geo_location": "San Francisco, CA",
  "threat_score": 0.15,
  "metadata": {
    "user_agent": "Mozilla/5.0",
    "endpoint": "/api/login"
  }
}
EOF

# Send to Kafka using Docker
docker exec -i streamguard-kafka kafka-console-producer.sh \
  --broker-list localhost:9092 \
  --topic security-events < event.json

# Or use Python producer
python3 << 'EOF'
from kafka import KafkaProducer
import json
import time

producer = KafkaProducer(
    bootstrap_servers=['localhost:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

event = {
    "event_id": f"evt_{int(time.time())}_001",
    "user": "alice",
    "timestamp": int(time.time() * 1000),
    "type": "LOGIN_SUCCESS",
    "source_ip": "10.0.1.100",
    "geo_location": "San Francisco, CA",
    "threat_score": 0.15
}

producer.send('security-events', event)
producer.flush()
print("Event sent successfully")
EOF
```

---

## Step 9: Query Events

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

## Step 10: View Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/metrics

# Filter for anomaly metrics
curl -s http://localhost:8080/metrics | grep anomaly
```

---

## Step 11: Run Spark ML Pipeline (Optional)

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
cd scripts

# Run test data generator
python3 generate_test_data.py \
  --broker localhost:9092 \
  --topic security-events \
  --users 10 \
  --events 1000 \
  --rate 100
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
echo $ANTHROPIC_API_KEY

# Test API key manually
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{"model":"claude-3-5-sonnet-20241022","max_tokens":100,"messages":[{"role":"user","content":"test"}]}'
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
ANTHROPIC_API_KEY           # Claude API key
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
