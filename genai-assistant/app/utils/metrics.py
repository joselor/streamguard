"""
Prometheus metrics for GenAI Security Assistant

This module defines all metrics collected by the GenAI Assistant service:
- Query performance and latency
- OpenAI API usage and costs
- Error tracking
- Data source performance
- AI response quality
"""

from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
import time
from typing import Optional, Dict
from contextlib import contextmanager
import logging

logger = logging.getLogger(__name__)


# ============================================================================
# QUERY PERFORMANCE METRICS
# ============================================================================

QUERY_DURATION = Histogram(
    'genai_query_duration_seconds',
    'Time spent processing queries end-to-end',
    ['endpoint', 'status'],
    buckets=[0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0]
)

QUERIES_TOTAL = Counter(
    'genai_queries_total',
    'Total number of queries received',
    ['include_threat_intel']
)


# ============================================================================
# OPENAI API METRICS
# ============================================================================

OPENAI_REQUESTS = Counter(
    'genai_openai_requests_total',
    'Total OpenAI API requests',
    ['model', 'status']
)

OPENAI_TOKENS = Counter(
    'genai_openai_tokens_total',
    'Total OpenAI tokens used',
    ['model', 'token_type']  # token_type: prompt, completion
)

OPENAI_COST = Counter(
    'genai_openai_cost_dollars',
    'Estimated OpenAI API cost in dollars',
    ['model']
)

OPENAI_DURATION = Histogram(
    'genai_openai_duration_seconds',
    'Time spent waiting for OpenAI API',
    ['model'],
    buckets=[0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
)


# ============================================================================
# ERROR TRACKING
# ============================================================================

ERRORS_TOTAL = Counter(
    'genai_errors_total',
    'Total errors by type',
    ['error_type']  # error_type: openai, java_api, rag, validation, internal
)


# ============================================================================
# DATA SOURCE PERFORMANCE
# ============================================================================

DATA_SOURCE_DURATION = Histogram(
    'genai_data_source_duration_seconds',
    'Time spent fetching data from sources',
    ['source'],  # source: java_api, rag_service, rocksdb
    buckets=[0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0]
)

DATA_SOURCE_REQUESTS = Counter(
    'genai_data_source_requests_total',
    'Total requests to data sources',
    ['source', 'status']
)


# ============================================================================
# AI QUALITY METRICS
# ============================================================================

CONFIDENCE_SCORE = Histogram(
    'genai_confidence_score',
    'AI response confidence scores',
    buckets=[0.0, 0.2, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
)

SUPPORTING_EVENTS_COUNT = Histogram(
    'genai_supporting_events_count',
    'Number of supporting events per query',
    buckets=[0, 1, 2, 5, 10, 20, 50, 100]
)

THREAT_INTEL_COUNT = Histogram(
    'genai_threat_intel_count',
    'Number of threat intel results per query',
    buckets=[0, 1, 2, 3, 5, 10]
)


# ============================================================================
# SYSTEM HEALTH
# ============================================================================

SERVICE_UP = Gauge(
    'genai_service_up',
    'Service health status (1=up, 0=down)'
)

DEPENDENCY_HEALTH = Gauge(
    'genai_dependency_health',
    'Dependency health status (1=healthy, 0=unhealthy)',
    ['dependency']  # dependency: java_api, rag_service, openai
)


# ============================================================================
# OPENAI PRICING (as of Oct 2024 - GPT-4o-mini)
# ============================================================================

OPENAI_PRICING = {
    'gpt-4o-mini': {
        'input': 0.15 / 1_000_000,   # $0.15 per 1M input tokens
        'output': 0.60 / 1_000_000,  # $0.60 per 1M output tokens
    },
    'gpt-4o': {
        'input': 2.50 / 1_000_000,   # $2.50 per 1M input tokens
        'output': 10.00 / 1_000_000, # $10.00 per 1M output tokens
    },
    'gpt-4-turbo': {
        'input': 10.00 / 1_000_000,  # $10.00 per 1M input tokens
        'output': 30.00 / 1_000_000, # $30.00 per 1M output tokens
    }
}


def get_model_pricing(model: str) -> Optional[Dict[str, float]]:
    """
    Get pricing for a model, matching by family if exact match not found.

    Handles versioned model names like 'gpt-4o-mini-2024-07-18' by matching
    the base model family 'gpt-4o-mini'.

    Args:
        model: Full model name from OpenAI API

    Returns:
        Pricing dict with 'input' and 'output' keys, or None if not found
    """
    # Try exact match first
    if model in OPENAI_PRICING:
        return OPENAI_PRICING[model]

    # Try matching by prefix (e.g., gpt-4o-mini-2024-07-18 -> gpt-4o-mini)
    for base_model in OPENAI_PRICING.keys():
        if model.startswith(base_model):
            logger.debug(f"Matched model '{model}' to pricing family '{base_model}'")
            return OPENAI_PRICING[base_model]

    logger.warning(f"No pricing found for model: {model}")
    return None


# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

@contextmanager
def track_query_duration(endpoint: str, status: str = "success"):
    """
    Context manager to track query duration

    Usage:
        with track_query_duration("/query", "success"):
            # Do query processing
            pass
    """
    start_time = time.time()
    try:
        yield
        QUERY_DURATION.labels(endpoint=endpoint, status=status).observe(time.time() - start_time)
    except Exception as e:
        QUERY_DURATION.labels(endpoint=endpoint, status="error").observe(time.time() - start_time)
        raise


@contextmanager
def track_data_source_call(source: str):
    """
    Context manager to track data source call duration

    Usage:
        with track_data_source_call("java_api"):
            data = await java_api.get_events()
    """
    start_time = time.time()
    status = "success"
    try:
        yield
    except Exception as e:
        status = "error"
        ERRORS_TOTAL.labels(error_type=source).inc()
        raise
    finally:
        duration = time.time() - start_time
        DATA_SOURCE_DURATION.labels(source=source).observe(duration)
        DATA_SOURCE_REQUESTS.labels(source=source, status=status).inc()


@contextmanager
def track_openai_call(model: str):
    """
    Context manager to track OpenAI API call

    Usage:
        with track_openai_call("gpt-4o-mini"):
            response = await openai_client.complete(...)
    """
    start_time = time.time()
    try:
        yield
        OPENAI_REQUESTS.labels(model=model, status="success").inc()
        OPENAI_DURATION.labels(model=model).observe(time.time() - start_time)
    except Exception as e:
        OPENAI_REQUESTS.labels(model=model, status="error").inc()
        ERRORS_TOTAL.labels(error_type="openai").inc()
        raise


def record_openai_usage(model: str, prompt_tokens: int, completion_tokens: int):
    """
    Record OpenAI token usage and estimate cost

    Args:
        model: Model name (e.g., "gpt-4o-mini" or "gpt-4o-mini-2024-07-18")
        prompt_tokens: Number of input tokens
        completion_tokens: Number of output tokens
    """
    logger.debug(f"Recording OpenAI usage - model: {model}, prompt: {prompt_tokens}, completion: {completion_tokens}")

    # Record token counts
    OPENAI_TOKENS.labels(model=model, token_type="prompt").inc(prompt_tokens)
    OPENAI_TOKENS.labels(model=model, token_type="completion").inc(completion_tokens)

    # Estimate and record cost using model family matching
    pricing = get_model_pricing(model)
    if pricing:
        input_cost = prompt_tokens * pricing['input']
        output_cost = completion_tokens * pricing['output']
        total_cost = input_cost + output_cost

        logger.info(f"OpenAI cost: ${total_cost:.6f} (model: {model}, {prompt_tokens}+{completion_tokens} tokens)")

        # Increment cost counter (use base model name for metric label consistency)
        # Extract base model for cleaner metrics (e.g., gpt-4o-mini-2024-07-18 -> gpt-4o-mini)
        base_model = model.split('-202')[0] if '-202' in model else model
        OPENAI_COST.labels(model=base_model).inc(total_cost)
    else:
        logger.warning(f"Cannot calculate cost for unknown model: {model}")


def record_query_metrics(
    confidence: float,
    supporting_events: int,
    threat_intel: int,
    include_threat_intel: bool
):
    """
    Record query result metrics

    Args:
        confidence: AI confidence score (0.0-1.0)
        supporting_events: Number of supporting events
        threat_intel: Number of threat intel results
        include_threat_intel: Whether threat intel was requested
    """
    QUERIES_TOTAL.labels(include_threat_intel=str(include_threat_intel).lower()).inc()
    CONFIDENCE_SCORE.observe(confidence)
    SUPPORTING_EVENTS_COUNT.observe(supporting_events)
    THREAT_INTEL_COUNT.observe(threat_intel)


def record_error(error_type: str):
    """
    Record an error occurrence

    Args:
        error_type: Type of error (openai, java_api, rag, validation, internal)
    """
    ERRORS_TOTAL.labels(error_type=error_type).inc()


def update_dependency_health(dependency: str, is_healthy: bool):
    """
    Update dependency health status

    Args:
        dependency: Name of dependency (java_api, rag_service, openai)
        is_healthy: Whether dependency is healthy
    """
    DEPENDENCY_HEALTH.labels(dependency=dependency).set(1 if is_healthy else 0)


def set_service_status(is_up: bool):
    """
    Set overall service status

    Args:
        is_up: Whether service is operational
    """
    SERVICE_UP.set(1 if is_up else 0)


# ============================================================================
# METRICS EXPORT
# ============================================================================

def get_metrics() -> tuple[bytes, str]:
    """
    Get Prometheus metrics in text format

    Returns:
        Tuple of (metrics_content, content_type)
    """
    return generate_latest(), CONTENT_TYPE_LATEST
