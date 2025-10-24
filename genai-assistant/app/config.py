"""
Configuration management for AI Security Assistant

Uses pydantic-settings for environment variable validation and type safety.
"""

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Service Configuration
    service_host: str = "0.0.0.0"
    service_port: int = 8002
    log_level: str = "INFO"

    # OpenAI Configuration
    openai_api_key: str
    openai_model: str = "gpt-4o-mini"
    openai_temperature: float = 0.7
    openai_max_tokens: int = 1000
    openai_timeout: int = 30

    # StreamGuard Services
    java_api_url: str = "http://localhost:8081"
    rag_service_url: str = "http://localhost:8000"
    rocksdb_host: Optional[str] = None
    rocksdb_port: Optional[int] = None

    # Query Configuration
    default_context_window: str = "1h"
    max_supporting_events: int = 10
    max_threat_intel_results: int = 5

    # Observability
    enable_metrics: bool = True
    enable_request_logging: bool = True

    # Performance
    http_timeout: float = 30.0
    max_retries: int = 3

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# Global settings instance
settings = Settings()
