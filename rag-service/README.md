# StreamGuard RAG Service

Threat Intelligence Retrieval-Augmented Generation (RAG) microservice using FastAPI and ChromaDB.

## Features

- **Vector Similarity Search**: Find similar threats using semantic search
- **Threat Intelligence Database**: 100+ curated threat indicators
- **Automatic API Documentation**: Interactive Swagger UI at `/docs`
- **AI-Powered Summaries**: OpenAI GPT-4o-mini generates context summaries
- **Fast Performance**: Async FastAPI with < 500ms P95 latency

## Quick Start

### 1. Start ChromaDB

```bash
docker-compose up -d
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Set Environment Variables

```bash
export OPENAI_API_KEY="your-api-key-here"
export CHROMA_HOST="localhost"
export CHROMA_PORT="8001"
```

### 4. Start the Service

```bash
uvicorn main:app --reload --port 8000
```

### 5. Seed Threat Intelligence

```bash
python seed_threats.py
```

## API Endpoints

### Query Threats
```bash
POST /rag/query
{
  "event_context": "Suspicious process execution mimikatz.exe",
  "top_k": 5
}
```

### Seed Database
```bash
POST /rag/seed
{
  "threats": [...]
}
```

### Health Check
```bash
GET /health
```

## Interactive API Docs

Visit http://localhost:8000/docs for auto-generated Swagger UI

## Architecture

- **FastAPI**: Modern async Python web framework
- **ChromaDB**: Vector database for semantic search
- **OpenAI**: GPT-4o-mini for AI summaries
- **Docker**: Containerized ChromaDB deployment

Author: Jose Ortuno
