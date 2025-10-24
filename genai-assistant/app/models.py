"""
Pydantic models for request/response validation

All API endpoints use these models for type safety and automatic validation.
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum


class SeverityLevel(str, Enum):
    """Threat severity levels"""
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"
    INFO = "INFO"


class QueryRequest(BaseModel):
    """Request model for natural language security queries"""
    question: str = Field(
        ...,
        description="Natural language security query",
        min_length=5,
        max_length=500,
        examples=["What suspicious activity happened in the last hour?"]
    )
    context_window: Optional[str] = Field(
        "1h",
        description="Time window for query (e.g., 1h, 24h, 7d)",
        pattern=r"^\d+[hdw]$"
    )
    user_id: Optional[str] = Field(
        None,
        description="Optional user ID to filter events"
    )
    include_threat_intel: bool = Field(
        True,
        description="Whether to include threat intelligence from RAG service"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "question": "Show me alice's failed login attempts",
                "context_window": "24h",
                "user_id": "alice",
                "include_threat_intel": True
            }
        }


class SupportingEvent(BaseModel):
    """Individual security event cited as evidence"""
    event_id: str
    timestamp: datetime
    event_type: str
    severity: SeverityLevel
    user: Optional[str] = None
    source_ip: Optional[str] = None
    threat_score: Optional[float] = Field(None, ge=0.0, le=1.0)
    details: Dict[str, Any] = Field(default_factory=dict)

    class Config:
        json_schema_extra = {
            "example": {
                "event_id": "evt_1696723200_001",
                "timestamp": "2025-10-24T10:30:00Z",
                "event_type": "LOGIN_FAILED",
                "severity": "HIGH",
                "user": "alice",
                "source_ip": "10.0.1.50",
                "threat_score": 0.85,
                "details": {"attempts": 3, "location": "San Francisco"}
            }
        }


class ThreatIntelligence(BaseModel):
    """Threat intelligence from RAG service"""
    source: str = Field(..., description="Source of threat intelligence")
    relevance_score: float = Field(..., ge=0.0, le=1.0, description="Relevance to query")
    summary: str = Field(..., description="Threat intelligence summary")
    iocs: Optional[List[str]] = Field(None, description="Indicators of Compromise")

    class Config:
        json_schema_extra = {
            "example": {
                "source": "MITRE ATT&CK",
                "relevance_score": 0.92,
                "summary": "Credential stuffing attack pattern detected",
                "iocs": ["mimikatz.exe", "suspicious_login_pattern"]
            }
        }


class QueryResponse(BaseModel):
    """Response model for security queries"""
    answer: str = Field(..., description="Natural language answer to query")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence in answer")
    supporting_events: List[SupportingEvent] = Field(
        default_factory=list,
        description="Events cited as evidence"
    )
    threat_intel: List[ThreatIntelligence] = Field(
        default_factory=list,
        description="Relevant threat intelligence"
    )
    recommended_actions: List[str] = Field(
        default_factory=list,
        description="Actionable recommendations"
    )
    query_time_ms: int = Field(..., description="Query execution time in milliseconds")
    sources_used: List[str] = Field(
        default_factory=list,
        description="Data sources queried"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "answer": "Alice had 3 failed login attempts from IP 10.0.1.50...",
                "confidence": 0.87,
                "supporting_events": [],
                "threat_intel": [],
                "recommended_actions": [
                    "Investigate source IP 10.0.1.50",
                    "Check for credential compromise",
                    "Enable MFA for user alice"
                ],
                "query_time_ms": 1250,
                "sources_used": ["java_api", "rag_service", "openai"]
            }
        }


class HealthCheck(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status (healthy/unhealthy)")
    timestamp: datetime = Field(default_factory=datetime.now)
    services: Dict[str, bool] = Field(
        default_factory=dict,
        description="Status of dependent services"
    )
    version: str = Field("1.0.0", description="Service version")

    class Config:
        json_schema_extra = {
            "example": {
                "status": "healthy",
                "timestamp": "2025-10-24T10:30:00Z",
                "services": {
                    "java_api": True,
                    "rag_service": True,
                    "openai": True
                },
                "version": "1.0.0"
            }
        }


class ErrorResponse(BaseModel):
    """Error response model"""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Error message")
    detail: Optional[str] = Field(None, description="Detailed error information")
    timestamp: datetime = Field(default_factory=datetime.now)

    class Config:
        json_schema_extra = {
            "example": {
                "error": "ServiceUnavailable",
                "message": "Java API is not responding",
                "detail": "Connection timeout after 30 seconds",
                "timestamp": "2025-10-24T10:30:00Z"
            }
        }
