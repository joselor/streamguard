"""
StreamGuard RAG (Retrieval-Augmented Generation) Service

Provides threat intelligence context by querying a vector database
of known security threats, attack patterns, and IOCs.

Author: Jose Ortuno
"""

import os
import time
from typing import List, Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import chromadb
from chromadb.config import Settings
from openai import OpenAI


# Configuration
CHROMA_HOST = os.getenv("CHROMA_HOST", "localhost")
CHROMA_PORT = int(os.getenv("CHROMA_PORT", "8001"))
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

# Global clients
chroma_client = None
openai_client = None
threat_collection = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifecycle manager for FastAPI app - initializes clients on startup"""
    global chroma_client, openai_client, threat_collection

    # Initialize ChromaDB client
    print(f"[RAG] Connecting to ChromaDB at {CHROMA_HOST}:{CHROMA_PORT}")
    chroma_client = chromadb.HttpClient(
        host=CHROMA_HOST,
        port=CHROMA_PORT,
        settings=Settings(
            anonymized_telemetry=False,
            allow_reset=True
        )
    )

    # Get or create collection
    threat_collection = chroma_client.get_or_create_collection(
        name="threat_intelligence",
        metadata={"description": "Security threat intelligence and IOCs"}
    )
    print(f"[RAG] Connected to collection: threat_intelligence (count: {threat_collection.count()})")

    # Initialize OpenAI client
    if OPENAI_API_KEY:
        openai_client = OpenAI(api_key=OPENAI_API_KEY)
        print("[RAG] OpenAI client initialized")
    else:
        print("[RAG] WARNING: OpenAI API key not configured")

    yield

    # Cleanup (if needed)
    print("[RAG] Shutting down RAG service")


# FastAPI app with automatic OpenAPI docs
app = FastAPI(
    title="StreamGuard RAG Service",
    description="Threat Intelligence Retrieval-Augmented Generation API",
    version="1.0.0",
    lifespan=lifespan
)


# Request/Response Models
class ThreatQuery(BaseModel):
    """Query model for threat intelligence lookup"""
    event_context: str = Field(
        ...,
        description="Security event context to search for similar threats",
        example="Suspicious process execution: mimikatz.exe by user admin"
    )
    top_k: int = Field(
        default=5,
        ge=1,
        le=20,
        description="Number of similar threats to return"
    )


class ThreatMatch(BaseModel):
    """Individual threat intelligence match"""
    threat_id: str
    description: str
    similarity: float
    category: str
    severity: str
    mitre_attack: Optional[str] = None


class RAGResponse(BaseModel):
    """RAG query response with context and analysis"""
    query: str
    matches: List[ThreatMatch]
    context_summary: str
    response_time_ms: float


class SeedRequest(BaseModel):
    """Request to seed threat intelligence data"""
    threats: List[dict] = Field(
        ...,
        description="List of threat intelligence entries to add"
    )


# Endpoints
@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "StreamGuard RAG Service",
        "status": "running",
        "threats_indexed": threat_collection.count() if threat_collection else 0
    }


@app.get("/health")
async def health():
    """Detailed health check"""
    chroma_healthy = False
    openai_healthy = OPENAI_API_KEY is not None

    try:
        if chroma_client:
            chroma_client.heartbeat()
            chroma_healthy = True
    except Exception as e:
        print(f"[RAG] ChromaDB health check failed: {e}")

    return {
        "status": "healthy" if (chroma_healthy and openai_healthy) else "degraded",
        "chromadb": "connected" if chroma_healthy else "disconnected",
        "openai": "configured" if openai_healthy else "not configured",
        "threats_count": threat_collection.count() if threat_collection else 0
    }


@app.post("/rag/query", response_model=RAGResponse)
async def query_threats(query: ThreatQuery):
    """
    Query threat intelligence database for similar known threats.

    Uses vector similarity search to find relevant threat intelligence
    that matches the provided event context, then generates an AI summary.
    """
    start_time = time.time()

    if not threat_collection:
        raise HTTPException(status_code=503, detail="ChromaDB not initialized")

    if threat_collection.count() == 0:
        raise HTTPException(
            status_code=404,
            detail="No threat intelligence data available. Use /rag/seed to populate."
        )

    try:
        # Query ChromaDB for similar threats
        results = threat_collection.query(
            query_texts=[query.event_context],
            n_results=min(query.top_k, threat_collection.count())
        )

        # Parse results
        matches = []
        if results['ids'][0]:
            for i in range(len(results['ids'][0])):
                metadata = results['metadatas'][0][i]
                distance = results['distances'][0][i]
                similarity = 1.0 - distance  # Convert distance to similarity

                matches.append(ThreatMatch(
                    threat_id=results['ids'][0][i],
                    description=results['documents'][0][i],
                    similarity=round(similarity, 4),
                    category=metadata.get('category', 'unknown'),
                    severity=metadata.get('severity', 'unknown'),
                    mitre_attack=metadata.get('mitre_attack')
                ))

        # Generate context summary using OpenAI
        context_summary = await generate_context_summary(query.event_context, matches)

        response_time = (time.time() - start_time) * 1000

        return RAGResponse(
            query=query.event_context,
            matches=matches,
            context_summary=context_summary,
            response_time_ms=round(response_time, 2)
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"RAG query failed: {str(e)}")


@app.post("/rag/seed")
async def seed_threats(request: SeedRequest):
    """
    Seed the threat intelligence database with known threats.

    This endpoint allows populating the database with threat intelligence
    from various sources (MITRE ATT&CK, threat feeds, CVE databases, etc.)
    """
    if not threat_collection:
        raise HTTPException(status_code=503, detail="ChromaDB not initialized")

    try:
        ids = []
        documents = []
        metadatas = []

        for threat in request.threats:
            ids.append(threat['id'])
            documents.append(threat['description'])
            metadatas.append({
                'category': threat.get('category', 'unknown'),
                'severity': threat.get('severity', 'medium'),
                'mitre_attack': threat.get('mitre_attack', '')
            })

        # Add to ChromaDB (with automatic embedding generation)
        threat_collection.add(
            ids=ids,
            documents=documents,
            metadatas=metadatas
        )

        return {
            "status": "success",
            "threats_added": len(ids),
            "total_threats": threat_collection.count()
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Seeding failed: {str(e)}")


@app.delete("/rag/reset")
async def reset_database():
    """Reset the threat intelligence database (development only)"""
    global threat_collection

    if not chroma_client:
        raise HTTPException(status_code=503, detail="ChromaDB not initialized")

    try:
        chroma_client.delete_collection("threat_intelligence")
        threat_collection = chroma_client.create_collection(
            name="threat_intelligence",
            metadata={"description": "Security threat intelligence and IOCs"}
        )
        return {
            "status": "success",
            "message": "Database reset successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Reset failed: {str(e)}")


async def generate_context_summary(query: str, matches: List[ThreatMatch]) -> str:
    """Generate AI summary of threat intelligence matches"""
    if not openai_client or not matches:
        return "No context available"

    try:
        # Build context from matches
        context = "\n".join([
            f"- {m.description} (Category: {m.category}, Severity: {m.severity}, Similarity: {m.similarity})"
            for m in matches[:3]  # Top 3 matches
        ])

        prompt = f"""Based on this security event:
"{query}"

And these similar known threats from our intelligence database:
{context}

Provide a concise 2-3 sentence summary of the threat context and recommendations."""

        response = openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are a cybersecurity threat analyst."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=150,
            temperature=0.7
        )

        return response.choices[0].message.content.strip()

    except Exception as e:
        print(f"[RAG] OpenAI summary generation failed: {e}")
        return f"Found {len(matches)} similar threats in database"


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
