# Spark ML Pipeline - Quick Start Guide

**Get up and running with StreamGuard's Spark ML pipeline in 10 minutes**

---

## Prerequisites Check

```bash
# Verify installations
python3 --version  # Should be 3.11+
docker --version   # For Spark cluster (optional)
```

---

## 5-Step Quick Start

### Step 1: Setup Virtual Environment (2 min)

```bash
cd spark-ml-pipeline

# Create and activate venv
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### Step 2: Start Infrastructure (1 min)

```bash
# From project root
cd ..
docker-compose up -d kafka spark-master spark-worker

# Verify services
docker ps | grep streamguard
```

Expected output:
```
streamguard-kafka           Up
streamguard-spark-master    Up
streamguard-spark-worker    Up
```

### Step 3: Generate Sample Events (3 min)

```bash
# Run event generator for 3 minutes
cd event-generator
mvn clean package
java -jar target/event-generator-1.0.0.jar \
  --broker localhost:9092 \
  --topic security-events \
  --rate 100 \
  --duration 180
```

Expected output:
```
Generated 18,000 events in 3 minutes
```

### Step 4: Run Spark Pipeline (3 min)

```bash
cd ../spark-ml-pipeline
source venv/bin/activate

# Run pipeline
./scripts/run_pipeline.sh --max-events 10000
```

Expected output:
```
[Step 1/5] Reading events from Kafka
Events loaded: 10000

[Step 2/5] Extracting behavioral features
Selected 23 numeric features for ML

[Step 3/5] Detecting anomalies with ML
Detected 850 anomalies (8.5% of users)

[Step 4/5] Exporting training data to Parquet
Training data exported to: ./output/training_data

Pipeline Complete!
Duration: 45.2 seconds
```

### Step 5: Inspect Results (1 min)

```bash
# View anomaly report
cat output/training_data/anomaly_report.json

# Check Parquet files
ls -lh output/training_data/

# Quick analysis with Python
python -c "
import pandas as pd
df = pd.read_parquet('output/training_data')
print(f'Total users: {len(df)}')
print(f'Anomalies: {df[\"is_anomaly\"].sum()}')
print(df[df['is_anomaly']==1].head())
"
```

---

## Verify Success

✅ **Kafka Running**: `docker ps | grep kafka`
✅ **Spark UI Accessible**: Open http://localhost:8088
✅ **Events Generated**: Check Kafka UI at http://localhost:8090
✅ **Parquet Files Created**: `ls output/training_data/*.parquet`
✅ **Anomaly Report Exists**: `cat output/training_data/anomaly_report.json`

---

## Common Issues

### Issue 1: "Kafka connection refused"

**Solution**: Start Kafka
```bash
docker-compose up -d kafka
sleep 10  # Wait for startup
```

### Issue 2: "Module 'pyspark' not found"

**Solution**: Install dependencies
```bash
source venv/bin/activate
pip install -r requirements.txt
```

### Issue 3: "No events found in Kafka"

**Solution**: Generate events first
```bash
cd event-generator
java -jar target/event-generator-1.0.0.jar --rate 100 --duration 60
```

### Issue 4: "Permission denied: run_pipeline.sh"

**Solution**: Make script executable
```bash
chmod +x scripts/run_pipeline.sh
```

---

## Next Steps

1. **Explore Features**: Check `docs/SPARK_INTEGRATION.md` for detailed guide
2. **Tune Configuration**: Edit `config/spark_config.yaml`
3. **Run on Cluster**: Use `spark-submit` for production
4. **Integrate with C++**: Export baselines to stream processor

---

## Quick Commands Reference

```bash
# Start all services
docker-compose up -d

# Run pipeline (local mode)
./scripts/run_pipeline.sh --max-events 10000

# Run pipeline (cluster mode)
spark-submit \
  --master spark://spark-master:7077 \
  src/training_data_generator.py

# View Spark UI
open http://localhost:8088

# Stop all services
docker-compose down
```

---

## Architecture Overview

```
Kafka Events → Spark ML Pipeline → Training Data (Parquet)
                    ↓
            Feature Extraction (20+ features)
                    ↓
            Anomaly Detection (Isolation Forest)
                    ↓
            Labeled Dataset for ML Training
```

---

**Questions?** Check `README.md` or `docs/SPARK_INTEGRATION.md`

**Ready for more?** Try the advanced examples in `notebooks/`
