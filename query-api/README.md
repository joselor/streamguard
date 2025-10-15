# StreamGuard Query API

Spring Boot REST API for querying security events and AI threat analyses stored in RocksDB.

## Features

- **Read-only RocksDB access** - Safe query interface without affecting write operations
- **Column family support** - Queries both events and AI analyses
- **RESTful endpoints** - Standard HTTP APIs for easy integration
- **OpenAPI documentation** - Swagger UI for interactive API exploration
- **Spring Boot Actuator** - Health checks and metrics

## Architecture

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │ HTTP/REST
       ↓
┌─────────────────────┐
│  Query API (8081)   │
│  ┌──────────────┐   │
│  │ Controllers  │   │
│  ├──────────────┤   │
│  │ QueryService │   │
│  ├──────────────┤   │
│  │  RocksDB     │   │
│  │  (read-only) │   │
│  └──────────────┘   │
└─────────────────────┘
       ↓ reads
┌─────────────────────┐
│   RocksDB Storage   │
│  ┌──────────────┐   │
│  │   default    │   │ ← Security Events
│  │ ai_analysis  │   │ ← AI Threat Analyses
│  └──────────────┘   │
└─────────────────────┘
```

## API Endpoints

### Security Events

- `GET /api/events` - Get latest security events
  - Query param: `limit` (default: 100)
- `GET /api/events/{eventId}` - Get specific event by ID
- `GET /api/events/count` - Get total event count

### AI Threat Analyses

- `GET /api/analyses` - Get latest AI threat analyses
  - Query param: `limit` (default: 100)
- `GET /api/analyses/event/{eventId}` - Get analysis for specific event
- `GET /api/analyses/severity/{severity}` - Filter analyses by severity
  - Severity: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
  - Query param: `limit` (default: 100)
- `GET /api/analyses/count` - Get total analysis count

### Anomalies

- `GET /api/anomalies` - Get latest anomalies
  - Query param: `limit` (default: 100)
- `GET /api/anomalies/high-score` - Get high-score anomalies
  - Query param: `threshold` (default: 0.5)
  - Query param: `limit` (default: 100)
- `GET /api/anomalies/user/{user}` - Get anomalies for specific user
  - Query param: `limit` (default: 100)

### Documentation, Health & Metrics

- `GET /swagger-ui.html` - Interactive API documentation
- `GET /api-docs` - OpenAPI specification (JSON)
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/prometheus` - Prometheus metrics endpoint

## Configuration

Configuration in `application.yml`:

```yaml
server:
  port: 8081

rocksdb:
  path: ${ROCKSDB_PATH:./data/events.db}
  read-only: true

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `ROCKSDB_PATH` | RocksDB database path | `./data/events.db` | No |
| `OPENAI_API_KEY` | OpenAI API key (if using AI features) | None | No |

**Important:**
- `ROCKSDB_PATH` should point to the **same database** created by stream-processor
- Set to an absolute path or relative from project root
- The database must exist before starting the query-api (stream-processor must run first)
- Example: `ROCKSDB_PATH=/Users/you/streamguard/data/events.db` or `ROCKSDB_PATH=./data/events.db`

## Running

### Prerequisites

**IMPORTANT:** The query-api requires an existing RocksDB database created by stream-processor:

1. Start stream-processor first to create the database
2. Then start query-api pointing to the same database path

### Build

```bash
mvn clean package
```

### Running the API

#### Option 1: Using Startup Script (Recommended)

```bash
# Configure environment (edit .env file first)
cp ../env.example ../.env

# Start query API (stream-processor must be running first!)
../scripts/start-query-api.sh
```

The startup script will:
- Load configuration from `.env`
- Validate that the database exists
- Convert relative paths to absolute paths
- Start the API with correct environment variables

#### Option 2: Manual Start

```bash
# From project root directory
cd query-api

# Option A: Use environment variable
export ROCKSDB_PATH=../data/events.db
java -jar target/query-api-1.0.0.jar

# Option B: Inline environment variable
ROCKSDB_PATH=../data/events.db java -jar target/query-api-1.0.0.jar

# Option C: Use absolute path
ROCKSDB_PATH=/Users/you/streamguard/data/events.db java -jar target/query-api-1.0.0.jar
```

### Docker

```bash
docker build -t streamguard-query-api .
docker run -p 8081:8081 \
  -v /path/to/rocksdb:/data \
  -e ROCKSDB_PATH=/data/events.db \
  streamguard-query-api
```

**Note:** In Docker Compose, the database volume is automatically shared between stream-processor and query-api.

## Testing

### Using curl

```bash
# Get event count
curl http://localhost:8081/api/events/count

# Get latest 10 events
curl "http://localhost:8081/api/events?limit=10"

# Get specific event
curl http://localhost:8081/api/events/evt_123

# Get high severity analyses
curl "http://localhost:8081/api/analyses/severity/HIGH?limit=20"

# Get high-score anomalies
curl "http://localhost:8081/api/anomalies/high-score?threshold=0.7&limit=10"

# Get Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

### Using Swagger UI

Open browser to: http://localhost:8081/swagger-ui.html

## Monitoring

### Prometheus Metrics

The Query API exposes Prometheus metrics at `/actuator/prometheus`:

**Standard Spring Boot Metrics:**
- **HTTP Metrics**: `http_server_requests_seconds` - Request duration and counts
- **JVM Metrics**:
  - `jvm_memory_used_bytes` - Memory usage by pool
  - `jvm_gc_pause_seconds` - Garbage collection pauses
  - `jvm_threads_live` - Active thread count
- **System Metrics**:
  - `system_cpu_usage` - System CPU utilization
  - `process_cpu_usage` - Process CPU utilization
  - `system_load_average_1m` - System load average

**Query API Specific Metrics:**
- `rocksdb_reads_total` - Total RocksDB read operations
- `rocksdb_read_latency_seconds` - RocksDB read latency histogram
- `api_queries_total` - Total API queries by endpoint

### Grafana Integration

Import the StreamGuard dashboard from `/monitoring/grafana/dashboards/streamguard-performance.json`

**Key Panels:**
- API request rate and latency (p50, p95, p99)
- JVM heap usage and GC frequency
- RocksDB read performance
- Error rates by endpoint

### Health Checks

The `/actuator/health` endpoint provides health status:

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "rocksdb": {"status": "UP", "details": {"columnFamilies": 4}}
  }
}
```

## Dependencies

- **Spring Boot 3.2.0** - Application framework
- **RocksDB Java 8.6.7** - Database client library
- **Micrometer Prometheus** - Metrics export
- **Spring Boot Actuator** - Monitoring endpoints
- **Lombok** - Code generation
- **Springdoc OpenAPI** - API documentation
- **Jackson** - JSON serialization

## Development

### Project Structure

```
query-api/
├── src/main/java/com/streamguard/queryapi/
│   ├── QueryApiApplication.java       # Main application
│   ├── config/
│   │   └── RocksDBConfig.java        # RocksDB configuration
│   ├── controller/
│   │   ├── EventController.java      # Event endpoints
│   │   └── AnalysisController.java   # Analysis endpoints
│   ├── model/
│   │   ├── SecurityEvent.java        # Event model
│   │   └── ThreatAnalysis.java       # Analysis model
│   └── service/
│       └── QueryService.java         # Query business logic
├── src/main/resources/
│   └── application.yml               # Configuration
├── pom.xml                          # Maven dependencies
└── README.md
```

### Adding New Endpoints

1. Add method to `QueryService.java`
2. Create controller endpoint in appropriate controller
3. Add Swagger annotations for documentation
4. Test with curl or Swagger UI

## Notes

- API runs in **read-only mode** - no write operations allowed
- Database must be created by stream-processor before querying
- Column families are auto-discovered from existing database
- Jackson deserializes JSON stored by C++ stream processor
