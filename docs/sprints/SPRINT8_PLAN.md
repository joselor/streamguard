# Sprint 8 Plan: AI Security Assistant API

**Sprint Duration:** Sprint 8
**Date:** October 24, 2025
**Status:** 🚀 In Progress
**Focus:** GenAI conversational interface for security event analysis

---

## 🎯 Sprint Goal

Build a conversational FastAPI service that lets users query security events using natural language. The service integrates with existing StreamGuard components (Java API, RAG service, RocksDB) and uses OpenAI to synthesize intelligent answers.

---

## 📋 Current Architecture Context

### StreamGuard Components (Already Built)

| Component | Technology | Port | Purpose |
|-----------|------------|------|---------|
| Stream Processor | C++ | 8080 (metrics) | Real-time event processing |
| Query API | Java/Spring Boot | 8081 | REST API for events/anomalies |
| RAG Service | FastAPI + ChromaDB | 8000 | Threat intelligence vector search |
| Spark ML Pipeline | Python/PySpark | - | Batch processing layer |
| Prometheus | - | 9090 | Metrics collection |
| Grafana | - | 3000 | Dashboards |

### New Component: AI Security Assistant

| Property | Value |
|----------|-------|
| Location | `genai-assistant/` |
| Port | 8002 |
| Technology | FastAPI + OpenAI + async Python |
| Purpose | Natural language interface to security data |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│              User Natural Language Query            │
│          "Show me alice's suspicious activity"      │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│         AI Security Assistant (Port 8002)            │
│              FastAPI + OpenAI                        │
│  ┌────────────────────────────────────────────────┐ │
│  │ 1. Parse intent with LLM                       │ │
│  │ 2. Fetch data from multiple sources            │ │
│  │ 3. Synthesize intelligent answer               │ │
│  └────────────────────────────────────────────────┘ │
└───┬────────────────┬────────────────┬────────────────┘
    │                │                │
    ▼                ▼                ▼
┌─────────┐  ┌──────────────┐  ┌──────────┐
│ Java    │  │ RAG Service  │  │ RocksDB  │
│ API     │  │ (ChromaDB)   │  │ Direct   │
│ :8081   │  │ :8000        │  │ Queries  │
└─────────┘  └──────────────┘  └──────────┘
    │                │                │
    └────────────────┴────────────────┘
                     │
                     ▼
            ┌────────────────┐
            │ OpenAI GPT-4o  │
            │ Synthesis      │
            └────────────────┘
```

---

## 📁 Directory Structure

```
streamguard/
└── genai-assistant/
    ├── app/
    │   ├── __init__.py
    │   ├── main.py                 # FastAPI application
    │   ├── config.py               # Configuration (env vars)
    │   ├── models.py               # Pydantic models
    │   ├── services/
    │   │   ├── __init__.py
    │   │   ├── assistant.py        # Core AI logic
    │   │   ├── java_api.py         # Java API client
    │   │   ├── rag_client.py       # RAG service client
    │   │   └── rocksdb_client.py   # RocksDB queries (optional)
    │   ├── prompts/
    │   │   ├── __init__.py
    │   │   └── system_prompts.py   # LLM prompts
    │   └── utils/
    │       ├── __init__.py
    │       ├── metrics.py          # Prometheus metrics
    │       └── logging_config.py   # Structured logging
    ├── tests/
    │   ├── __init__.py
    │   ├── test_assistant.py
    │   └── conftest.py
    ├── Dockerfile
    ├── docker-compose.yml
    ├── requirements.txt
    ├── .env.example
    └── README.md
```

---

## 🎯 Key Features

### 1. Natural Language Query Interface
- Accept plain English questions about security events
- Examples:
  - "What suspicious activity did alice have in the last hour?"
  - "Show me all failed logins with high threat scores"
  - "Are there any anomalies in network traffic today?"

### 2. Multi-Source Context Aggregation
- **Java API**: Real-time events, anomalies, threat scores
- **RAG Service**: Historical threat intelligence from ChromaDB
- **RocksDB**: Direct queries for aggregated statistics (optional)

### 3. Intelligent Synthesis
- OpenAI GPT-4o-mini generates context-aware answers
- Cites specific events and data points
- Provides actionable recommendations
- Explains WHY something is suspicious

### 4. Production-Ready
- Async FastAPI with proper error handling
- Pydantic validation for all inputs/outputs
- Structured logging (JSON format)
- Prometheus metrics endpoint
- Health checks
- Docker containerization

---

## 🔧 Implementation Tasks

### Phase 1: Foundation (30 min)
- [x] Create Sprint 8 plan document
- [ ] Create directory structure
- [ ] Implement `config.py` (environment variables)
- [ ] Implement `models.py` (Pydantic schemas)
- [ ] Create `requirements.txt`
- [ ] Create `.env.example`

### Phase 2: Prompt Engineering (20 min)
- [ ] Implement `system_prompts.py`
- [ ] Create security analyst system prompt
- [ ] Create query construction prompt
- [ ] Add few-shot examples

### Phase 3: Service Integration (40 min)
- [ ] Implement `java_api.py` (HTTP client for Java API)
- [ ] Implement `rag_client.py` (HTTP client for RAG service)
- [ ] Implement `rocksdb_client.py` (optional direct queries)
- [ ] Add connection pooling and retry logic

### Phase 4: Core AI Logic (40 min)
- [ ] Implement `assistant.py`
- [ ] Multi-source data aggregation
- [ ] OpenAI API integration
- [ ] Response synthesis logic
- [ ] Extract recommendations from LLM output

### Phase 5: FastAPI Application (30 min)
- [ ] Implement `main.py`
- [ ] `/query` endpoint (POST)
- [ ] `/health` endpoint (GET)
- [ ] `/metrics` endpoint (GET) - Prometheus format
- [ ] Error handling middleware
- [ ] CORS configuration

### Phase 6: Observability (20 min)
- [ ] Implement `metrics.py` (Prometheus metrics)
- [ ] Implement `logging_config.py` (structured JSON logs)
- [ ] Add request/response logging
- [ ] Track query latency and success rates

### Phase 7: Containerization (20 min)
- [ ] Create `Dockerfile` (multi-stage build)
- [ ] Create `docker-compose.yml`
- [ ] Add health checks
- [ ] Test container build

### Phase 8: Documentation & Testing (30 min)
- [ ] Create comprehensive `README.md`
- [ ] Add API usage examples
- [ ] Write integration tests
- [ ] Test end-to-end flow
- [ ] Update root README.md

### Phase 9: Integration (20 min)
- [ ] Update `docker-compose.yml` in root
- [ ] Add service to architecture diagrams
- [ ] Test with existing services
- [ ] Verify Grafana dashboard integration

### Phase 10: Commit & Push (10 min)
- [ ] Git commit with descriptive message
- [ ] Push to GitHub
- [ ] Update Sprint 8 handoff document

**Estimated Total Time:** 4-5 hours

---

## ✅ Acceptance Criteria

### Functional Requirements
- [ ] FastAPI service starts successfully on port 8002
- [ ] `POST /query` accepts natural language questions
- [ ] Service fetches events from Java API (port 8081)
- [ ] Service queries RAG service for threat intelligence (port 8000)
- [ ] OpenAI integration synthesizes intelligent answers
- [ ] Responses include supporting evidence and recommendations
- [ ] All API endpoints return valid Pydantic-validated JSON

### Non-Functional Requirements
- [ ] Response time < 3 seconds for typical queries
- [ ] Proper error handling with meaningful messages
- [ ] Structured JSON logging for all requests
- [ ] Prometheus metrics exported at `/metrics`
- [ ] Health check passes when all dependencies available
- [ ] Docker container builds without errors
- [ ] Service starts via docker-compose

### Documentation Requirements
- [ ] README.md with setup instructions
- [ ] API examples with curl commands
- [ ] .env.example with all required variables
- [ ] Inline code comments for complex logic
- [ ] Updated architecture diagrams

---

## 🧪 Testing Strategy

### Manual Testing
```bash
# 1. Start all services
docker-compose up -d

# 2. Test health check
curl http://localhost:8002/health

# 3. Test simple query
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What happened in the last hour?"}'

# 4. Test user-specific query
curl -X POST http://localhost:8002/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Show me suspicious activity for alice", "context_window": "24h"}'

# 5. Test metrics endpoint
curl http://localhost:8002/metrics
```

### Integration Testing
- [ ] Mock Java API responses
- [ ] Mock RAG service responses
- [ ] Mock OpenAI responses
- [ ] Test error scenarios (service unavailable, timeout, etc.)

---

## 📊 Success Metrics

| Metric | Target |
|--------|--------|
| Query Response Time (P95) | < 3 seconds |
| Service Uptime | > 99% |
| OpenAI API Success Rate | > 95% |
| Useful Answer Rate | > 90% (subjective) |
| Error Rate | < 5% |

---

## 🚧 Known Limitations & Future Work

### Current Scope (Sprint 8)
- ✅ Single-turn queries (no conversation history)
- ✅ English language only
- ✅ Basic prompt engineering (no advanced RAG patterns)
- ✅ Manual deployment (no CI/CD)

### Future Enhancements (Sprint 9+)
- [ ] Multi-turn conversations with memory
- [ ] Function calling for dynamic tool selection
- [ ] Streaming responses for better UX
- [ ] Advanced RAG with re-ranking
- [ ] Fine-tuned model for security domain
- [ ] Rate limiting and authentication
- [ ] Caching for common queries

---

## 🔗 Related Documentation

- [Sprint 7 Handoff](PROJECT_HANDOFF_SPRINT7.md) - Previous sprint context
- [Architecture Guide](../product/guides/ARCHITECTURE.md) - System design
- [API Reference](../product/api/API_REFERENCE.md) - Java API endpoints
- [RAG Service README](../../rag-service/README.md) - Existing RAG service

---

## 👥 Collaboration Notes

**Primary Developer:** Jose Ortuno
**Supporting Agents:**
- Claude Code (implementation, file operations)
- Web Claude (strategic planning, architecture)
- Claude Remote (future: testing, deployment)

**Communication:**
- Web Claude provides strategic direction and Sprint planning
- Claude Code (this instance) handles implementation and git operations
- User coordinates between instances with context updates

---

**Sprint 8 Status:** 🚀 Ready to Execute
**Next Action:** Begin Phase 1 - Foundation
