# Sprint 12 Plan: Complete Lambda Architecture Integration

**Sprint Duration**: Sprint 12
**Start Date**: October 27, 2025
**Status**: 🚧 In Progress
**Focus**: Connect Spark ML batch layer to serving layer (complete Lambda Architecture)

---

## Executive Summary

Sprint 12 completes the Lambda Architecture by integrating the optimized Spark ML batch layer (Sprint 11) with the serving layer. This enables the GenAI Assistant to use batch ML insights alongside real-time data, providing comprehensive security analysis.

### Sprint Context

**Sprint 11 Achievements**:
- ✅ Spark ML pipeline optimized (13.4x faster)
- ✅ E2E test coverage added (Test 8)
- ✅ Production-ready batch processing

**Sprint 12 Goal**: Connect batch layer → serving layer → GenAI Assistant

---

## Sprint Goals

| Phase | Goal | Est. Hours | Priority |
|-------|------|------------|----------|
| **Phase 3** | Serving Layer Integration | 12-16h | Critical |
| **Phase 4** | Grafana Metrics | 2-3h | Medium |
| **Phase 5** | Documentation | 2-3h | High |

**Total Estimated Effort**: 16-22 hours

---

## Architecture: Before vs After

### Before Sprint 12 (Current)

```
Kafka Events
    ↓
┌────────────────────────────────┐
│   Speed Layer (C++)            │
│   Real-time: <1ms              │
└───────────┬────────────────────┘
            │
            ↓
┌────────────────────────────────┐
│   Batch Layer (Spark ML)       │
│   Historical: ~17s (optimized) │
└───────────┬────────────────────┘
            │
            ↓
    Parquet Files
            │
            ❌ DISCONNECTED

┌────────────────────────────────┐
│   Serving Layer (Query API)    │
│   - Real-time events ✅        │
│   - Batch anomalies ❌         │
└───────────┬────────────────────┘
            │
            ↓
┌────────────────────────────────┐
│   GenAI Assistant              │
│   - Uses real-time only        │
└────────────────────────────────┘
```

### After Sprint 12 (Target)

```
Kafka Events
    ↓
┌────────────────────────────────┐
│   Speed Layer (C++)            │
│   Real-time: <1ms              │
└───────────┬────────────────────┘
            │
            ↓
┌────────────────────────────────┐
│   Batch Layer (Spark ML)       │
│   Historical: ~17s             │
└───────────┬────────────────────┘
            │
            ↓
    Parquet Files
            │
            ✅ CONNECTED (NEW)
            │
            ↓
┌────────────────────────────────┐
│   Serving Layer (Query API)    │
│   - Real-time events ✅        │
│   - Batch anomalies ✅ NEW     │
│   - /api/training-data/* NEW   │
└───────────┬────────────────────┘
            │
            ↓
┌────────────────────────────────┐
│   GenAI Assistant              │
│   - Real-time + Batch ML ✅    │
│   "User X: 87 events + ML      │
│    flagged as anomaly (0.92)"  │
└────────────────────────────────┘
```

---

## Phase 3: Serving Layer Integration (12-16 hours)

### Objective
Create Java API to read Parquet training data and expose batch anomalies to GenAI Assistant.

### Task 3.1: Add Parquet Dependencies (1 hour)

**File**: `query-api/pom.xml`

**Dependencies to Add**:
```xml
<!-- Apache Parquet for reading ML training data -->
<dependency>
    <groupId>org.apache.parquet</groupId>
    <artifactId>parquet-avro</artifactId>
    <version>1.13.1</version>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-common</artifactId>
    <version>3.3.6</version>
    <exclusions>
        <!-- Exclude conflicting dependencies -->
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-reload4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.apache.hadoop</groupId>
    <artifactId>hadoop-mapreduce-client-core</artifactId>
    <version>3.3.6</version>
</dependency>
```

**Success Criteria**:
- Maven build succeeds
- No dependency conflicts
- Parquet classes available

---

### Task 3.2: Create Data Models (1 hour)

**File**: `query-api/src/main/java/com/streamguard/model/BatchAnomaly.java`

```java
package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BatchAnomaly {
    private String user;

    @JsonProperty("anomaly_score_normalized")
    private Double anomalyScore;

    @JsonProperty("total_events")
    private Integer totalEvents;

    @JsonProperty("avg_threat_score")
    private Double avgThreatScore;

    @JsonProperty("unique_ips")
    private Integer uniqueIps;

    @JsonProperty("failed_auth_rate")
    private Double failedAuthRate;

    @JsonProperty("is_anomaly")
    private Integer isAnomaly;

    // Constructors
    public BatchAnomaly() {}

    public BatchAnomaly(String user, Double anomalyScore, Integer totalEvents,
                       Double avgThreatScore, Integer uniqueIps,
                       Double failedAuthRate, Integer isAnomaly) {
        this.user = user;
        this.anomalyScore = anomalyScore;
        this.totalEvents = totalEvents;
        this.avgThreatScore = avgThreatScore;
        this.uniqueIps = uniqueIps;
        this.failedAuthRate = failedAuthRate;
        this.isAnomaly = isAnomaly;
    }

    // Getters and setters
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public Double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(Double anomalyScore) { this.anomalyScore = anomalyScore; }

    public Integer getTotalEvents() { return totalEvents; }
    public void setTotalEvents(Integer totalEvents) { this.totalEvents = totalEvents; }

    public Double getAvgThreatScore() { return avgThreatScore; }
    public void setAvgThreatScore(Double avgThreatScore) { this.avgThreatScore = avgThreatScore; }

    public Integer getUniqueIps() { return uniqueIps; }
    public void setUniqueIps(Integer uniqueIps) { this.uniqueIps = uniqueIps; }

    public Double getFailedAuthRate() { return failedAuthRate; }
    public void setFailedAuthRate(Double failedAuthRate) { this.failedAuthRate = failedAuthRate; }

    public Integer getIsAnomaly() { return isAnomaly; }
    public void setIsAnomaly(Integer isAnomaly) { this.isAnomaly = isAnomaly; }
}
```

**File**: `query-api/src/main/java/com/streamguard/model/AnomalyReport.java`

```java
package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AnomalyReport {
    @JsonProperty("total_users")
    private Integer totalUsers;

    @JsonProperty("anomalous_users")
    private Integer anomalousUsers;

    @JsonProperty("anomaly_rate")
    private Double anomalyRate;

    @JsonProperty("top_anomalies")
    private List<BatchAnomaly> topAnomalies;

    // Constructors
    public AnomalyReport() {}

    public AnomalyReport(Integer totalUsers, Integer anomalousUsers,
                        Double anomalyRate, List<BatchAnomaly> topAnomalies) {
        this.totalUsers = totalUsers;
        this.anomalousUsers = anomalousUsers;
        this.anomalyRate = anomalyRate;
        this.topAnomalies = topAnomalies;
    }

    // Getters and setters
    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }

    public Integer getAnomalousUsers() { return anomalousUsers; }
    public void setAnomalousUsers(Integer anomalousUsers) { this.anomalousUsers = anomalousUsers; }

    public Double getAnomalyRate() { return anomalyRate; }
    public void setAnomalyRate(Double anomalyRate) { this.anomalyRate = anomalyRate; }

    public List<BatchAnomaly> getTopAnomalies() { return topAnomalies; }
    public void setTopAnomalies(List<BatchAnomaly> topAnomalies) { this.topAnomalies = topAnomalies; }
}
```

---

### Task 3.3: Create TrainingDataService (4-5 hours)

**File**: `query-api/src/main/java/com/streamguard/service/TrainingDataService.java`

**Approach**: Use Parquet-Avro for reading Parquet files

```java
package com.streamguard.service;

import com.streamguard.model.AnomalyReport;
import com.streamguard.model.BatchAnomaly;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingDataService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingDataService.class);
    private final String trainingDataPath;
    private final ObjectMapper objectMapper;

    public TrainingDataService() {
        this.trainingDataPath = System.getenv()
            .getOrDefault("TRAINING_DATA_PATH",
                         "../spark-ml-pipeline/output/training_data");
        this.objectMapper = new ObjectMapper();

        logger.info("TrainingDataService initialized with path: {}", trainingDataPath);
    }

    /**
     * Read anomaly report JSON summary.
     */
    public AnomalyReport getAnomalyReport() throws IOException {
        File reportFile = new File(trainingDataPath + "/anomaly_report.json");

        if (!reportFile.exists()) {
            logger.warn("Anomaly report not found: {}", reportFile.getPath());
            return null;
        }

        logger.debug("Reading anomaly report from: {}", reportFile.getPath());
        return objectMapper.readValue(reportFile, AnomalyReport.class);
    }

    /**
     * Read all batch anomalies from Parquet files.
     */
    public List<BatchAnomaly> getAnomalies() throws IOException {
        File anomalyDir = new File(trainingDataPath + "/is_anomaly=1");

        if (!anomalyDir.exists() || !anomalyDir.isDirectory()) {
            logger.warn("Anomaly directory not found: {}", anomalyDir.getPath());
            return new ArrayList<>();
        }

        List<BatchAnomaly> anomalies = new ArrayList<>();

        // Find all .parquet files
        File[] parquetFiles = anomalyDir.listFiles(
            (dir, name) -> name.endsWith(".parquet")
        );

        if (parquetFiles == null || parquetFiles.length == 0) {
            logger.warn("No parquet files found in: {}", anomalyDir.getPath());
            return anomalies;
        }

        logger.debug("Found {} parquet files", parquetFiles.length);

        // Read each parquet file
        for (File parquetFile : parquetFiles) {
            try {
                anomalies.addAll(readParquetFile(parquetFile));
            } catch (Exception e) {
                logger.error("Failed to read parquet file: {}", parquetFile.getName(), e);
            }
        }

        logger.info("Loaded {} batch anomalies", anomalies.size());
        return anomalies;
    }

    /**
     * Get batch anomaly for specific user.
     */
    public BatchAnomaly getAnomalyForUser(String userId) throws IOException {
        List<BatchAnomaly> anomalies = getAnomalies();

        return anomalies.stream()
            .filter(a -> userId.equals(a.getUser()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Read a single Parquet file and convert to BatchAnomaly objects.
     */
    private List<BatchAnomaly> readParquetFile(File file) throws IOException {
        List<BatchAnomaly> anomalies = new ArrayList<>();

        Configuration conf = new Configuration();
        Path path = new Path(file.getAbsolutePath());

        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(path)
                .withConf(conf)
                .build()) {

            GenericRecord record;
            while ((record = reader.read()) != null) {
                anomalies.add(convertRecordToAnomaly(record));
            }
        }

        return anomalies;
    }

    /**
     * Convert Avro GenericRecord to BatchAnomaly.
     */
    private BatchAnomaly convertRecordToAnomaly(GenericRecord record) {
        BatchAnomaly anomaly = new BatchAnomaly();

        // Extract fields (handle nulls gracefully)
        anomaly.setUser(getStringValue(record, "user"));
        anomaly.setAnomalyScore(getDoubleValue(record, "anomaly_score_normalized"));
        anomaly.setTotalEvents(getIntValue(record, "total_events"));
        anomaly.setAvgThreatScore(getDoubleValue(record, "avg_threat_score"));
        anomaly.setUniqueIps(getIntValue(record, "unique_ips"));
        anomaly.setFailedAuthRate(getDoubleValue(record, "failed_auth_rate"));
        anomaly.setIsAnomaly(getIntValue(record, "is_anomaly"));

        return anomaly;
    }

    // Helper methods for safe field extraction
    private String getStringValue(GenericRecord record, String field) {
        Object value = record.get(field);
        return value != null ? value.toString() : null;
    }

    private Double getDoubleValue(GenericRecord record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Integer getIntValue(GenericRecord record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Check if training data is available.
     */
    public boolean isTrainingDataAvailable() {
        File reportFile = new File(trainingDataPath + "/anomaly_report.json");
        return reportFile.exists();
    }
}
```

---

### Task 3.4: Create REST Controller (2 hours)

**File**: `query-api/src/main/java/com/streamguard/controller/TrainingDataController.java`

```java
package com.streamguard.controller;

import com.streamguard.model.AnomalyReport;
import com.streamguard.model.BatchAnomaly;
import com.streamguard.service.TrainingDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/training-data")
public class TrainingDataController {

    private static final Logger logger = LoggerFactory.getLogger(TrainingDataController.class);
    private final TrainingDataService trainingDataService;

    public TrainingDataController(TrainingDataService trainingDataService) {
        this.trainingDataService = trainingDataService;
        logger.info("TrainingDataController initialized");
    }

    /**
     * GET /api/training-data/report
     * Returns anomaly detection summary report.
     */
    @GetMapping("/report")
    public ResponseEntity<AnomalyReport> getReport() {
        try {
            logger.debug("GET /api/training-data/report");

            AnomalyReport report = trainingDataService.getAnomalyReport();

            if (report == null) {
                logger.warn("Anomaly report not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            logger.info("Returned anomaly report: {} users, {} anomalies",
                       report.getTotalUsers(), report.getAnomalousUsers());

            return ResponseEntity.ok(report);

        } catch (IOException e) {
            logger.error("Failed to read anomaly report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/anomalies
     * Returns all batch-detected anomalies.
     */
    @GetMapping("/anomalies")
    public ResponseEntity<List<BatchAnomaly>> getAnomalies() {
        try {
            logger.debug("GET /api/training-data/anomalies");

            List<BatchAnomaly> anomalies = trainingDataService.getAnomalies();

            logger.info("Returned {} batch anomalies", anomalies.size());
            return ResponseEntity.ok(anomalies);

        } catch (IOException e) {
            logger.error("Failed to read batch anomalies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/anomalies/{userId}
     * Returns batch anomaly for specific user.
     */
    @GetMapping("/anomalies/{userId}")
    public ResponseEntity<BatchAnomaly> getAnomalyForUser(@PathVariable String userId) {
        try {
            logger.debug("GET /api/training-data/anomalies/{}", userId);

            BatchAnomaly anomaly = trainingDataService.getAnomalyForUser(userId);

            if (anomaly == null) {
                logger.debug("No batch anomaly found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            logger.info("Returned batch anomaly for user: {} (score: {})",
                       userId, anomaly.getAnomalyScore());

            return ResponseEntity.ok(anomaly);

        } catch (IOException e) {
            logger.error("Failed to get anomaly for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/training-data/health
     * Check if training data is available.
     */
    @GetMapping("/health")
    public ResponseEntity<String> getHealth() {
        boolean available = trainingDataService.isTrainingDataAvailable();

        if (available) {
            return ResponseEntity.ok("Training data available");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Training data not available");
        }
    }
}
```

---

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
                logger.info(f"Batch anomaly found for user: {user_id}")
                return response.json()
            elif response.status_code == 404:
                logger.debug(f"No batch anomaly for user: {user_id}")
                return None
            else:
                logger.warning(f"Unexpected status fetching batch anomaly: {response.status_code}")
                return None

        except Exception as e:
            logger.warning(f"Batch anomaly fetch failed for {user_id}: {e}")
            return None


async def get_batch_anomaly_report(self) -> Optional[Dict[str, Any]]:
    """
    Fetch batch ML anomaly detection summary report.

    Returns:
        Anomaly report with total users and top anomalies
    """
    async with httpx.AsyncClient(timeout=self.timeout) as client:
        try:
            response = await client.get(
                f"{self.base_url}/api/training-data/report"
            )

            if response.status_code == 200:
                return response.json()
            else:
                logger.warning(f"Anomaly report not available: {response.status_code}")
                return None

        except Exception as e:
            logger.warning(f"Anomaly report fetch failed: {e}")
            return None
```

**File**: `genai-assistant/app/services/assistant.py`

Update `answer_query()` to include batch anomalies:

```python
async def answer_query(
    self,
    question: str,
    user_id: Optional[str] = None,
    include_threat_intel: bool = True,
    max_events: int = 100
) -> Dict[str, Any]:
    """Answer security question with real-time + batch ML context."""

    logger.info(f"Answering query: {question} (user={user_id})")

    # Fetch real-time events
    if user_id:
        events = await self.java_api.get_events_by_user(user_id, limit=max_events)
    else:
        events = await self.java_api.get_recent_events(limit=max_events)

    # NEW: Fetch batch anomaly if user specified
    batch_anomaly = None
    if user_id:
        batch_anomaly = await self.java_api.get_batch_anomaly(user_id)

    # Fetch threat intel if requested
    threat_intel = []
    if include_threat_intel:
        threat_intel = await self.rag_service.query_threats(question)

    # Build context with batch anomaly
    context = self._build_context(
        events=events,
        threat_intel=threat_intel,
        batch_anomaly=batch_anomaly  # NEW
    )

    # Generate answer
    answer = await self._generate_answer(question, context)

    return {
        "answer": answer,
        "supporting_events": events[:10],
        "threat_intel": threat_intel,
        "batch_anomaly": batch_anomaly,  # NEW
        "sources_used": self._get_sources_used(events, threat_intel, batch_anomaly)
    }


def _build_context(
    self,
    events: List[Dict],
    threat_intel: List[Dict],
    batch_anomaly: Optional[Dict] = None
) -> str:
    """Build context string for LLM with batch ML insights."""

    context_parts = []

    # Real-time events
    if events:
        context_parts.append(f"Real-time Events ({len(events)} events):")
        context_parts.append(json.dumps(events[:20], indent=2))

    # NEW: Batch ML anomaly
    if batch_anomaly:
        context_parts.append("\nBatch ML Analysis:")
        context_parts.append(
            f"User '{batch_anomaly['user']}' flagged as anomaly by batch ML "
            f"(score: {batch_anomaly['anomaly_score']:.2f}). "
            f"Based on {batch_anomaly['total_events']} historical events with "
            f"avg threat score: {batch_anomaly['avg_threat_score']:.2f}."
        )

    # Threat intel
    if threat_intel:
        context_parts.append(f"\nThreat Intelligence ({len(threat_intel)} threats):")
        context_parts.append(json.dumps(threat_intel[:10], indent=2))

    return "\n\n".join(context_parts)


def _get_sources_used(
    self,
    events: List[Dict],
    threat_intel: List[Dict],
    batch_anomaly: Optional[Dict]
) -> List[str]:
    """Get list of data sources used."""
    sources = []
    if events:
        sources.append("real-time-events")
    if threat_intel:
        sources.append("rag")
    if batch_anomaly:
        sources.append("batch-ml")  # NEW
    return sources
```

---

### Task 3.6: Update Health Check (1 hour)

**File**: `genai-assistant/app/main.py`

Update health endpoint:

```python
@app.get("/health")
async def health_check():
    """Health check with batch layer status."""

    # Existing checks...
    java_api_healthy = await check_java_api()
    rag_healthy = await check_rag_service()
    openai_healthy = await check_openai()

    # NEW: Check training data availability
    training_data_available = False
    try:
        async with httpx.AsyncClient(timeout=2.0) as client:
            response = await client.get(f"{JAVA_API_URL}/api/training-data/health")
            training_data_available = response.status_code == 200
    except:
        pass

    return {
        "status": "healthy" if all([java_api_healthy, rag_healthy, openai_healthy]) else "degraded",
        "services": {
            "java_api": java_api_healthy,
            "rag_service": rag_healthy,
            "openai": openai_healthy,
            "training_data": training_data_available  # NEW
        },
        "lambda_architecture": {
            "speed_layer": java_api_healthy,
            "batch_layer": training_data_available,
            "serving_layer": java_api_healthy
        }
    }
```

---

## Phase 4: Grafana Metrics (2-3 hours)

### Task 4.1: Expose Spark Metrics (1 hour)

**File**: `query-api/src/main/java/com/streamguard/controller/MetricsController.java`

Add batch metrics to existing metrics endpoint:

```java
@GetMapping(value = "/metrics", produces = "text/plain")
public String getMetrics() {
    StringBuilder metrics = new StringBuilder();

    // ... existing metrics ...

    // NEW: Batch anomaly metrics
    try {
        if (trainingDataService.isTrainingDataAvailable()) {
            AnomalyReport report = trainingDataService.getAnomalyReport();

            if (report != null) {
                metrics.append("# HELP training_data_users_total Total users analyzed in batch ML\n");
                metrics.append("# TYPE training_data_users_total gauge\n");
                metrics.append("training_data_users_total ")
                      .append(report.getTotalUsers()).append("\n");

                metrics.append("# HELP training_data_anomalies_total Anomalous users detected by batch ML\n");
                metrics.append("# TYPE training_data_anomalies_total gauge\n");
                metrics.append("training_data_anomalies_total ")
                      .append(report.getAnomalousUsers()).append("\n");

                metrics.append("# HELP training_data_anomaly_rate Batch ML anomaly detection rate\n");
                metrics.append("# TYPE training_data_anomaly_rate gauge\n");
                metrics.append("training_data_anomaly_rate ")
                      .append(report.getAnomalyRate()).append("\n");
            }
        }
    } catch (Exception e) {
        logger.error("Failed to get training data metrics", e);
    }

    return metrics.toString();
}
```

### Task 4.2: Update Grafana Dashboard (1-2 hours)

**File**: `monitoring/grafana/dashboards/streamguard-genai.json`

Add new panel for Spark ML metrics (append to existing panels array).

---

## Phase 5: Documentation (2-3 hours)

### Files to Update

1. **docs/product/guides/ARCHITECTURE.md**
   - Add Lambda Architecture diagram with full integration
   - Document batch layer → serving layer connection

2. **docs/integrations/SPARK_INTEGRATION.md**
   - Add serving layer integration section
   - Document REST API endpoints
   - Add usage examples

3. **docs/product/guides/QUICK_START.md**
   - Update API examples with batch anomaly queries

4. **README.md**
   - Update architecture overview

---

## Success Criteria

### Functional Requirements
✅ Query API reads Parquet files
✅ REST endpoints serve batch anomalies
✅ GenAI Assistant includes batch ML in responses
✅ Health check reports training data status
✅ Grafana displays Spark ML metrics

### Integration Tests
✅ GET /api/training-data/report returns JSON
✅ GET /api/training-data/anomalies returns list
✅ GET /api/training-data/anomalies/{userId} returns user anomaly
✅ GenAI query includes batch_anomaly in response
✅ Health check shows training_data: true

### Performance Requirements
✅ Parquet read: <500ms
✅ No degradation to existing endpoints
✅ Health check: <2s response time

---

## Testing Plan

### Unit Tests (Optional - Future)
- TrainingDataService.getAnomalies()
- BatchAnomaly model serialization
- Controller endpoint validation

### Integration Tests (Manual)

1. **Test Parquet Reading**:
```bash
curl http://localhost:8081/api/training-data/report | jq
# Expected: JSON with total_users, anomalous_users
```

2. **Test User Anomaly Fetch**:
```bash
curl http://localhost:8081/api/training-data/anomalies/manager | jq
# Expected: BatchAnomaly JSON with score: 1.0
```

3. **Test GenAI Integration**:
```bash
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Is user manager anomalous?", "user_id": "manager"}'
# Expected: Response includes batch_anomaly field
```

4. **Test Health Check**:
```bash
curl http://localhost:8002/health | jq
# Expected: training_data: true
```

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Parquet dependency conflicts | Medium | High | Test thoroughly, exclude conflicts |
| Large Parquet files slow | Low | Medium | Add caching layer |
| Schema mismatch | Medium | High | Validate with sample data first |
| GenAI timeout | Low | Low | Graceful degradation (no batch data) |

---

## Timeline

**Day 1** (8 hours):
- Add Parquet dependencies (1h)
- Create data models (1h)
- Create TrainingDataService (4-5h)
- Initial testing (1-2h)

**Day 2** (8 hours):
- Create REST controller (2h)
- Update GenAI Assistant (3-4h)
- Update health check (1h)
- Integration testing (2h)

**Day 3** (4-6 hours):
- Add Grafana metrics (2-3h)
- Update documentation (2-3h)
- Final E2E testing
- Create Sprint 12 handoff

**Total**: 20-22 hours (2.5-3 days)

---

## Next Steps

1. Start with Task 3.1: Add Parquet dependencies
2. Create data models (Task 3.2)
3. Implement TrainingDataService (Task 3.3)
4. Continue sequentially through tasks

---

**Plan Created**: October 27, 2025
**Sprint Start**: October 27, 2025
**Estimated Completion**: October 30, 2025
