# StreamGuard Grafana Dashboards

## Available Dashboards

### 1. StreamGuard Lambda Architecture (NEW - Sprint 12)
**File**: `streamguard-lambda.json`

**Purpose**: Monitor the complete Lambda Architecture with real-time (speed layer) and batch ML (Spark) metrics.

**Metrics Displayed**:
- **Batch ML Status**: Training data availability (green = available, red = unavailable)
- **Speed Layer Events**: Total events processed by C++ stream processor
- **Batch ML Users Analyzed**: Total users analyzed by Spark ML pipeline
- **Speed Layer Anomalies**: Real-time anomaly count from stream processor
- **Batch ML Anomalies**: Anomalous users detected by Spark ML
- **Anomaly Detection Rates**: Dual-layer comparison (real-time vs batch)
- **Lambda Architecture Data Volume**: Combined metrics from both layers
- **Batch ML Anomaly Rate**: Gauge showing anomaly detection percentage
- **AI Threat Analyses**: Total AI-powered threat analyses
- **User Classification**: Pie chart of anomalous vs normal users

**Screenshot Preview**:
```
┌─────────────────────────────────────────────────────────────┐
│ Batch ML    │ Speed Layer │ Batch ML Users │ Speed │ Batch  │
│ Status      │ Events      │ Analyzed       │ Layer │ ML     │
│ [AVAILABLE] │ [156,789]   │ [145]          │ Anom. │ Anom.  │
│             │             │                │ [12]  │ [2]    │
├─────────────────────────────┬───────────────────────────────┤
│ Anomaly Detection Rates     │ Lambda Architecture Volume    │
│ (Time Series - Dual Layer)  │ (Stacked Bar Chart)          │
├─────────────┬───────────────┼───────────────────────────────┤
│ Batch ML    │ AI Threat    │ User Classification          │
│ Anomaly     │ Analyses     │ (Donut Chart)                │
│ Rate Gauge  │ [456]        │ Anomalous: 8%  Normal: 92%   │
└─────────────┴──────────────┴───────────────────────────────┘
```

**Prometheus Queries Used**:
- `training_data_available` - Batch ML status (0/1)
- `streamguard_events_total` - Speed layer events
- `training_data_users_total` - Batch ML users
- `streamguard_anomalies_total` - Speed layer anomalies
- `training_data_anomalies_total` - Batch ML anomalies
- `training_data_anomaly_rate` - Batch detection rate
- `streamguard_ai_analyses_total` - AI analyses

---

### 2. StreamGuard GenAI
**File**: `streamguard-genai.json`

**Purpose**: Monitor GenAI Assistant performance, LLM usage, and costs.

---

### 3. StreamGuard Performance
**File**: `streamguard-performance.json`

**Purpose**: System performance metrics (CPU, memory, throughput).

---

### 4. StreamGuard Pipeline
**File**: `streamguard-pipeline.json`

**Purpose**: Event processing pipeline metrics.

---

### 5. StreamGuard Threats
**File**: `streamguard-threats.json`

**Purpose**: Threat detection and severity tracking.

---

## Deployment

### Automatic Loading (Docker Compose)

The dashboards are automatically loaded when Grafana starts via provisioning:

```yaml
# docker-compose.yml
grafana:
  image: grafana/grafana:latest
  volumes:
    - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
```

**Dashboard Provider Config**: `dashboard-provider.yml`
- Auto-updates every 10 seconds
- Allows UI updates
- No deletion restriction

### Manual Import

If running Grafana standalone:

1. **Access Grafana**: http://localhost:3000
2. **Login**: admin / admin (default)
3. **Import Dashboard**:
   - Click "+" → "Import"
   - Upload JSON file or paste content
   - Select Prometheus datasource
   - Click "Import"

### Verify Prometheus Datasource

Ensure Prometheus is configured as the default datasource:

**Name**: Prometheus
**Type**: Prometheus
**URL**: http://prometheus:9090 (Docker) or http://localhost:9090
**UID**: `PBFA97CFB590B2093`

---

## Sprint 12: Lambda Architecture Dashboard

### Panel Descriptions

#### 1. Batch ML Status (Stat Panel)
- **Metric**: `training_data_available`
- **Values**: 1 = Available (green), 0 = Unavailable (red)
- **Purpose**: Quick health check for batch ML layer

#### 2. Speed Layer Events (Stat Panel)
- **Metric**: `streamguard_events_total`
- **Purpose**: Total events processed by real-time layer

#### 3. Batch ML Users Analyzed (Stat Panel)
- **Metric**: `training_data_users_total`
- **Purpose**: Total users analyzed in batch ML pipeline

#### 4. Speed Layer Anomalies (Stat Panel)
- **Metric**: `streamguard_anomalies_total`
- **Thresholds**: Green < 5, Yellow 5-10, Red > 10
- **Purpose**: Real-time anomaly count

#### 5. Batch ML Anomalies (Stat Panel)
- **Metric**: `training_data_anomalies_total`
- **Thresholds**: Green < 5, Yellow 5-10, Red > 10
- **Purpose**: Batch ML anomaly count

#### 6. Anomaly Detection Rates (Time Series)
- **Metrics**:
  - `training_data_anomaly_rate` (Batch ML rate)
  - `streamguard_anomalies_total / streamguard_events_total` (Real-time rate)
- **Purpose**: Compare anomaly detection between layers

#### 7. Lambda Architecture Data Volume (Time Series)
- **Metrics**:
  - `streamguard_events_total` (Speed layer)
  - `training_data_users_total` (Batch layer)
- **Purpose**: Visualize data volume in both layers

#### 8. Batch ML Anomaly Rate (Gauge)
- **Metric**: `training_data_anomaly_rate`
- **Thresholds**: Green < 5%, Yellow 5-10%, Orange 10-20%, Red > 20%
- **Purpose**: Batch ML detection rate with visual thresholds

#### 9. AI Threat Analyses (Stat Panel)
- **Metric**: `streamguard_ai_analyses_total`
- **Purpose**: Total AI-powered threat analyses

#### 10. User Classification (Pie Chart)
- **Metrics**:
  - `training_data_anomalies_total` (Anomalous)
  - `training_data_users_total - training_data_anomalies_total` (Normal)
- **Purpose**: Visualize user distribution (anomalous vs normal)

---

## Testing the Dashboard

### 1. Start Services

```bash
# Start Prometheus, Grafana, and Java API
docker-compose up -d prometheus grafana

# Start Java Query API (exposes metrics)
cd query-api
java -jar target/query-api-1.0.0.jar
```

### 2. Generate Batch ML Data

```bash
# Run Spark ML pipeline to generate training data
cd spark-ml-pipeline
./scripts/run_pipeline.sh --max-events 10000 --output ./output/training_data
```

### 3. Verify Metrics

```bash
# Check if metrics are being scraped
curl http://localhost:8081/api/metrics | grep training_data

# Expected output:
# training_data_available 1
# training_data_users_total 145
# training_data_anomalies_total 12
# training_data_anomaly_rate 0.0827
```

### 4. Access Dashboard

1. Open http://localhost:3000
2. Navigate to "Dashboards" → "StreamGuard Lambda Architecture"
3. Verify all panels show data
4. Check time range (default: last 6 hours)

---

## Troubleshooting

### Dashboard Not Appearing

**Issue**: Dashboard doesn't auto-load

**Solution**:
```bash
# Check Grafana logs
docker-compose logs grafana | grep dashboard

# Verify provisioning directory
ls -la monitoring/grafana/dashboards/

# Restart Grafana
docker-compose restart grafana
```

### No Data in Panels

**Issue**: Panels show "No data"

**Solution**:
```bash
# 1. Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets

# 2. Verify Java API metrics endpoint
curl http://localhost:8081/api/metrics

# 3. Check Prometheus queries in Grafana
# Go to panel → Edit → Run query manually
```

### Metrics Showing Zero

**Issue**: All metrics show 0

**Solution**:
```bash
# 1. Run Spark ML pipeline to generate data
cd spark-ml-pipeline
./scripts/run_pipeline.sh --max-events 1000

# 2. Verify training data exists
ls -la ./output/training_data/
cat ./output/training_data/anomaly_report.json

# 3. Restart Java API to reload data
pkill java
java -jar query-api/target/query-api-1.0.0.jar
```

### UID Conflict

**Issue**: "Dashboard with UID already exists"

**Solution**:
```bash
# Option 1: Delete existing dashboard via UI
# Dashboards → StreamGuard Lambda Architecture → Settings → Delete

# Option 2: Change UID in JSON
# Edit streamguard-lambda.json
# Change: "uid": "streamguard-lambda-v2"
```

---

## Customization

### Add New Panel

1. Edit `streamguard-lambda.json`
2. Add new panel to `panels` array
3. Set `gridPos` for layout (24-column grid)
4. Configure datasource and query
5. Save and refresh Grafana

### Modify Thresholds

```json
"thresholds": {
  "mode": "absolute",
  "steps": [
    { "color": "green", "value": null },
    { "color": "yellow", "value": 5 },
    { "color": "red", "value": 10 }
  ]
}
```

### Change Time Range

```json
"time": {
  "from": "now-24h",  // Change from 6h to 24h
  "to": "now"
}
```

---

## Demo Preparation

### Pre-Demo Checklist

- [ ] Run Spark ML pipeline (generates fresh data)
- [ ] Start all services (Kafka, Prometheus, Grafana, Java API)
- [ ] Verify metrics endpoint returns data
- [ ] Open dashboard and set time range to "Last 6 hours"
- [ ] Take screenshot for presentation
- [ ] Prepare talking points (Lambda Architecture benefits)

### Demo Script

1. **Show Lambda Architecture Overview**
   - Point to dual-layer panels (Speed + Batch)
   - Explain real-time vs historical analysis

2. **Highlight Batch ML Metrics**
   - Show "Batch ML Status" (green = available)
   - Point to "145 users analyzed, 12 anomalies"
   - Explain 8.3% anomaly detection rate

3. **Compare Detection Rates**
   - Show "Anomaly Detection Rates" time series
   - Explain differences between real-time and batch
   - Batch ML = more comprehensive, less false positives

4. **Show User Classification**
   - Pie chart: 92% normal, 8% anomalous
   - Explain Spark ML Isolation Forest algorithm
   - Mention 31 behavioral features analyzed

5. **Demonstrate Data Volume**
   - Stacked bar chart showing both layers
   - Explain scalability benefits
   - Lambda Architecture handles both real-time + batch

---

## Metrics Reference

### Speed Layer Metrics (from stream-processor)

| Metric | Type | Description |
|--------|------|-------------|
| `streamguard_events_total` | gauge | Total events processed |
| `streamguard_anomalies_total` | gauge | Total anomalies detected |
| `streamguard_ai_analyses_total` | gauge | Total AI analyses |

### Batch ML Metrics (from query-api - Sprint 12)

| Metric | Type | Description |
|--------|------|-------------|
| `training_data_available` | gauge | Training data status (0/1) |
| `training_data_users_total` | gauge | Total users analyzed |
| `training_data_anomalies_total` | gauge | Anomalous users detected |
| `training_data_anomaly_rate` | gauge | Detection rate (0.0-1.0) |

---

## Support

**Dashboard Issues**: Check `monitoring/grafana/` directory
**Metrics Issues**: Check `query-api/src/main/java/.../MetricsController.java`
**Spark ML Issues**: Check `spark-ml-pipeline/` directory

**Created**: Sprint 12 (October 27, 2025)
**Author**: Lambda Architecture Integration
**Version**: 1.0
