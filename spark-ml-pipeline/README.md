# StreamGuard Spark ML Pipeline

**Apache Spark-based ML training data generation pipeline for security event analysis**

---

## Overview

This module implements a **Lambda Architecture** approach to StreamGuard, adding batch processing capabilities alongside the existing real-time C++ stream processor. It demonstrates:

- **Distributed Data Processing**: Apache Spark for large-scale event analysis
- **Feature Engineering**: Extract 20+ behavioral features from security events
- **ML Anomaly Detection**: Isolation Forest and K-Means clustering
- **Training Data Generation**: Labeled datasets in Parquet format
- **Kafka Integration**: Read historical events from Kafka topics

---

## Architecture: Lambda Pattern

```
┌──────────────────────────────────────────────────────────────────┐
│                    Lambda Architecture                            │
└──────────────────────────────────────────────────────────────────┘

  ┌─────────────────┐
  │  Kafka Events   │
  │ (security-evts) │
  └────────┬────────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
┌─────────┐  ┌──────────────┐
│ SPEED   │  │   BATCH      │ <-- THIS MODULE
│ LAYER   │  │   LAYER      │
│         │  │              │
│ C++     │  │ Apache Spark │
│ Stream  │  │ PySpark 3.5  │
│ Proc    │  │              │
│         │  │ - Historical │
│ Real-   │  │   analysis   │
│ time    │  │ - Feature    │
│ <1ms    │  │   engineering│
│ latency │  │ - ML training│
│         │  │ - Anomaly    │
│         │  │   detection  │
└────┬────┘  └──────┬───────┘
     │              │
     │              ▼
     │       ┌──────────────┐
     │       │ Training Data│
     │       │  (Parquet)   │
     │       └──────────────┘
     │
     ▼
┌──────────────────────┐
│   SERVING LAYER      │
│  - Query API (Java)  │
│  - RocksDB           │
│  - Grafana           │
└──────────────────────┘
```

**Benefits**:
- **Speed Layer**: Real-time threat detection (<1ms latency)
- **Batch Layer**: Deep analysis, feature engineering, model training
- **Best of Both Worlds**: Low latency + comprehensive analysis

---

## Features

### 1. Behavioral Feature Extraction

Extracts 20+ features per user from security events:

**User Features**:
- Total events, unique IPs, unique event types
- Average/max threat score
- Failed authentication rate
- Unusual hour activity rate (0-6 AM)
- Geographic diversity
- Events per day

**Temporal Features**:
- Hourly activity distribution
- Standard deviation of activity
- Weekend vs weekday patterns

**IP Features**:
- IP frequency distribution
- Rare IP detection (single-use IPs)
- IP diversity score

**Sequence Features**:
- Average time between events
- Burst detection (rapid-fire events)
- Event interval variance

### 2. ML Anomaly Detection

**Isolation Forest** (scikit-learn):
- Probabilistic anomaly detection
- Configurable contamination rate (default: 10%)
- Anomaly scores normalized to 0-1 range
- Suitable for unsupervised learning

**K-Means Clustering** (Spark MLlib):
- Distributed clustering
- Distance-based anomaly scoring
- Scalable to millions of users
- No training data required

### 3. Training Data Generation

- **Format**: Apache Parquet (compressed, columnar)
- **Partitioning**: By anomaly label (is_anomaly=0/1)
- **Output**: Feature vectors + labels + metadata
- **Size**: ~500 bytes per user (compressed)

---

## Quick Start

### Prerequisites

```bash
# Python 3.11+
python3 --version

# Docker (for Spark cluster)
docker --version
```

### 1. Setup Environment

```bash
cd spark-ml-pipeline

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Start Infrastructure

```bash
# From project root
docker-compose up -d zookeeper kafka spark-master spark-worker

# Verify Spark is running
open http://localhost:8088  # Spark Master UI
```

### 3. Generate Sample Events

```bash
# Terminal 1: Run event generator
cd ../event-generator
mvn clean package
java -jar target/event-generator-1.0.0.jar \
  --broker localhost:9092 \
  --topic security-events \
  --rate 100 \
  --duration 300  # 5 minutes = ~30,000 events
```

### 4. Run ML Pipeline

```bash
# Terminal 2: Generate training data
cd spark-ml-pipeline
source venv/bin/activate

python src/training_data_generator.py \
  --config config/spark_config.yaml \
  --start-offset earliest \
  --max-events 10000
```

### 5. Inspect Results

```bash
# Check output
ls -lh output/training_data/

# View anomaly report
cat output/training_data/anomaly_report.json

# Inspect Parquet files (requires pandas)
python -c "import pandas as pd; df = pd.read_parquet('output/training_data'); print(df.head())"
```

---

## Configuration

Edit `config/spark_config.yaml`:

```yaml
# Kafka settings
kafka:
  bootstrap_servers: "localhost:9092"
  topic: "security-events"

# Spark settings
spark:
  master: "local[*]"  # or "spark://spark-master:7077" for cluster mode
  executor_memory: "2g"
  driver_memory: "2g"

# Anomaly detection
anomaly_detection:
  algorithm: "isolation_forest"  # or "kmeans"
  contamination: 0.1  # Expected anomaly rate
  n_estimators: 100
```

---

## Usage Examples

### Example 1: Time-Range Analysis

```python
from kafka_reader import KafkaEventReader
from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("TimeRange").getOrCreate()
reader = KafkaEventReader(spark)

# Analyze last 24 hours
df = reader.read_time_range(
    start_time="2024-10-12T00:00:00",
    end_time="2024-10-13T00:00:00"
)

stats = reader.get_summary_statistics(df)
print(f"Events: {stats['total_events']}, Users: {stats['unique_users']}")
```

### Example 2: Feature Extraction Only

```python
from feature_extractor import FeatureExtractor

extractor = FeatureExtractor(spark)

# Extract features
df_features = extractor.extract_all_features(df_events)

# Get feature vector columns
feature_cols = extractor.get_feature_vector_columns(df_features)

# Show top users by threat score
df_features.orderBy("avg_threat_score", ascending=False).show(10)
```

### Example 3: Streaming Mode (Micro-Batch)

```python
from kafka_reader import KafkaEventReader

reader = KafkaEventReader(spark)

# Start streaming
df_stream = reader.read_streaming(checkpoint_location="./checkpoints")

# Process in micro-batches
query = df_stream.writeStream \
    .foreachBatch(process_batch_function) \
    .start()

query.awaitTermination()
```

---

## Pipeline Components

### 1. `kafka_reader.py`

**Purpose**: Read events from Kafka into Spark DataFrames

**Key Methods**:
- `read_batch()` - Batch mode (historical events)
- `read_streaming()` - Streaming mode (continuous processing)
- `read_time_range()` - Filter by timestamp
- `get_summary_statistics()` - Event statistics

### 2. `feature_extractor.py`

**Purpose**: Extract behavioral features from raw events

**Key Methods**:
- `extract_user_features()` - Per-user aggregations
- `extract_temporal_features()` - Time-based patterns
- `extract_ip_features()` - IP behavior analysis
- `extract_sequence_features()` - Event sequencing
- `extract_all_features()` - Complete feature set

### 3. `anomaly_detector.py`

**Purpose**: ML-based anomaly detection

**Key Methods**:
- `detect_with_isolation_forest()` - Isolation Forest (sklearn)
- `detect_with_kmeans()` - K-Means clustering (Spark MLlib)
- `generate_anomaly_report()` - Summary statistics

### 4. `training_data_generator.py`

**Purpose**: Main orchestrator (CLI application)

**Workflow**:
1. Read events from Kafka
2. Extract features
3. Detect anomalies with ML
4. Export to Parquet
5. Generate report

---

## Output Format

### Parquet Schema

```
root
 |-- user: string
 |-- total_events: long
 |-- unique_ips: long
 |-- unique_event_types: long
 |-- avg_threat_score: double
 |-- failed_auth_rate: double
 |-- unusual_hour_rate: double
 |-- ip_diversity_score: double
 |-- events_per_day: double
 |-- [... 10+ more features ...]
 |-- anomaly_score_normalized: double (0.0 - 1.0)
 |-- is_anomaly: integer (0 or 1)
```

### Anomaly Report (JSON)

```json
{
  "total_users": 1000,
  "anomalous_users": 95,
  "anomaly_rate": 0.095,
  "top_anomalies": [
    {
      "user": "alice",
      "anomaly_score": 0.952,
      "total_events": 487,
      "avg_threat_score": 0.823
    }
  ]
}
```

---

## Performance

### Benchmarks

| Metric | Local Mode | Cluster Mode |
|--------|------------|--------------|
| **Events Processed** | 10,000/sec | 50,000+/sec |
| **Feature Extraction** | ~5 sec/10K events | ~2 sec/10K events |
| **Anomaly Detection** | ~3 sec/1K users | ~1 sec/1K users |
| **Parquet Write** | ~1 sec/10MB | ~500ms/10MB |

**Tested On**:
- MacBook Pro M1 (2021), 16GB RAM
- Spark 3.5.0, PySpark, local[*] mode

### Scalability

- **Horizontal**: Add more Spark workers via `docker-compose scale spark-worker=3`
- **Vertical**: Increase memory/cores in `config/spark_config.yaml`
- **Data Size**: Tested up to 1M events, 10K users

---

## Integration with StreamGuard

### Use Cases

1. **Baseline Training**:
   - Generate feature baselines from historical data
   - Train supervised models for C++ processor
   - Export thresholds to configuration files

2. **Periodic Retraining**:
   - Run daily/weekly to update models
   - Detect concept drift
   - Refine anomaly detection thresholds

3. **Deep Analysis**:
   - Investigate specific incidents
   - Historical trend analysis
   - User behavior profiling

4. **Model Export**:
   - Train models in Spark
   - Export to ONNX/PMML
   - Deploy to C++ inference engine

---

## Running in Production

### Cluster Mode

```bash
# Submit to Spark cluster
spark-submit \
  --master spark://spark-master:7077 \
  --deploy-mode cluster \
  --executor-memory 4g \
  --num-executors 4 \
  src/training_data_generator.py \
  --config config/spark_config.yaml
```

### Scheduled Jobs

```bash
# Cron job (daily at 2 AM)
0 2 * * * cd /path/to/spark-ml-pipeline && ./scripts/run_daily_training.sh
```

### Monitoring

- **Spark UI**: http://localhost:8088
- **Metrics**: Expose Spark metrics to Prometheus
- **Logs**: Configure `loguru` to write to files

---

## Troubleshooting

### Issue: "Py4JJavaError: An error occurred while calling"

**Solution**: Ensure Kafka connector JAR is available:
```python
spark = SparkSession.builder \
    .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0") \
    .getOrCreate()
```

### Issue: "OutOfMemoryError"

**Solution**: Increase executor/driver memory in config:
```yaml
spark:
  executor_memory: "4g"
  driver_memory: "4g"
```

### Issue: Slow Feature Extraction

**Solution**: Increase shuffle partitions:
```yaml
spark:
  shuffle_partitions: 8  # Default is 4
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Apache Spark** | 3.5.0 | Distributed data processing |
| **PySpark** | 3.5.0 | Python Spark API |
| **Python** | 3.11+ | Application language |
| **scikit-learn** | 1.3.2 | ML algorithms (Isolation Forest) |
| **Kafka** | 3.6.0 | Event streaming |
| **Parquet** | via pyarrow | Columnar storage format |

---

## Future Enhancements

1. **Structured Streaming**:
   - Continuous micro-batch processing
   - Incremental feature updates
   - Real-time model retraining

2. **Advanced ML**:
   - LSTM for sequence modeling
   - Graph-based anomaly detection
   - Ensemble methods

3. **Distributed Training**:
   - Spark MLlib distributed ML
   - TensorFlow on Spark
   - Horovod integration

4. **AutoML**:
   - Hyperparameter tuning
   - Feature selection
   - Model comparison

---

## License

Proprietary - StreamGuard Demo Project

---

## Author

**Jose Ortuno**
Built for CrowdStrike Technical Interview

**Contact**: [Your Email]
**Demo**: [GitHub Repository](https://github.com/joselor/streamguard)