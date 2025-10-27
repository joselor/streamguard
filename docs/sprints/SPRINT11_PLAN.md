# Sprint 11 Plan: Spark ML Lambda Architecture Integration

**Sprint Duration**: Sprint 11
**Start Date**: October 27, 2025
**Status**: 🚧 In Progress
**Focus**: Complete Lambda Architecture by integrating Spark ML batch layer with serving layer

---

## Executive Summary

Sprint 11 completes the Lambda Architecture vision by integrating the validated Spark ML batch processing layer with the real-time serving layer. The validation in Sprint 10 confirmed the Spark ML pipeline works flawlessly in isolation - this sprint connects it to the rest of the system.

### Sprint Goals

| Goal | Effort | Priority | Success Criteria |
|------|--------|----------|------------------|
| Add Spark ML E2E test | 4-6h | High | Test 8 passing in test suite |
| Optimize Kafka read | 6-8h | High | <20s for 1K events (10x faster) |
| Serving layer integration | 12-16h | Critical | Query API reads Parquet files |
| Grafana metrics | 2-3h | Medium | Spark dashboard panel added |
| Documentation | 2-3h | High | Integration guides updated |

**Total Estimated Effort**: 26-36 hours

---

## Context: What We Learned from Validation

### Sprint 10 Validation Results

✅ **Pipeline Status**: Fully functional, production-ready code
✅ **Execution**: Successfully processed 1,000 events in 228 seconds
✅ **ML Results**: Detected 2 anomalies (14.29%) using Isolation Forest
✅ **Output**: Valid Parquet files with 31 features

### Critical Gaps Identified

❌ **Integration Gap**: Parquet output not consumed by serving layer
⚠️ **Performance**: Kafka read takes 209s (91.7% of pipeline time)
❌ **Testing**: Not included in E2E test suite
❌ **Monitoring**: No Grafana visibility

### Current Architecture (Incomplete)

```
           ┌─────────────────┐
           │   Kafka Events  │
           └────────┬────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
  ┌─────────────┐      ┌─────────────┐
  │ Speed Layer │      │ Batch Layer │
  │   (C++)     │      │  (Spark ML) │
  │             │      │             │
  │ ✅ Working  │      │ ✅ Working  │
  └──────┬──────┘      └──────┬──────┘
         │                    │
         │                    ▼
         │            ┌──────────────┐
         │            │  Parquet     │
         │            │  (Training)  │
         │            └──────────────┘
         │                    │
         │                    ❌ DISCONNECTED
         │
         ▼
  ┌────────────────────┐
  │  Serving Layer     │
  │  (Query API)       │
  │                    │
  │  ❌ No batch data  │
  └────────────────────┘
```

---

## Sprint 11 Architecture Goal

```
           ┌─────────────────┐
           │   Kafka Events  │
           └────────┬────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
  ┌─────────────┐      ┌─────────────┐
  │ Speed Layer │      │ Batch Layer │
  │   (C++)     │      │  (Spark ML) │
  │             │      │             │
  │ Real-time   │      │ Historical  │
  │ <1ms        │      │ Deep ML     │
  └──────┬──────┘      └──────┬──────┘
         │                    │
         │                    ▼
         │            ┌──────────────┐
         │            │  Parquet     │
         │            │  Files       │
         │            └──────┬───────┘
         │                   │
         │                   │ ✅ NEW: Read API
         │                   ▼
         │            ┌──────────────┐
         │            │ Batch Anomaly│
         │            │  Predictions │
         │            └──────┬───────┘
         │                   │
         └──────────┬────────┘
                    │
                    ▼
         ┌────────────────────┐
         │  Serving Layer     │
         │  (Query API)       │
         │                    │
         │  • Real-time view  │
         │  • Batch view      │
         │  • Unified query   │
         └──────┬─────────────┘
                │
                ▼
         ┌────────────────────┐
         │  GenAI Assistant   │
         │                    │
         │  "User X has 87    │
         │   real-time events │
         │   and ML flagged   │
         │   them as anomaly  │
         │   (score: 0.92)"   │
         └────────────────────┘
```

---

## Phase 1: E2E Test Integration (4-6 hours)

### Objective
Add Spark ML pipeline to automated E2E test suite to prevent regressions.

### Tasks

#### Task 1.1: Create Test 8 in E2E Suite
**File**: `/tmp/run_e2e_tests.sh`

Add after Test 7:
```bash
# Test 8: Spark ML Training Data Generation
echo "Test 8: Spark ML Training Data Generation..."
cd spark-ml-pipeline

# Clean previous output
rm -rf output/training_data

# Run pipeline with small sample
./scripts/run_pipeline.sh --max-events 100 --output /tmp/spark_test_output > /tmp/spark_pipeline.log 2>&1

# Check exit code
if [ $? -ne 0 ]; then
  echo "❌ FAIL - Spark pipeline execution error"
  cat /tmp/spark_pipeline.log | tail -20
  cd ..
  exit 1
fi

# Validate Parquet output
if [ ! -f "/tmp/spark_test_output/anomaly_report.json" ]; then
  echo "❌ FAIL - No anomaly report generated"
  cd ..
  exit 1
fi

# Validate anomaly report structure
TOTAL_USERS=$(cat /tmp/spark_test_output/anomaly_report.json | jq -r '.total_users')
if [ "$TOTAL_USERS" -gt 0 ]; then
  echo "✅ PASS - Spark ML: $TOTAL_USERS users analyzed"
else
  echo "❌ FAIL - Invalid anomaly report"
  cd ..
  exit 1
fi

cd ..
```

#### Task 1.2: Update Test Count
Update test counter from 7 to 8 tests total.

**Success Criteria**:
- Test 8 passes with 100 events
- Execution time <60 seconds
- Validates Parquet output exists
- Validates anomaly report structure

---

## Phase 2: Kafka Read Optimization (6-8 hours)

### Objective
Reduce Kafka read time from 209s to <20s for small samples (10x speedup).

### Current Issue

```python
# kafka_reader.py:178 (SLOW - reads ALL 9.9M events)
df = self.read_batch(start_offset="earliest")  # ❌ Reads entire topic
df_filtered = df.filter(...)[:limit]            # Then limits
```

**Problem**: Reading 9.9M events to get 1,000 = 99.9% waste

### Solution: Smart Offset Calculation

#### Task 2.1: Add Offset Calculation Method
**File**: `spark-ml-pipeline/src/kafka_reader.py`

Add new method:
```python
def calculate_end_offset_for_limit(
    self,
    start_offset: str,
    max_events: int
) -> str:
    """
    Calculate Kafka end offset to read only max_events.

    Args:
        start_offset: Starting offset (earliest/latest)
        max_events: Maximum events to read

    Returns:
        JSON string with calculated end offsets per partition
    """
    import json
    from kafka import KafkaConsumer, TopicPartition

    consumer = KafkaConsumer(
        bootstrap_servers=self.kafka_config['bootstrap_servers'],
        group_id=f"{self.kafka_config['group_id']}_offset_calc"
    )

    topic = self.kafka_config['topic']
    partitions = consumer.partitions_for_topic(topic)

    # Get current offsets for all partitions
    partition_offsets = {}
    events_per_partition = max_events // len(partitions)

    for partition_id in partitions:
        tp = TopicPartition(topic, partition_id)

        if start_offset == "earliest":
            start = consumer.beginning_offsets([tp])[tp]
        else:
            start = consumer.end_offsets([tp])[tp]

        # Calculate end offset
        end = start + events_per_partition
        partition_offsets[str(partition_id)] = end

    consumer.close()

    # Return JSON format for Spark
    end_offset_json = json.dumps({topic: partition_offsets})
    logger.debug(f"Calculated end offsets: {end_offset_json}")

    return end_offset_json
```

#### Task 2.2: Update read_batch Method
Modify `read_batch()` to use calculated offsets:

```python
def read_batch(
    self,
    start_offset: str = "earliest",
    end_offset: Optional[str] = None,
    max_events: Optional[int] = None
) -> DataFrame:
    """Read a batch of events from Kafka."""

    # NEW: Calculate end offset if max_events specified
    if max_events and not end_offset:
        end_offset = self.calculate_end_offset_for_limit(start_offset, max_events)
        logger.info(f"Optimized read: limiting to ~{max_events} events via offset")

    # Rest of method unchanged...
```

#### Task 2.3: Add kafka-python Dependency
**File**: `spark-ml-pipeline/requirements.txt`

Add:
```txt
kafka-python==2.0.2  # For offset calculation
```

**Success Criteria**:
- 1,000 events read in <20 seconds (down from 209s)
- 10x speedup achieved
- Backward compatible (works without max_events)
- No data loss or skipping

**Performance Target**:
```
Before: 209s to read 9.9M events (then limit to 1K)
After:  <20s to read ~1K events directly
Speedup: 10.5x
```

---

## Phase 3: Serving Layer Integration (12-16 hours)

### Objective
Create Java API endpoint to read Parquet training data and expose batch anomalies.

### Architecture

```
Parquet Files → Java Parquet Reader → REST API → GenAI Assistant
```

### Task 3.1: Add Parquet Dependencies (1 hour)
**File**: `query-api/pom.xml`

Add Apache Parquet and Arrow dependencies:
```xml
<!-- Parquet Support -->
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-hadoop</artifactId>
    <version>1.13.1</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.4</version>
</dependency>
<dependency>
    <groupId>org.apache.arrow</groupId>
    <artifactId>arrow-vector</artifactId>
    <version>15.0.0</version>
</dependency>
```

### Task 3.2: Create Parquet Reader Service (3-4 hours)
**File**: `query-api/src/main/java/com/streamguard/service/TrainingDataService.java`

```java
package com.streamguard.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Service
public class TrainingDataService {

    private final String trainingDataPath;
    private final ObjectMapper objectMapper;

    public TrainingDataService() {
        this.trainingDataPath = System.getenv()
            .getOrDefault("TRAINING_DATA_PATH", "../spark-ml-pipeline/output/training_data");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Read batch anomalies from Parquet files.
     */
    public List<BatchAnomaly> getAnomalies() throws Exception {
        File anomalyDir = new File(trainingDataPath + "/is_anomaly=1");

        if (!anomalyDir.exists()) {
            return new ArrayList<>();
        }

        List<BatchAnomaly> anomalies = new ArrayList<>();

        // Read all parquet files in anomaly partition
        for (File parquetFile : anomalyDir.listFiles()) {
            if (parquetFile.getName().endsWith(".parquet")) {
                anomalies.addAll(readParquetFile(parquetFile));
            }
        }

        return anomalies;
    }

    /**
     * Get batch anomaly for specific user.
     */
    public BatchAnomaly getAnomalyForUser(String userId) throws Exception {
        List<BatchAnomaly> anomalies = getAnomalies();

        return anomalies.stream()
            .filter(a -> a.getUser().equals(userId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Read anomaly report JSON.
     */
    public AnomalyReport getAnomalyReport() throws Exception {
        File reportFile = new File(trainingDataPath + "/anomaly_report.json");

        if (!reportFile.exists()) {
            return null;
        }

        return objectMapper.readValue(reportFile, AnomalyReport.class);
    }

    private List<BatchAnomaly> readParquetFile(File file) throws Exception {
        // Parquet reading implementation
        // (Simplified - actual implementation needs Parquet schema mapping)

        Configuration conf = new Configuration();
        Path path = new Path(file.getAbsolutePath());

        List<BatchAnomaly> anomalies = new ArrayList<>();

        // TODO: Implement Parquet reading with proper schema
        // This requires ParquetReader configuration with Avro/Arrow

        return anomalies;
    }
}
```

### Task 3.3: Create Data Models (1 hour)
**File**: `query-api/src/main/java/com/streamguard/model/BatchAnomaly.java`

```java
package com.streamguard.model;

public class BatchAnomaly {
    private String user;
    private double anomalyScore;
    private int totalEvents;
    private double avgThreatScore;
    private int uniqueIps;
    private double failedAuthRate;

    // Getters and setters...
}
```

**File**: `query-api/src/main/java/com/streamguard/model/AnomalyReport.java`

```java
package com.streamguard.model;

import java.util.List;

public class AnomalyReport {
    private int totalUsers;
    private int anomalousUsers;
    private double anomalyRate;
    private List<BatchAnomaly> topAnomalies;

    // Getters and setters...
}
```

### Task 3.4: Create REST Endpoints (2-3 hours)
**File**: `query-api/src/main/java/com/streamguard/controller/TrainingDataController.java`

```java
package com.streamguard.controller;

import com.streamguard.service.TrainingDataService;
import com.streamguard.model.BatchAnomaly;
import com.streamguard.model.AnomalyReport;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-data")
public class TrainingDataController {

    private final TrainingDataService trainingDataService;

    public TrainingDataController(TrainingDataService trainingDataService) {
        this.trainingDataService = trainingDataService;
    }

    /**
     * GET /api/training-data/anomalies
     * Returns all batch-detected anomalies
     */
    @GetMapping("/anomalies")
    public List<BatchAnomaly> getAnomalies() throws Exception {
        return trainingDataService.getAnomalies();
    }

    /**
     * GET /api/training-data/anomalies/{userId}
     * Returns batch anomaly for specific user
     */
    @GetMapping("/anomalies/{userId}")
    public BatchAnomaly getAnomalyForUser(@PathVariable String userId) throws Exception {
        return trainingDataService.getAnomalyForUser(userId);
    }

    /**
     * GET /api/training-data/report
     * Returns anomaly detection summary report
     */
    @GetMapping("/report")
    public AnomalyReport getReport() throws Exception {
        return trainingDataService.getAnomalyReport();
    }
}
```

### Task 3.5: Update GenAI Assistant (3-4 hours)
**File**: `genai-assistant/app/services/java_api.py`

Add method to fetch batch anomalies:
```python
async def get_batch_anomaly(self, user_id: str) -> Optional[Dict[str, Any]]:
    """
    Fetch batch ML anomaly detection for user.

    Returns:
        Batch anomaly data if user flagged, None otherwise
    """
    async with httpx.AsyncClient(timeout=self.timeout) as client:
        try:
            response = await client.get(
                f"{self.base_url}/api/training-data/anomalies/{user_id}"
            )

            if response.status_code == 200:
                return response.json()
            else:
                return None

        except Exception as e:
            logger.warning(f"Batch anomaly fetch failed: {e}")
            return None
```

**File**: `genai-assistant/app/services/assistant.py`

Update `answer_query()` to include batch anomalies:
```python
async def answer_query(self, question: str, user_id: Optional[str] = None, ...) -> Dict[str, Any]:
    """Answer security question with context."""

    # Existing event fetching...

    # NEW: Fetch batch anomaly if user specified
    batch_anomaly = None
    if user_id:
        batch_anomaly = await self.java_api.get_batch_anomaly(user_id)

    # Build context with batch anomaly
    context = self._build_context(
        events=events,
        threat_intel=threat_intel,
        batch_anomaly=batch_anomaly  # NEW
    )

    # Update prompt to mention batch analysis
    if batch_anomaly:
        context += f"\n\nBatch ML Analysis: User '{user_id}' flagged as anomaly "
        context += f"(score: {batch_anomaly['anomaly_score']:.2f}) based on "
        context += f"{batch_anomaly['total_events']} historical events."

    # Rest of method...
```

### Task 3.6: Update Health Check (1 hour)
**File**: `genai-assistant/app/main.py`

Add training data check to health endpoint:
```python
@app.get("/health")
async def health_check():
    """Health check with batch layer status."""

    # Existing checks...

    # NEW: Check training data availability
    training_data_available = False
    try:
        response = await httpx.get(f"{JAVA_API_URL}/api/training-data/report", timeout=2.0)
        training_data_available = response.status_code == 200
    except:
        pass

    return {
        "status": "healthy",
        "services": {
            "java_api": java_api_healthy,
            "rag_service": rag_healthy,
            "openai": openai_healthy,
            "training_data": training_data_available  # NEW
        }
    }
```

**Success Criteria**:
- Query API serves batch anomalies via REST
- GenAI Assistant fetches batch data for users
- Health check includes training data status
- Sample query: "Is user manager an anomaly?" returns batch ML score

---

## Phase 4: Grafana Metrics (2-3 hours)

### Objective
Add Spark ML metrics to Grafana dashboard for monitoring.

### Task 4.1: Expose Spark Metrics Endpoint (1 hour)
**File**: `query-api/src/main/java/com/streamguard/controller/MetricsController.java`

Add batch metrics:
```java
@GetMapping("/metrics")
public String getMetrics() {
    StringBuilder metrics = new StringBuilder();

    // Existing metrics...

    // NEW: Batch anomaly metrics
    try {
        AnomalyReport report = trainingDataService.getAnomalyReport();
        if (report != null) {
            metrics.append("# HELP training_data_users_total Total users analyzed\n");
            metrics.append("# TYPE training_data_users_total gauge\n");
            metrics.append("training_data_users_total " + report.getTotalUsers() + "\n");

            metrics.append("# HELP training_data_anomalies_total Anomalous users detected\n");
            metrics.append("# TYPE training_data_anomalies_total gauge\n");
            metrics.append("training_data_anomalies_total " + report.getAnomalousUsers() + "\n");

            metrics.append("# HELP training_data_anomaly_rate Anomaly detection rate\n");
            metrics.append("# TYPE training_data_anomaly_rate gauge\n");
            metrics.append("training_data_anomaly_rate " + report.getAnomalyRate() + "\n");
        }
    } catch (Exception e) {
        logger.error("Failed to get training data metrics", e);
    }

    return metrics.toString();
}
```

### Task 4.2: Update Grafana Dashboard (1-2 hours)
**File**: `monitoring/grafana/dashboards/streamguard-genai.json`

Add new panel for Spark ML metrics:
```json
{
  "title": "Batch ML Anomaly Detection",
  "gridPos": {"h": 8, "w": 12, "x": 0, "y": 24},
  "targets": [
    {
      "expr": "training_data_users_total",
      "legendFormat": "Total Users Analyzed"
    },
    {
      "expr": "training_data_anomalies_total",
      "legendFormat": "Anomalies Detected"
    },
    {
      "expr": "training_data_anomaly_rate * 100",
      "legendFormat": "Anomaly Rate (%)"
    }
  ]
}
```

**Success Criteria**:
- Grafana displays Spark ML metrics
- Updates when new training data generated
- Shows anomaly detection rate over time

---

## Phase 5: Documentation (2-3 hours)

### Task 5.1: Update Architecture Docs
**File**: `docs/product/guides/ARCHITECTURE.md`

Add Lambda Architecture section with integration details.

### Task 5.2: Update Integration Guide
**File**: `docs/integrations/SPARK_INTEGRATION.md`

Add:
- Serving layer integration instructions
- API endpoint documentation
- Example queries

### Task 5.3: Update Quick Start
**File**: `docs/product/guides/QUICK_START.md`

Add Spark ML pipeline to startup instructions.

---

## Testing Strategy

### Unit Tests (Future Sprint)
- Parquet reader service tests
- Training data controller tests
- GenAI assistant batch anomaly tests

### Integration Tests
- E2E test with Spark ML (Test 8)
- Query API → Parquet file reading
- GenAI Assistant → Batch anomaly fetching

### Performance Tests
- Kafka read optimization (<20s for 1K events)
- Parquet read latency (<500ms)
- End-to-end query with batch data (<15s)

---

## Success Criteria

### Functional Requirements
✅ Spark ML pipeline integrated with E2E tests
✅ Kafka read optimized (10x faster)
✅ Query API reads Parquet training data
✅ GenAI Assistant uses batch anomalies in responses
✅ Grafana displays Spark ML metrics

### Performance Requirements
✅ Kafka read: <20s for 1,000 events
✅ Parquet read: <500ms per query
✅ Health check: includes training data status
✅ No regressions in existing E2E tests

### Documentation Requirements
✅ Architecture updated with Lambda integration
✅ API endpoints documented
✅ Integration guide updated

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Parquet reading complexity | Medium | High | Use Arrow for fast reads |
| Java Parquet dependencies | Low | Medium | Test with sample files first |
| Performance regression | Low | High | Benchmark before/after |
| E2E test flakiness | Medium | Low | Add retry logic |

---

## Timeline

**Week 1** (Days 1-3):
- Day 1: Phase 1 (E2E test) + Phase 2 start (Kafka optimization)
- Day 2: Phase 2 complete + Phase 3 start (Java API)
- Day 3: Phase 3 continue (Parquet reader)

**Week 2** (Days 4-5):
- Day 4: Phase 3 complete (GenAI integration) + Phase 4 (Grafana)
- Day 5: Phase 5 (Documentation) + Testing + Handoff

**Total**: 5 days (26-36 hours)

---

## Next Steps

1. Create feature branch: `git checkout -b sprint11-lambda-integration`
2. Start with Phase 1: E2E test integration
3. Execute phases sequentially
4. Test integration after each phase
5. Create handoff document at completion

---

**Plan Created**: October 27, 2025
**Sprint Start**: October 27, 2025
**Estimated Completion**: November 1, 2025
