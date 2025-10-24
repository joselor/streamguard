# StreamGuard API Reference

Complete REST API documentation for StreamGuard Query API.

## Base URL

```
http://localhost:8081/api
```

## Authentication

Currently no authentication required. For production, implement API key authentication.

---

## Events API

### Get Latest Events

```http
GET /events?limit={limit}
```

**Parameters:**
- `limit` (optional, default: 100): Maximum number of events to return

**Response:**
```json
[
  {
    "eventId": "evt_1696723200_001",
    "user": "alice",
    "timestamp": 1696723200000,
    "type": "LOGIN_FAILED",
    "sourceIp": "10.0.1.50",
    "geoLocation": "San Francisco, CA",
    "threatScore": 0.45,
    "metadata": {}
  }
]
```

### Get Event by ID

```http
GET /events/{eventId}
```

**Response:** Single event object or 404

### Get Events by Threat Score

```http
GET /events/threat-score?min_score={score}&limit={limit}
```

**Parameters:**
- `min_score` (required): Minimum threat score (0.0-1.0)
- `limit` (optional, default: 100)

### Get Events by Time Range

```http
GET /events/time-range?start_time={start}&end_time={end}&limit={limit}
```

**Parameters:**
- `start_time` (required): Unix timestamp in milliseconds
- `end_time` (required): Unix timestamp in milliseconds
- `limit` (optional, default: 100)

### Get Event Count

```http
GET /events/count
```

**Response:**
```json
1245678
```

---

## Anomalies API

### Get Latest Anomalies

```http
GET /anomalies?limit={limit}
```

**Response:**
```json
[
  {
    "eventId": "evt_1696723200_001",
    "user": "alice",
    "timestamp": 1696723200000,
    "anomalyScore": 0.73,
    "timeAnomaly": 0.15,
    "ipAnomaly": 0.98,
    "locationAnomaly": 0.08,
    "typeAnomaly": 0.05,
    "failureAnomaly": 0.62,
    "reasons": [
      "Unusual IP address",
      "High failure rate"
    ]
  }
]
```

### Get Anomaly by Event ID

```http
GET /anomalies/{eventId}
```

### Get Anomalies by User

```http
GET /anomalies/user/{user}?limit={limit}
```

### Get High-Score Anomalies

```http
GET /anomalies/high-score?threshold={threshold}&limit={limit}
```

**Parameters:**
- `threshold` (optional, default: 0.7): Minimum anomaly score
- `limit` (optional, default: 100)

### Get Anomalies by Time Range

```http
GET /anomalies/time-range?start_time={start}&end_time={end}&limit={limit}
```

### Get Anomaly Count

```http
GET /anomalies/count
```

### Get Recent Anomalies

```http
GET /anomalies/recent?limit={limit}
```

**Note:** Alias for `GET /anomalies` for consistency with other endpoints.

---

## Analyses API

### Get Latest AI Analyses

```http
GET /analyses?limit={limit}
```

**Response:**
```json
[
  {
    "eventId": "evt_1696723200_001",
    "severity": "MEDIUM",
    "confidence": 0.85,
    "indicators": ["Unknown IP", "Multiple failures"],
    "summary": "Potential brute force attack",
    "recommendation": "Block IP after 3 more failures",
    "analyzedAt": 1696723201500
  }
]
```

### Get Analysis by Event ID

```http
GET /analyses/{eventId}
```

### Get Analyses by Severity

```http
GET /analyses/severity/{severity}?limit={limit}
```

**Severity values:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

### Get Analysis Count

```http
GET /analyses/count
```

### Get Recent Analyses

```http
GET /analyses/recent?limit={limit}
```

**Note:** Alias for `GET /analyses` for consistency with other endpoints.

---

## Statistics API

### Get Summary Statistics

```http
GET /stats/summary
```

**Response:**
```json
{
  "totalEvents": 1245678,
  "highThreatEvents": 12456,
  "averageThreatScore": 0.35,
  "totalAnalyses": 1245678,
  "totalAnomalies": 8542
}
```

---

## Health & Status API

### Basic Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": 1696723200000
}
```

### Detailed System Status

```http
GET /health/status
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": 1696723200000,
  "database": {
    "connected": true,
    "eventCount": 125000,
    "anomalyCount": 1250,
    "analysisCount": 5000
  },
  "features": {
    "eventsAvailable": true,
    "anomalyDetectionAvailable": true,
    "aiAnalysisAvailable": true
  },
  "warnings": {
    "aiAnalysis": "No AI analyses found - check OPENAI_API_KEY configuration"
  }
}
```

### Readiness Check

```http
GET /health/ready
```

Returns 200 if system is ready, 503 if not ready.

---

## Swagger UI

Interactive API documentation:

```
http://localhost:8081/swagger-ui.html
```

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid parameter",
  "message": "threshold must be between 0.0 and 1.0"
}
```

### 404 Not Found
```json
{
  "error": "Resource not found",
  "message": "Event with ID evt_123 not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "message": "RocksDB read error"
}
```

---

## Rate Limiting

No rate limiting currently implemented. Recommended for production.

---

## Example Queries

### Using curl

```bash
# Get latest 10 events
curl 'http://localhost:8081/api/events?limit=10'

# Get high-risk anomalies
curl 'http://localhost:8081/api/anomalies/high-score?threshold=0.8'

# Get critical AI analyses
curl 'http://localhost:8081/api/analyses/severity/CRITICAL'

# Get events from last hour
START=$(date -d '1 hour ago' +%s)000
END=$(date +%s)000
curl "http://localhost:8081/api/events/time-range?start_time=$START&end_time=$END"
```

### Using Python

```python
import requests

BASE_URL = "http://localhost:8081/api"

# Get anomalies
response = requests.get(f"{BASE_URL}/anomalies/high-score", params={
    "threshold": 0.7,
    "limit": 50
})
anomalies = response.json()

for anomaly in anomalies:
    print(f"User: {anomaly['user']}, Score: {anomaly['anomalyScore']}")
```

### Using JavaScript

```javascript
const BASE_URL = "http://localhost:8081/api";

// Get latest events
fetch(`${BASE_URL}/events?limit=10`)
  .then(response => response.json())
  .then(events => {
    events.forEach(event => {
      console.log(`${event.user}: ${event.type}`);
    });
  });
```

---

## Troubleshooting Empty Results

If endpoints return empty arrays `[]`, check the system status:

```bash
# Check system health
curl http://localhost:8081/api/health/status | jq

# Expected response:
{
  "status": "UP",
  "database": {
    "eventCount": 12500,      # Should be > 0
    "anomalyCount": 125,      # Will be 0 until anomalies detected
    "analysisCount": 250      # Will be 0 if OPENAI_API_KEY not set
  },
  "features": {
    "eventsAvailable": true,
    "anomalyDetectionAvailable": true,
    "aiAnalysisAvailable": true
  }
}
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `/api/events` returns `[]` | Stream-processor not running or not connected to database | Start stream-processor: `./scripts/start-stream-processor.sh` |
| `/api/analyses` returns `[]` | OPENAI_API_KEY not configured | Set `OPENAI_API_KEY` in `.env` and restart stream-processor |
| `/api/anomalies` returns `[]` | Not enough events or no anomalies detected | Wait for 100+ events, or lower threshold in anomaly_detector.cpp |
| `/api/analyses/count` returns `0` | AI analysis column family not created | Check query-api logs for warnings about column families |

### Debug Steps

1. **Check if stream-processor is running:**
   ```bash
   ps aux | grep stream-processor
   ```

2. **Check stream-processor logs:**
   ```bash
   # Look for "EventStore" messages about column families
   # Should see: "Column families: default (events), ai_analysis, embeddings, anomalies"
   ```

3. **Check query-api logs:**
   ```bash
   # Look for warnings like:
   # "AI analysis column family not available"
   # "OPENAI_API_KEY is not configured"
   ```

4. **Verify database exists:**
   ```bash
   ls -la ./data/events.db/
   # Should see CURRENT, MANIFEST, *.sst files
   ```

5. **Check OPENAI_API_KEY:**
   ```bash
   grep OPENAI_API_KEY .env
   # Should NOT be empty or "sk-test-dummy"
   ```
