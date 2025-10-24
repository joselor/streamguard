"""
StreamGuard AI Security Assistant API

FastAPI application providing natural language interface to security data.
"""

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from datetime import datetime
import logging
import sys

from app.config import settings
from app.models import QueryRequest, QueryResponse, HealthCheck, ErrorResponse
from app.services import SecurityAssistant, JavaAPIClient, RAGClient

# Configure logging
logging.basicConfig(
    level=settings.log_level,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# Global instances
assistant = None
java_api = None
rag_client = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifecycle manager - initializes services on startup"""
    global assistant, java_api, rag_client

    logger.info("=€ Starting AI Security Assistant")
    logger.info(f"Service: {settings.service_host}:{settings.service_port}")
    logger.info(f"OpenAI Model: {settings.openai_model}")
    logger.info(f"Java API: {settings.java_api_url}")
    logger.info(f"RAG Service: {settings.rag_service_url}")

    # Initialize clients
    try:
        assistant = SecurityAssistant()
        java_api = JavaAPIClient()
        rag_client = RAGClient()
        logger.info(" All services initialized successfully")
    except Exception as e:
        logger.error(f" Failed to initialize services: {str(e)}")
        raise

    yield

    # Cleanup
    logger.info("Shutting down AI Security Assistant")


# Create FastAPI app
app = FastAPI(
    title="StreamGuard AI Security Assistant",
    description="""
    Natural language interface for security event analysis.

    ## Features
    - Ask questions in plain English about security events
    - Get AI-powered insights with supporting evidence
    - Query threat intelligence knowledge base
    - Receive actionable recommendations

    ## Example Queries
    - "What suspicious activity happened in the last hour?"
    - "Show me alice's failed login attempts"
    - "Are there any high-severity threats today?"
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Exception handlers
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Unhandled exception: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content=ErrorResponse(
            error="InternalServerError",
            message="An unexpected error occurred",
            detail=str(exc)
        ).dict()
    )


# Root endpoint
@app.get("/")
async def root():
    """Root endpoint with service information"""
    return {
        "service": "StreamGuard AI Security Assistant",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "query": "POST /query - Ask security questions",
            "health": "GET /health - Health check",
            "docs": "GET /docs - Interactive API documentation"
        },
        "examples": [
            "What suspicious activity happened in the last hour?",
            "Show me alice's failed login attempts",
            "Are there any anomalies for user bob?"
        ]
    }


# Query endpoint
@app.post(
    "/query",
    response_model=QueryResponse,
    summary="Query Security Events",
    description="Ask natural language questions about security events and get AI-powered answers",
    response_description="Structured answer with evidence and recommendations"
)
async def query_assistant(request: QueryRequest):
    """
    Answer natural language security queries

    Orchestrates:
    1. Fetching events from Java API
    2. Querying threat intelligence from RAG service
    3. Synthesizing answer with OpenAI
    4. Providing actionable recommendations
    """
    try:
        logger.info(f"Received query: '{request.question}'")

        if not assistant:
            raise HTTPException(
                status_code=503,
                detail="Assistant not initialized"
            )

        result = await assistant.answer_query(
            question=request.question,
            context_window=request.context_window,
            user_id=request.user_id,
            include_threat_intel=request.include_threat_intel
        )

        return QueryResponse(**result)

    except Exception as e:
        logger.error(f"Query failed: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Query processing failed: {str(e)}"
        )


# Health check endpoint
@app.get(
    "/health",
    response_model=HealthCheck,
    summary="Health Check",
    description="Check service health and dependency status"
)
async def health_check():
    """
    Health check endpoint

    Returns service status and availability of dependencies
    """
    services_status = {}

    # Check Java API
    try:
        if java_api:
            services_status["java_api"] = await java_api.health_check()
        else:
            services_status["java_api"] = False
    except Exception as e:
        logger.error(f"Java API health check failed: {str(e)}")
        services_status["java_api"] = False

    # Check RAG Service
    try:
        if rag_client:
            services_status["rag_service"] = await rag_client.health_check()
        else:
            services_status["rag_service"] = False
    except Exception as e:
        logger.error(f"RAG service health check failed: {str(e)}")
        services_status["rag_service"] = False

    # Check OpenAI (assume healthy if API key configured)
    services_status["openai"] = bool(settings.openai_api_key)

    # Overall status
    all_healthy = all(services_status.values())
    status = "healthy" if all_healthy else "degraded"

    return HealthCheck(
        status=status,
        timestamp=datetime.now(),
        services=services_status,
        version="1.0.0"
    )


# Metrics endpoint (placeholder for Prometheus)
@app.get(
    "/metrics",
    summary="Prometheus Metrics",
    description="Metrics endpoint for monitoring (placeholder)"
)
async def metrics():
    """
    Prometheus-compatible metrics endpoint

    TODO: Implement prometheus_client integration
    """
    return {
        "note": "Metrics endpoint placeholder",
        "todo": "Implement prometheus_client for production metrics"
    }


# Run with uvicorn
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.service_host,
        port=settings.service_port,
        reload=True,
        log_level=settings.log_level.lower()
    )
