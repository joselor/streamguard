# StreamGuard AI Security Assistant

Natural language interface for security event analysis using FastAPI and OpenAI GPT-4o-mini.

## Overview

The AI Security Assistant provides a conversational interface to StreamGuard's security data, allowing security analysts to ask questions in plain English and receive intelligent, context-aware answers backed by real events and threat intelligence.

### Key Features

- ✅ **Natural Language Queries** - Ask questions in plain English
- ✅ **Multi-Source Integration** - Combines data from Java API, RAG service, and RocksDB
- ✅ **AI-Powered Synthesis** - OpenAI GPT-4o-mini generates comprehensive answers
- ✅ **Evidence-Based** - Cites specific events and threat intelligence
- ✅ **Actionable Recommendations** - Provides prioritized next steps
- ✅ **Production-Ready** - Async FastAPI with proper error handling
- ✅ **Interactive Docs** - Auto-generated Swagger UI at `/docs`

## Architecture

```
User Query (Natural Language)
         │
         ▼
┌─────────────────────────────┐
│  AI Security Assistant API  │ ← FastAPI (Port 8002)
│                             │
│  1. Parse Intent            │
│  2. Gather Context          │
│  3. Synthesize Answer       │
└───┬─────────┬───────┬───────┘
    │         │       │
    ▼         ▼       ▼
┌────────┐ ┌─────┐ ┌────────┐
│ Java   │ │ RAG │ │RocksDB │
│ API    │ │ Svc │ │(Future)│
│ :8081  │ │:8000│ │        │
└────────┘ └─────┘ └────────┘
    │         │         │
    └─────────┴─────────┘
             │
             ▼
    ┌────────────────┐
    │  OpenAI API    │
    │  GPT-4o-mini   │
    └────────────────┘
```

## Quick Start

### Prerequisites

- Python 3.11+
- **LLM Provider** (choose one):
  - **OpenAI**: API key required
  - **Ollama**: Local installation (FREE, no API key needed)
- Running StreamGuard services:
  - Java API (port 8081)
  - RAG Service (port 8000) - optional

### Quick Start with Startup Script (Recommended)

The easiest way to start the GenAI Assistant:

```bash
# From project root
./scripts/start-genai-assistant.sh
```

This script will:
- ✅ Validate all dependencies
- ✅ Check LLM provider configuration
- ✅ Create virtual environment if needed
- ✅ Install dependencies
- ✅ Start the service

### Manual Installation

1. **Clone and navigate**
   ```bash
   cd streamguard/genai-assistant
   ```

2. **Install dependencies**
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and configure your LLM provider (see Configuration section)
   ```

4. **Run the service**
   ```bash
   uvicorn app.main:app --reload --port 8002
   ```

5. **Test it works**
   ```bash
   curl http://localhost:8002/health
   ```

### Docker Deployment

```bash
# Build image
docker build -t streamguard-genai-assistant .

# Run container
docker run -d \
  -p 8002:8002 \
  -e OPENAI_API_KEY=your-key \
  -e JAVA_API_URL=http://host.docker.internal:8081 \
  -e RAG_SERVICE_URL=http://host.docker.internal:8000 \
  --name genai-assistant \
  streamguard-genai-assistant

# Or use docker-compose
docker-compose up -d
```

## API Documentation

### Interactive Docs

Visit `http://localhost:8002/docs` for interactive Swagger UI documentation.

### Endpoints

#### POST `/query` - Ask Security Questions

**Request:**
```json
{
  "question": "What suspicious activity happened in the last hour?",
  "context_window": "1h",
  "user_id": null,
  "include_threat_intel": true
}
```

**Response:**
```json
{
  "answer": "In the past hour, there were 5 failed login attempts...",
  "confidence": 0.87,
  "supporting_events": [
    {
      "event_id": "evt_001",
      "timestamp": "2025-10-24T10:30:00Z",
      "event_type": "LOGIN_FAILED",
      "severity": "HIGH",
      "user": "alice",
      "source_ip": "10.0.1.50",
      "threat_score": 0.85
    }
  ],
  "threat_intel": [
    {
      "source": "MITRE ATT&CK",
      "summary": "Credential stuffing attack pattern",
      "relevance_score": 0.92
    }
  ],
  "recommended_actions": [
    "Investigate source IP 10.0.1.50",
    "Force password reset for affected users",
    "Enable MFA"
  ],
  "query_time_ms": 1250,
  "sources_used": ["java_api", "rag_service", "openai"]
}
```

#### GET `/health` - Health Check

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-10-24T10:30:00Z",
  "services": {
    "java_api": true,
    "rag_service": true,
    "openai": true
  },
  "version": "1.0.0"
}
```

## Example Queries

### General Security Questions
```bash
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What happened in the last hour?"}'
```

### User-Specific Queries
```bash
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Show me alice'\''s failed login attempts",
    "context_window": "24h",
    "user_id": "alice"
  }'
```

### Threat-Focused Queries
```bash
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Are there any high-severity threats today?",
    "context_window": "24h",
    "include_threat_intel": true
  }'
```

## Configuration

All configuration via environment variables (see `.env.example`):

### LLM Provider Selection
- `LLM_PROVIDER` - Choose "openai" or "ollama" (default: openai)

### Service Configuration
- `SERVICE_HOST` - Host to bind (default: 0.0.0.0)
- `SERVICE_PORT` - Port to listen (default: 8002)
- `LOG_LEVEL` - Logging level (default: INFO)

### OpenAI Settings (when LLM_PROVIDER=openai)
- `OPENAI_API_KEY` - Your OpenAI API key (required for OpenAI)
- `OPENAI_MODEL` - Model to use (default: gpt-4o-mini)
- `OPENAI_TEMPERATURE` - Creativity (default: 0.7)
- `OPENAI_MAX_TOKENS` - Max response length (default: 1000)

### Ollama Settings (when LLM_PROVIDER=ollama)
- `OLLAMA_BASE_URL` - Ollama server URL (default: http://localhost:11434)
- `OLLAMA_MODEL` - Model to use (default: llama3.2:latest)
- `OLLAMA_TEMPERATURE` - Creativity (default: 0.7)
- `OLLAMA_MAX_TOKENS` - Max response length (default: 1000)

### Integration Settings
- `JAVA_API_URL` - Java API endpoint (default: http://localhost:8081)
- `RAG_SERVICE_URL` - RAG service endpoint (default: http://localhost:8000)

## Local Model Setup (Ollama)

### Why Use Ollama?

✅ **Zero Cost** - No API fees, unlimited queries
✅ **Privacy** - All data stays local
✅ **Fast** - No network latency (25-50% faster potential)
✅ **Offline** - Works without internet

### Installation

1. **Install Ollama**
   ```bash
   # Mac
   brew install ollama

   # Or download from https://ollama.com
   ```

2. **Start Ollama Server**
   ```bash
   ollama serve
   ```

3. **Pull a Model**
   ```bash
   # Recommended: Llama 3.2 (2GB, good for security analysis)
   ollama pull llama3.2:latest

   # Alternative: Mistral 7B (4GB, excellent quality)
   ollama pull mistral:7b

   # Or: DeepSeek-R1 (1.1GB, faster but smaller)
   ollama pull deepseek-r1:1.5b
   ```

4. **Verify Model is Available**
   ```bash
   ollama list
   ```

5. **Configure GenAI Assistant**
   ```bash
   # In .env file
   LLM_PROVIDER=ollama
   OLLAMA_MODEL=llama3.2:latest
   OLLAMA_BASE_URL=http://localhost:11434
   ```

6. **Start the Service**
   ```bash
   ./scripts/start-genai-assistant.sh
   ```

### Model Recommendations

| Model | Size | Speed | Quality | Use Case |
|-------|------|-------|---------|----------|
| **llama3.2:latest** | 2.0GB | Fast | Excellent | **Recommended** for security analysis |
| mistral:7b | 4.1GB | Medium | Excellent | High-quality responses |
| deepseek-r1:1.5b | 1.1GB | Very Fast | Good | Quick demos, limited resources |
| phi3:medium | 7.9GB | Slow | Very Good | Best quality, needs more RAM |

### Switching Between Providers

**Use OpenAI:**
```bash
# In .env
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-key-here
```

**Use Ollama:**
```bash
# In .env
LLM_PROVIDER=ollama
OLLAMA_MODEL=llama3.2:latest
```

Restart the service after changing providers.

## Development

### Running Tests
```bash
pytest tests/ -v --cov=app
```

### Code Formatting
```bash
black app/
flake8 app/
mypy app/
```

### Local Development with Auto-Reload
```bash
uvicorn app.main:app --reload --port 8002
```

## Performance

- **Response Time**: 1-3 seconds typical
- **Throughput**: ~30 requests/minute (OpenAI rate limits apply)
- **Async**: All I/O operations are async
- **Timeout**: 30s default for HTTP calls

## Troubleshooting

### Service Won't Start

**Check dependencies:**
```bash
# Java API
curl http://localhost:8081/api/events?limit=1

# RAG Service
curl http://localhost:8000/health

# OpenAI
echo $OPENAI_API_KEY
```

### Query Returns Empty Results

**Check event data:**
```bash
# Verify Java API has events
curl http://localhost:8081/api/events?limit=10

# Check time window
curl -X POST http://localhost:8002/query \
  -d '{"question": "...", "context_window": "24h"}'  # Try longer window
```

### OpenAI Errors

**Rate limit exceeded:**
- Reduce query frequency
- Wait 60 seconds
- Check OpenAI dashboard for usage

**Invalid API key:**
- Verify `OPENAI_API_KEY` in `.env`
- Check key permissions in OpenAI dashboard

## Project Structure

```
genai-assistant/
├── app/
│   ├── main.py              # FastAPI application
│   ├── config.py            # Configuration management
│   ├── models.py            # Pydantic models
│   ├── services/
│   │   ├── assistant.py     # Core AI logic
│   │   ├── java_api.py      # Java API client
│   │   └── rag_client.py    # RAG service client
│   ├── prompts/
│   │   └── system_prompts.py # Prompt engineering
│   └── utils/
│       ├── metrics.py       # Prometheus metrics (future)
│       └── logging_config.py # Logging setup
├── tests/                   # Test suite
├── Dockerfile               # Container definition
├── docker-compose.yml       # Local deployment
├── requirements.txt         # Python dependencies
├── .env.example            # Configuration template
└── README.md               # This file
```

## Integration with StreamGuard

This service is part of the StreamGuard ecosystem:

1. **Stream Processor (C++)** - Real-time event processing
2. **Java API** - Query interface for events/anomalies
3. **RAG Service** - Threat intelligence knowledge base
4. **AI Assistant** (this service) - Natural language interface
5. **Grafana** - Visualization dashboards

## Contributing

### Code Style
- Follow PEP 8
- Use type hints
- Document public functions
- Write tests for new features

### Testing
```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app --cov-report=html

# Run specific test
pytest tests/test_assistant.py -k test_query
```

## License

Part of StreamGuard - Demo project for CrowdStrike application

## Author

**Jose Ortuno** - Senior Solutions Architect
- LinkedIn: [linkedin.com/in/jose-ortuno](https://linkedin.com/in/jose-ortuno)
- GitHub: [github.com/joselor](https://github.com/joselor)

## Acknowledgments

- OpenAI GPT-4o-mini for AI capabilities
- FastAPI for modern async Python framework
- StreamGuard team for existing services integration
