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

### Documentation & Health

- `GET /swagger-ui.html` - Interactive API documentation
- `GET /api-docs` - OpenAPI specification (JSON)
- `GET /actuator/health` - Health check endpoint

## Configuration

Configuration in `application.yml`:

```yaml
server:
  port: 8081

rocksdb:
  path: ${ROCKSDB_PATH:../stream-processor/build/data/events.db}
  read-only: true
```

### Environment Variables

- `ROCKSDB_PATH` - Path to RocksDB database (default: `../stream-processor/build/data/events.db`)

## Running

### Build

```bash
mvn clean package
```

### Run

```bash
# Use default RocksDB path
java -jar target/query-api-1.0.0.jar

# Use custom RocksDB path
ROCKSDB_PATH=/path/to/events.db java -jar target/query-api-1.0.0.jar
```

### Docker

```bash
docker build -t streamguard-query-api .
docker run -p 8081:8081 \
  -v /path/to/rocksdb:/data \
  -e ROCKSDB_PATH=/data/events.db \
  streamguard-query-api
```

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
```

### Using Swagger UI

Open browser to: http://localhost:8081/swagger-ui.html

## Dependencies

- **Spring Boot 3.2.0** - Application framework
- **RocksDB Java 8.6.7** - Database client library
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
