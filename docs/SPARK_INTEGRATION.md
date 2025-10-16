# Apache Spark Integration Guide

**StreamGuard Lambda Architecture: Real-Time + Batch Processing**

---

## Table of Contents

1. [Overview](#overview)
2. [Lambda Architecture](#lambda-architecture)
3. [Why Spark?](#why-spark)
4. [System Architecture](#system-architecture)
5. [Integration Points](#integration-points)
6. [Use Cases](#use-cases)
7. [Setup & Configuration](#setup--configuration)
8. [Running the Pipeline](#running-the-pipeline)
9. [Performance & Scalability](#performance--scalability)
10. [Best Practices](#best-practices)

---

## Overview

StreamGuard now implements a **Lambda Architecture**, combining:
- **Speed Layer**: Real-time C++ stream processor (<1ms latency)
- **Batch Layer**: Apache Spark ML pipeline for deep analysis
- **Serving Layer**: Unified query API (Java Spring Boot)

This architecture provides:
✅ **Low-latency** real-time threat detection
✅ **Deep analysis** with batch processing
✅ **ML training** data generation
✅ **Scalability** for historical analysis

---

## Lambda Architecture

### Concept

Lambda Architecture handles both real-time and batch processing:

```
                    ┌─────────────────┐
                    │   Data Sources  │
                    │  (Kafka Events) │
                    └────────┬────────┘
                             │
                   ┌─────────┴──────────┐
                   │                    │
                   ▼                    ▼
           ┌─────────────┐      ┌─────────────┐
           │ SPEED LAYER │      │ BATCH LAYER │
           │             │      │             │
           │ C++ Stream  │      │ Apache      │
           │ Processor   │      │ Spark       │
           │             │      │             │
           │ • Real-time │      │ • Historical│
           │ • <1ms      │      │ • Deep ML   │
           │ • Events    │      │ • Features  │
           │ • Anomalies │      │ • Training  │
           └──────┬──────┘      └──────┬──────┘
                  │                    │
                  │                    ▼
                  │            ┌──────────────┐
                  │            │ Training Data│
                  │            │  (Parquet)   │
                  │            └──────────────┘
                  │
                  ▼
           ┌────────────────────┐
           │   SERVING LAYER    │
           │                    │
           │  • Query API       │
           │  • RocksDB         │
           │  • REST endpoints  │
           │  • Grafana         │
           └────────────────────┘
```

### Benefits

| Aspect | Speed Layer (C++) | Batch Layer (Spark) |
|--------|------------------|---------------------|
| **Latency** | <1ms | Minutes to hours |
| **Complexity** | Simple, fast | Complex, deep analysis |
| **Data Volume** | Streaming, recent | Historical, all events |
| **Accuracy** | Real-time approx | Precise, complete |
| **Use Case** | Threat detection | ML training, reports |

---

## Why Spark?

### Distributed Computing Power

Apache Spark provides:

1. **Scalability**: Process millions of events across cluster
2. **Fault Tolerance**: Resilient Distributed Datasets (RDDs)
3. **Rich API**: DataFrame API, SQL, MLlib
4. **Kafka Integration**: Native Kafka connector
5. **Parquet Support**: Efficient columnar storage

### Interview Value

Demonstrates:
- ✅ Distributed systems expertise
- ✅ Lambda Architecture understanding
- ✅ Multiple processing paradigms (stream + batch)
- ✅ Big data technology stack
- ✅ Production-ready thinking

---

## System Architecture

### Complete StreamGuard Architecture with Spark

```
┌──────────────────────────────────────────────────────────────────────┐
│                       StreamGuard Platform v4.0                       │
│                    (Lambda Architecture)                              │
└──────────────────────────────────────────────────────────────────────┘

┌────────────────┐      ┌──────────────────┐      ┌─────────────┐
│ Event Generator│─────▶│  Apache Kafka    │◀────▶│  ZooKeeper  │
│   (Synthetic)  │      │ (security-events)│      │             │
└────────────────┘      └────────┬─────────┘      └─────────────┘
                                 │
                   ┌─────────────┴─────────────┐
                   │                           │
                   ▼                           ▼
        ┌────────────────────┐      ┌────────────────────┐
        │  SPEED LAYER       │      │  BATCH LAYER       │
        │  Stream Processor  │      │  Spark ML Pipeline │
        │  (C++)             │      │  (PySpark)         │
        │                    │      │                    │
        │ ┌────────────────┐ │      │ ┌────────────────┐ │
        │ │ Kafka Consumer │ │      │ │ Kafka Reader   │ │
        │ │ ├─ Real-time   │ │      │ │ ├─ Batch mode  │ │
        │ │ └─ <1ms        │ │      │ │ └─ Historical  │ │
        │ └────────────────┘ │      │ └────────────────┘ │
        │                    │      │                    │
        │ ┌────────────────┐ │      │ ┌────────────────┐ │
        │ │ Anomaly        │ │      │ │ Feature        │ │
        │ │ Detection      │ │      │ │ Engineering    │ │
        │ │ ├─ Statistical │ │      │ │ ├─ 20+ features│ │
        │ │ └─ Fast rules  │ │      │ │ └─ Behavioral  │ │
        │ └────────────────┘ │      │ └────────────────┘ │
        │                    │      │                    │
        │ ┌────────────────┐ │      │ ┌────────────────┐ │
        │ │ AI Analysis    │ │      │ │ ML Anomaly     │ │
        │ │ ├─ OpenAI API  │ │      │ │ Detection      │ │
        │ │ └─ Selective   │ │      │ │ ├─ Isolation   │ │
        │ └────────────────┘ │      │ │   Forest       │ │
        │                    │      │ │ └─ K-Means     │ │
        │ ┌────────────────┐ │      │ └────────────────┘ │
        │ │ RocksDB        │ │      │                    │
        │ │ ├─ events      │ │      │ ┌────────────────┐ │
        │ │ ├─ anomalies   │ │      │ │ Parquet Export │ │
        │ │ └─ ai_analysis │ │      │ │ ├─ Training    │ │
        │ └────────────────┘ │      │ │   data         │ │
        └──────┬─────────────┘      │ │ └─ Labels      │ │
               │                    │ └────────────────┘ │
               │                    └────────────────────┘
               │
               ▼
┌────────────────────────────────────────────┐
│         SERVING LAYER                      │
│                                            │
│  ┌──────────────────┐  ┌────────────────┐ │
│  │  Query API       │  │  Monitoring    │ │
│  │  Spring Boot 3.2 │  │                │ │
│  │                  │  │ • Prometheus   │ │
│  │ • /api/events    │  │ • Grafana      │ │
│  │ • /api/anomalies │  │ • Spark UI     │ │
│  │ • /api/analyses  │  │                │ │
│  │ • Swagger UI     │  │ Dashboards:    │ │
│  └──────────────────┘  │ • Real-time    │ │
│                        │ • Batch jobs   │ │
│                        │ • ML metrics   │ │
│                        └────────────────┘ │
└────────────────────────────────────────────┘
```

---

## Integration Points

### 1. Kafka as Central Hub

Both layers read from the same Kafka topic:

```python
# Speed Layer (C++)
KafkaConsumer consumer({"security-events"}, kafka_conf);
consumer.subscribe(topics);

# Batch Layer (PySpark)
df = spark.read \
    .format("kafka") \
    .option("subscribe", "security-events") \
    .option("startingOffsets", "earliest") \
    .load()
```

### 2. Feature Parity

Ensure features are consistent between layers:

| Feature | Speed Layer (C++) | Batch Layer (Spark) |
|---------|------------------|---------------------|
| **Event count** | `baseline.total_events` | `F.count("*")` |
| **Unique IPs** | `baseline.seen_ips.size()` | `F.countDistinct("source_ip")` |
| **Threat score** | `event.threat_score` | `F.avg("threat_score")` |
| **Failed rate** | `failures / total` | `F.sum(...) / F.count(*)` |

### 3. Training Data → Model Deployment

```
Spark Training Pipeline
    ↓
Generate labeled dataset (Parquet)
    ↓
Train ML model (Isolation Forest)
    ↓
Export model (ONNX/PMML)
    ↓
Deploy to C++ processor
    ↓
Real-time inference
```

### 4. Monitoring Integration

Prometheus metrics from both layers:

```yaml
# Prometheus scrape config
scrape_configs:
  - job_name: 'stream-processor'
    static_configs:
      - targets: ['localhost:8080']

  - job_name: 'spark-metrics'
    static_configs:
      - targets: ['localhost:8088']
```

---

## Use Cases

### Use Case 1: Daily Training Data Generation

**Objective**: Generate labeled training data from yesterday's events

**Steps**:
1. Run event generator for 24 hours
2. Accumulate ~10M events in Kafka
3. Run Spark pipeline overnight
4. Generate Parquet dataset with labels
5. Use for model retraining

**Command**:
```bash
python src/training_data_generator.py \
  --start-offset earliest \
  --output ./output/training_$(date +%Y%m%d)
```

**Output**:
```
output/training_20241012/
├── is_anomaly=0/
│   └── part-00000.parquet  (Normal users)
├── is_anomaly=1/
│   └── part-00000.parquet  (Anomalous users)
└── anomaly_report.json
```

### Use Case 2: Incident Investigation

**Objective**: Deep analysis of a specific user's behavior

**Steps**:
1. Filter Kafka events for specific user
2. Extract all behavioral features
3. Compare to baseline (normal users)
4. Generate detailed report

**Code**:
```python
df_user = df_events.filter(F.col("user") == "alice")
features = extractor.extract_all_features(df_user)
anomalies = detector.detect_anomalies(features, feature_cols)
```

### Use Case 3: Baseline Establishment

**Objective**: Create behavioral baselines for new user cohort

**Steps**:
1. Process 30 days of historical events
2. Extract features per user
3. Calculate mean, std, percentiles
4. Export baseline config for C++ processor

**Output**: JSON configuration file
```json
{
  "baseline_config": {
    "avg_events_per_day": 150.5,
    "avg_unique_ips": 3.2,
    "unusual_hour_threshold": 0.15
  }
}
```

### Use Case 4: A/B Testing

**Objective**: Compare anomaly detection algorithms

**Steps**:
1. Run Isolation Forest on dataset
2. Run K-Means on same dataset
3. Compare precision, recall, F1
4. Select best algorithm for production

---

## Setup & Configuration

### 1. Install Dependencies

```bash
cd spark-ml-pipeline

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install PySpark and dependencies
pip install -r requirements.txt
```

### 2. Configure Spark

Edit `config/spark_config.yaml`:

```yaml
spark:
  app_name: "StreamGuard ML Training Pipeline"
  master: "local[*]"  # Local mode
  # master: "spark://spark-master:7077"  # Cluster mode
  executor_memory: "2g"
  driver_memory: "2g"
  shuffle_partitions: 4
```

### 3. Start Spark Cluster (Optional)

```bash
# From project root
docker-compose up -d spark-master spark-worker

# Verify
curl http://localhost:8088
```

### 4. Configure Kafka Connection

```yaml
kafka:
  bootstrap_servers: "localhost:9092"
  # bootstrap_servers: "kafka:29092"  # Docker network
  topic: "security-events"
  group_id: "spark-ml-pipeline"
```

---

## Running the Pipeline

### Local Mode (Development)

```bash
cd spark-ml-pipeline
source venv/bin/activate

# Run with default config
python src/training_data_generator.py

# Run with custom config
python src/training_data_generator.py \
  --config config/spark_config.yaml \
  --max-events 10000 \
  --output ./output/test_run
```

### Cluster Mode (Production)

```bash
# Submit to Spark cluster
spark-submit \
  --master spark://spark-master:7077 \
  --deploy-mode cluster \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 4 \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  src/training_data_generator.py \
  --config config/spark_config.yaml
```

### Scheduled Jobs (Cron)

```bash
# Daily training at 2 AM
0 2 * * * cd /path/to/spark-ml-pipeline && ./scripts/run_daily.sh >> logs/daily.log 2>&1

# Weekly deep analysis on Sunday
0 0 * * 0 cd /path/to/spark-ml-pipeline && ./scripts/run_weekly.sh >> logs/weekly.log 2>&1
```

---

## Performance & Scalability

### Benchmarks

| Dataset Size | Events | Users | Duration (local) | Duration (cluster) |
|-------------|--------|-------|------------------|-------------------|
| **Small** | 10K | 100 | 8 sec | 3 sec |
| **Medium** | 100K | 1K | 45 sec | 12 sec |
| **Large** | 1M | 10K | 6 min | 90 sec |
| **XL** | 10M | 100K | 1 hour | 12 min |

**Test Environment**:
- Local: MacBook Pro M1, 16GB RAM, 8 cores
- Cluster: 1 master + 3 workers, 4GB each

### Scalability Tips

1. **Horizontal Scaling**:
```bash
docker-compose up -d --scale spark-worker=4
```

2. **Increase Partitions**:
```yaml
spark:
  shuffle_partitions: 16  # More for large datasets
```

3. **Memory Tuning**:
```yaml
spark:
  executor_memory: "4g"
  driver_memory: "4g"
```

4. **Caching**:
```python
df_events.cache()  # Cache frequently accessed DataFrames
df_features = extractor.extract_all_features(df_events)
df_events.unpersist()  # Release memory
```

---

## Best Practices

### 1. Partition Management

```python
# Repartition for better parallelism
df = df.repartition(8, "user")

# Coalesce before writing
df.coalesce(1).write.parquet("output/")
```

### 2. Resource Management

```python
# Stop Spark session when done
spark.stop()

# Use context manager
with SparkSession.builder.getOrCreate() as spark:
    # Process data
    pass
```

### 3. Error Handling

```python
try:
    df = kafka_reader.read_batch()
except Exception as e:
    logger.error(f"Failed to read from Kafka: {e}")
    # Fallback or retry logic
```

### 4. Monitoring

```python
# Log progress
logger.info(f"Processing {df.count()} events")

# Track metrics
start_time = time.time()
# ... processing ...
duration = time.time() - start_time
logger.info(f"Completed in {duration:.2f} seconds")
```

### 5. Testing

```python
# Unit test with sample data
def test_feature_extraction():
    spark = SparkSession.builder.master("local[1]").getOrCreate()
    extractor = FeatureExtractor(spark)

    # Create test DataFrame
    test_data = [...]
    df = spark.createDataFrame(test_data)

    # Test extraction
    features = extractor.extract_user_features(df)
    assert features.count() > 0
```

---

## Comparison: Speed vs Batch Layer

| Feature | Speed Layer (C++) | Batch Layer (Spark) |
|---------|------------------|---------------------|
| **Language** | C++17 | Python (PySpark) |
| **Latency** | <1ms | Minutes to hours |
| **Throughput** | 12K events/sec | 50K+ events/sec (cluster) |
| **Data Access** | Kafka stream | Kafka batch / HDFS / S3 |
| **Processing** | Incremental | Batch reprocessing |
| **Anomaly Detection** | Statistical baseline | ML (Isolation Forest, K-Means) |
| **Feature Count** | 5-10 simple features | 20+ complex features |
| **Memory** | ~200MB | 2-8GB (configurable) |
| **Use Case** | Real-time alerts | Training data, reports |

---

## Future Enhancements

1. **Structured Streaming**:
   - Migrate to Spark Structured Streaming
   - Continuous micro-batch processing
   - Watermarking for late events

2. **Advanced ML**:
   - LSTM for sequence modeling
   - Graph-based user similarity
   - Ensemble anomaly detection

3. **Delta Lake Integration**:
   - ACID transactions on data lake
   - Time travel for historical queries
   - Schema evolution

4. **MLflow Integration**:
   - Experiment tracking
   - Model registry
   - Automated deployment

---

## Conclusion

The Apache Spark integration completes StreamGuard's **Lambda Architecture**, providing:

✅ **Real-time** threat detection (C++ speed layer)
✅ **Batch** deep analysis (Spark batch layer)
✅ **ML training** data generation
✅ **Scalability** for enterprise workloads
✅ **Production-ready** architecture

**Interview Impact**: Demonstrates expertise in distributed systems, architectural patterns, and practical big data engineering.

---

**Document Version**: 1.0
**Last Updated**: October 12, 2025
**Author**: Jose Ortuno
