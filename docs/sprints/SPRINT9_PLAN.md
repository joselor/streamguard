# Sprint 9 Plan: GenAI Observability & Local Model Support

**Sprint Duration:** TBD
**Sprint Goal:** Enhance GenAI Assistant with production-grade observability and add local model support for cost-effective demos

---

## 🎯 Sprint Objectives

1. **Observability**: Add Prometheus metrics and Grafana dashboard for GenAI service monitoring
2. **Local Model Support**: Enable local LLM deployment (Ollama) for cost savings and demo flexibility
3. **Startup Automation**: Create startup script for GenAI assistant following Sprint 5 patterns

---

## 📊 Capability Analysis

### 1. Observability (Prometheus + Grafana)

#### **Value Assessment: HIGH ⭐⭐⭐⭐⭐**

**Business Value:**
- **Production Readiness**: Essential for monitoring service health in production
- **Cost Tracking**: Monitor OpenAI API usage and estimated costs
- **Performance Insights**: Identify slow queries and bottlenecks
- **Error Detection**: Quickly identify and respond to failures
- **Demo Polish**: Demonstrates professional observability practices

**Stakeholder Impact:**
- **Security Analysts**: Visibility into query performance
- **DevOps**: Service health monitoring and alerting
- **Management**: Cost visibility and usage analytics
- **Job Application**: Shows production-thinking and observability expertise

**Metrics ROI:**
- Track OpenAI costs → Optimize prompt engineering
- Monitor latency → Identify performance bottlenecks
- Error rates → Improve reliability
- Confidence scores → Tune AI response quality

#### **Complexity Assessment: MEDIUM 🟡**

**Technical Complexity:**
- **Low**: prometheus-client already in requirements.txt
- **Medium**: Strategic metric placement in code
- **Medium**: Grafana dashboard design and JSON configuration
- **Low**: Integration with existing Prometheus instance

**Estimated Effort:** 4-6 hours
- Metric instrumentation: 2 hours
- Grafana dashboard: 2 hours
- Testing and validation: 2 hours

**Implementation Challenges:**
- Accurate OpenAI cost estimation (model-dependent pricing)
- Histogram bucket tuning for latency metrics
- Grafana dashboard layout design
- Testing metric collection end-to-end

#### **Dependencies**

**Hard Dependencies:**
- ✅ Prometheus running (docker-compose) - ALREADY RUNNING
- ✅ Grafana running (docker-compose) - ALREADY RUNNING
- ✅ prometheus-client library - ALREADY IN requirements.txt
- ⚠️ GenAI assistant metrics endpoint (need to add)

**Soft Dependencies:**
- Existing Grafana dashboard patterns (reference for consistency)
- OpenAI pricing data (for cost estimation)

**Blockers:** None - All infrastructure is ready

#### **Proposed Metrics**

```python
# Core metrics to implement

# 1. Query Performance
genai_query_duration_seconds (histogram)
  - Labels: endpoint, status
  - Buckets: [0.5, 1.0, 2.0, 5.0, 10.0, 30.0]

# 2. OpenAI Usage
genai_openai_requests_total (counter)
  - Labels: model, status

genai_openai_tokens_total (counter)
  - Labels: model, token_type (prompt/completion)

genai_openai_cost_dollars (counter)
  - Labels: model

# 3. Error Tracking
genai_errors_total (counter)
  - Labels: error_type (openai, java_api, rag, validation)

# 4. Data Source Performance
genai_data_source_duration_seconds (histogram)
  - Labels: source (java_api, rag_service, rocksdb)
  - Buckets: [0.1, 0.25, 0.5, 1.0, 2.0]

# 5. AI Quality
genai_confidence_score (histogram)
  - Labels: none
  - Buckets: [0.0, 0.3, 0.5, 0.7, 0.85, 1.0]

# 6. Request Counts
genai_queries_total (counter)
  - Labels: include_threat_intel (true/false)
```

---

### 2. Local Model Support (Ollama Integration)

#### **Value Assessment: VERY HIGH ⭐⭐⭐⭐⭐**

**Business Value:**
- **Cost Savings**: Zero OpenAI API costs for demos/development ($0.15-0.60 per 1K tokens → $0)
- **Faster Response**: No network latency (1-3s → <1s potential)
- **Privacy**: All data stays local
- **Demo Flexibility**: Run without internet or API keys
- **Architectural Excellence**: Shows design for flexibility and configurability

**Stakeholder Impact:**
- **Demos**: Run unlimited queries without cost concerns
- **Development**: Test without API key dependencies
- **Job Application**: Demonstrates architectural thinking and cost awareness
- **Privacy-Conscious Users**: Keep security data local

**Cost Savings Analysis:**
- OpenAI GPT-4o-mini: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- Average query: ~2K input tokens + 500 output tokens = ~$0.0006/query
- 1000 demo queries: $0.60 (OpenAI) → $0 (local)
- **Unlimited demo queries with local model**

#### **Complexity Assessment: MEDIUM-HIGH 🟡🔴**

**Technical Complexity:**
- **Medium**: Ollama integration and model management
- **High**: LLM provider abstraction layer
- **Medium**: Prompt compatibility testing (local models may need tuning)
- **Low**: Configuration management (model selection)

**Estimated Effort:** 6-8 hours
- LLM abstraction layer: 3 hours
- Ollama integration: 2 hours
- Prompt tuning and testing: 2 hours
- Documentation: 1 hour

**Implementation Challenges:**
- Different prompt formats for different models
- Local model quality vs OpenAI (may need prompt adjustments)
- Model download size (4-8GB for good models)
- Mac M1 optimization (ensure native ARM64 support)
- Fallback logic if local model unavailable

#### **Dependencies**

**Hard Dependencies:**
- ⚠️ Ollama installation (not yet installed)
- ⚠️ Local model download (llama3.1, mistral, phi-3)
- ⚠️ LLM abstraction code (need to implement)
- ⚠️ Configuration updates (.env, config.py)

**Soft Dependencies:**
- Sufficient disk space for models (10-20GB)
- Adequate RAM (16GB+ recommended)
- Mac M1 for optimal performance

**Blockers:**
- User needs to install Ollama manually
- First model download takes 5-10 minutes

#### **Recommended Local Models**

**Option 1: Llama 3.1 8B (Recommended)**
- Size: ~4.7GB
- Quality: Excellent for security analysis
- Speed: Fast on M1
- Command: `ollama pull llama3.1:8b`

**Option 2: Mistral 7B**
- Size: ~4.1GB
- Quality: Very good, concise responses
- Speed: Very fast
- Command: `ollama pull mistral:7b`

**Option 3: Phi-3 Medium**
- Size: ~7.9GB
- Quality: Good for reasoning tasks
- Speed: Moderate
- Command: `ollama pull phi3:medium`

#### **Architecture Design**

```python
# LLM Provider Abstraction

class BaseLLMProvider(ABC):
    @abstractmethod
    async def complete(self, messages, temperature, max_tokens):
        pass

class OpenAIProvider(BaseLLMProvider):
    async def complete(self, messages, temperature, max_tokens):
        # Existing OpenAI implementation
        pass

class OllamaProvider(BaseLLMProvider):
    async def complete(self, messages, temperature, max_tokens):
        # New Ollama implementation via httpx
        async with httpx.AsyncClient() as client:
            response = await client.post(
                "http://localhost:11434/api/chat",
                json={
                    "model": self.model,
                    "messages": messages,
                    "temperature": temperature,
                    "options": {"num_predict": max_tokens}
                }
            )
            return response.json()

class LLMFactory:
    @staticmethod
    def create(provider_type: str) -> BaseLLMProvider:
        if provider_type == "openai":
            return OpenAIProvider()
        elif provider_type == "ollama":
            return OllamaProvider()
        else:
            raise ValueError(f"Unknown provider: {provider_type}")
```

---

### 3. Startup Script Automation

#### **Value Assessment: MEDIUM ⭐⭐⭐**

**Business Value:**
- **Consistency**: Follows Sprint 5 script patterns
- **User Convenience**: One-command startup
- **Documentation**: Self-documenting setup process
- **Demo Polish**: Professional startup experience

**Stakeholder Impact:**
- **Developers**: Easy local development setup
- **Demo Audience**: Quick service startup
- **Documentation**: Clear dependencies and checks

**Time Savings:**
- Manual startup: ~3-5 minutes (check deps, activate venv, start service)
- Script startup: ~30 seconds
- **Saves 2.5-4.5 minutes per startup**

#### **Complexity Assessment: LOW 🟢**

**Technical Complexity:**
- **Low**: Follow existing script patterns
- **Low**: Standard shell scripting
- **Low**: Environment validation

**Estimated Effort:** 2-3 hours
- Script development: 1 hour
- Testing: 1 hour
- Documentation: 30 minutes

**Implementation Challenges:**
- Python virtual environment detection
- Dependency checking (Docker, OpenAI key, etc.)
- Graceful error messages
- Cross-platform compatibility (Mac focus)

#### **Dependencies**

**Hard Dependencies:**
- ✅ Existing script patterns (start-stream-processor.sh, etc.)
- ✅ .env file structure
- ⚠️ Python virtual environment (may or may not exist)
- ⚠️ Docker running (for Java API dependency)

**Soft Dependencies:**
- RAG service running (can work without it)
- OpenAI API key (only if using OpenAI provider)

**Blockers:** None

#### **Script Requirements**

```bash
# scripts/start-genai-assistant.sh

# Must check:
1. Docker running (for Java API dependency)
2. Java API reachable (http://localhost:8081/health)
3. RAG service reachable (optional, http://localhost:8000/health)
4. Python 3.11+ installed
5. Virtual environment exists or create it
6. Dependencies installed (requirements.txt)
7. .env file exists with OPENAI_API_KEY or LLM_PROVIDER=ollama
8. If using Ollama, check it's running (http://localhost:11434/api/tags)
9. Start uvicorn with proper config
```

---

## 🎯 Sprint 9 Scope Summary

| Capability | Value | Complexity | Effort | Dependencies | Priority |
|-----------|-------|------------|--------|--------------|----------|
| **Observability** | HIGH | MEDIUM | 4-6h | ✅ Ready | **P0** |
| **Local Model** | VERY HIGH | MEDIUM-HIGH | 6-8h | ⚠️ Ollama install | **P1** |
| **Startup Script** | MEDIUM | LOW | 2-3h | ✅ Ready | **P2** |

**Total Estimated Effort:** 12-17 hours (~2-3 day sprint)

---

## 📋 Implementation Plan

### Phase 1: Observability (Priority P0)

**Goal:** Add Prometheus metrics and Grafana dashboard

#### Task 1.1: Instrument GenAI Assistant with Metrics (2h)
**Files to modify:**
- `genai-assistant/app/main.py` - Add metrics endpoint
- `genai-assistant/app/services/assistant.py` - Add metric instrumentation
- `genai-assistant/app/utils/metrics.py` - **NEW** - Metrics definitions

**Implementation:**
```python
# app/utils/metrics.py
from prometheus_client import Counter, Histogram, generate_latest

# Define metrics
QUERY_DURATION = Histogram(
    'genai_query_duration_seconds',
    'Time spent processing queries',
    ['endpoint', 'status'],
    buckets=[0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
)

OPENAI_REQUESTS = Counter(
    'genai_openai_requests_total',
    'Total OpenAI API requests',
    ['model', 'status']
)

# ... more metrics
```

**Instrumentation points:**
- Before/after query processing (duration)
- Before/after OpenAI calls (tokens, cost, errors)
- Before/after data source calls (java_api, rag_service)
- On confidence score calculation

#### Task 1.2: Add Metrics Endpoint (1h)
**Files to modify:**
- `genai-assistant/app/main.py`

```python
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST

@app.get("/metrics")
async def metrics():
    return Response(
        content=generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )
```

#### Task 1.3: Update Prometheus Configuration (30min)
**Files to modify:**
- `monitoring/prometheus/prometheus.yml`

```yaml
scrape_configs:
  # ... existing configs ...

  - job_name: 'genai-assistant'
    static_configs:
      - targets: ['genai-assistant:8002']
    metrics_path: '/metrics'
    scrape_interval: 15s
```

#### Task 1.4: Create Grafana Dashboard (2h)
**Files to create:**
- `monitoring/grafana/dashboards/streamguard-genai.json`

**Dashboard panels:**
1. **Query Performance**
   - Query rate (queries/min)
   - P50/P95/P99 latency
   - Error rate

2. **OpenAI Usage**
   - API calls per minute
   - Tokens per minute (prompt/completion)
   - Estimated cost per hour

3. **Data Sources**
   - Java API latency
   - RAG service latency
   - Error rates by source

4. **AI Quality**
   - Confidence score distribution
   - Low confidence alerts (<0.5)

5. **System Health**
   - Service status
   - Dependency health

#### Task 1.5: Test Metrics Collection (1h)
- Start services via docker-compose
- Generate sample queries
- Verify metrics appear in Prometheus
- Verify dashboard displays correctly

**Acceptance Criteria:**
- ✅ Metrics endpoint returns Prometheus format
- ✅ Prometheus scrapes metrics successfully
- ✅ Grafana dashboard displays all panels
- ✅ Metrics update in real-time during queries
- ✅ Cost estimation is accurate

---

### Phase 2: Local Model Support (Priority P1)

**Goal:** Enable Ollama integration for local LLM deployment

#### Task 2.1: Install and Test Ollama (30min - MANUAL)
**User action required:**
```bash
# Install Ollama
brew install ollama

# Start Ollama service
ollama serve

# Pull recommended model
ollama pull llama3.1:8b

# Test it works
ollama run llama3.1:8b "What is a brute force attack?"
```

#### Task 2.2: Create LLM Provider Abstraction (3h)
**Files to create:**
- `genai-assistant/app/llm/__init__.py`
- `genai-assistant/app/llm/base.py` - **NEW** - Base provider interface
- `genai-assistant/app/llm/openai_provider.py` - **NEW** - OpenAI implementation
- `genai-assistant/app/llm/ollama_provider.py` - **NEW** - Ollama implementation
- `genai-assistant/app/llm/factory.py` - **NEW** - Provider factory

**Base Interface:**
```python
# app/llm/base.py
from abc import ABC, abstractmethod
from typing import List, Dict, Any

class BaseLLMProvider(ABC):
    @abstractmethod
    async def complete(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> Dict[str, Any]:
        """Generate completion from messages"""
        pass

    @abstractmethod
    async def health_check(self) -> bool:
        """Check if provider is available"""
        pass
```

#### Task 2.3: Implement OpenAI Provider (1h)
**Files to create:**
- `genai-assistant/app/llm/openai_provider.py`

**Move existing OpenAI logic into provider class**

#### Task 2.4: Implement Ollama Provider (2h)
**Files to create:**
- `genai-assistant/app/llm/ollama_provider.py`

```python
class OllamaProvider(BaseLLMProvider):
    def __init__(self, model: str = "llama3.1:8b", base_url: str = "http://localhost:11434"):
        self.model = model
        self.base_url = base_url

    async def complete(self, messages, temperature, max_tokens):
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{self.base_url}/api/chat",
                json={
                    "model": self.model,
                    "messages": messages,
                    "temperature": temperature,
                    "stream": False,
                    "options": {
                        "num_predict": max_tokens,
                        "temperature": temperature
                    }
                }
            )
            data = response.json()
            return {
                "content": data["message"]["content"],
                "model": self.model,
                "usage": {
                    "prompt_tokens": data.get("prompt_eval_count", 0),
                    "completion_tokens": data.get("eval_count", 0)
                }
            }
```

#### Task 2.5: Update Configuration (30min)
**Files to modify:**
- `genai-assistant/app/config.py`
- `genai-assistant/.env.example`

```python
# config.py additions
class Settings(BaseSettings):
    # ... existing fields ...

    # LLM Provider Configuration
    llm_provider: str = "openai"  # "openai" or "ollama"

    # OpenAI settings (existing)
    openai_api_key: Optional[str] = None
    openai_model: str = "gpt-4o-mini"

    # Ollama settings (new)
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.1:8b"
```

#### Task 2.6: Integrate Provider into Assistant (1h)
**Files to modify:**
- `genai-assistant/app/services/assistant.py`

```python
from app.llm.factory import LLMFactory

class SecurityAssistant:
    def __init__(self):
        # Replace direct OpenAI client with factory
        self.llm = LLMFactory.create(settings.llm_provider)
        self.java_api = JavaAPIClient()
        self.rag_client = RAGClient()
```

#### Task 2.7: Test Both Providers (2h)
- Test OpenAI provider (existing functionality)
- Test Ollama provider with llama3.1:8b
- Compare response quality
- Tune prompts if needed for Ollama

**Acceptance Criteria:**
- ✅ Can switch between OpenAI and Ollama via config
- ✅ Both providers return valid responses
- ✅ Ollama responses are security-relevant and accurate
- ✅ Health checks work for both providers
- ✅ Error handling for unavailable provider

---

### Phase 3: Startup Script (Priority P2)

**Goal:** Create startup script following Sprint 5 patterns

#### Task 3.1: Create Startup Script (1h)
**Files to create:**
- `scripts/start-genai-assistant.sh`

```bash
#!/bin/bash
# StreamGuard GenAI Assistant Startup Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENAI_DIR="$PROJECT_ROOT/genai-assistant"

echo "[Startup] StreamGuard GenAI Assistant"
echo "======================================="

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "[Startup] Loading configuration from .env..."
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
else
    echo "[Error] No .env file found at $PROJECT_ROOT/.env"
    exit 1
fi

# Check Docker
if ! docker ps > /dev/null 2>&1; then
    echo "[Error] Docker is not running. Please start Docker Desktop."
    exit 1
fi

# Check Java API
if ! curl -s http://localhost:8081/health > /dev/null; then
    echo "[Warning] Java API not reachable at http://localhost:8081"
    echo "[Warning] Start it first: ./scripts/start-query-api.sh"
    exit 1
fi

# Check RAG Service (optional)
if ! curl -s http://localhost:8000/health > /dev/null 2>&1; then
    echo "[Warning] RAG service not reachable (optional)"
fi

# Check Python
if ! command -v python3 > /dev/null; then
    echo "[Error] Python 3 not found. Please install Python 3.11+"
    exit 1
fi

# Check LLM Provider
if [ "${LLM_PROVIDER:-openai}" = "openai" ]; then
    if [ -z "$OPENAI_API_KEY" ]; then
        echo "[Error] OPENAI_API_KEY not set in .env"
        exit 1
    fi
    echo "[Startup] Using OpenAI GPT-4o-mini"
elif [ "${LLM_PROVIDER}" = "ollama" ]; then
    if ! curl -s http://localhost:11434/api/tags > /dev/null; then
        echo "[Error] Ollama not running. Start it: ollama serve"
        exit 1
    fi
    echo "[Startup] Using Ollama (${OLLAMA_MODEL:-llama3.1:8b})"
fi

# Setup virtual environment
cd "$GENAI_DIR"
if [ ! -d "venv" ]; then
    echo "[Startup] Creating virtual environment..."
    python3 -m venv venv
fi

echo "[Startup] Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "[Startup] Installing dependencies..."
pip install -q -r requirements.txt

# Start service
echo "[Startup] Starting GenAI Assistant on port 8002..."
echo "[Startup] API docs: http://localhost:8002/docs"
echo "[Startup] Metrics: http://localhost:8002/metrics"
echo ""

uvicorn app.main:app --host 0.0.0.0 --port 8002
```

#### Task 3.2: Make Script Executable and Test (30min)
```bash
chmod +x scripts/start-genai-assistant.sh
./scripts/start-genai-assistant.sh
```

#### Task 3.3: Update Documentation (1h)
**Files to modify:**
- `README.md` - Add startup script to Quick Start
- `genai-assistant/README.md` - Update with script usage
- `docs/product/guides/QUICK_START.md` - Add to setup flow

**Acceptance Criteria:**
- ✅ Script validates all dependencies
- ✅ Clear error messages for missing dependencies
- ✅ Follows existing script patterns
- ✅ Works with both OpenAI and Ollama
- ✅ Documentation updated

---

## 🧪 Testing Strategy

### Unit Tests
```python
# tests/test_llm_providers.py
async def test_openai_provider():
    provider = OpenAIProvider()
    response = await provider.complete(
        messages=[{"role": "user", "content": "test"}],
        temperature=0.7,
        max_tokens=100
    )
    assert response["content"]
    assert response["model"]

async def test_ollama_provider():
    provider = OllamaProvider()
    response = await provider.complete(
        messages=[{"role": "user", "content": "test"}],
        temperature=0.7,
        max_tokens=100
    )
    assert response["content"]
```

### Integration Tests
```bash
# Test full flow with both providers

# 1. Start services
docker-compose up -d

# 2. Test with OpenAI
export LLM_PROVIDER=openai
./scripts/start-genai-assistant.sh &
curl -X POST http://localhost:8002/query -d '{"question": "test"}'

# 3. Test with Ollama
export LLM_PROVIDER=ollama
ollama serve &
./scripts/start-genai-assistant.sh &
curl -X POST http://localhost:8002/query -d '{"question": "test"}'

# 4. Verify metrics
curl http://localhost:8002/metrics | grep genai_

# 5. Check Grafana dashboard
open http://localhost:3000
```

### Manual Testing Checklist
- [ ] Metrics appear in Prometheus (:9090)
- [ ] Grafana dashboard loads (:3000)
- [ ] Dashboard updates in real-time
- [ ] Cost estimates are accurate
- [ ] OpenAI provider works
- [ ] Ollama provider works
- [ ] Provider switching works
- [ ] Startup script validates dependencies
- [ ] Error messages are clear
- [ ] Documentation is accurate

---

## ⚠️ Risk Assessment

### High Risks

**1. Ollama Quality vs OpenAI**
- **Risk**: Local model responses may be lower quality
- **Mitigation**: Test thoroughly, tune prompts, document differences
- **Fallback**: Keep OpenAI as default, Ollama as opt-in

**2. Model Download Size**
- **Risk**: 4-8GB download may be problematic
- **Mitigation**: Document requirement, provide model recommendations
- **Fallback**: Smaller models available (phi-3, mistral)

### Medium Risks

**3. Metrics Overhead**
- **Risk**: Metric collection may slow down queries
- **Mitigation**: Use efficient prometheus-client, test performance
- **Fallback**: Make metrics optional via config

**4. Prompt Compatibility**
- **Risk**: Prompts optimized for GPT-4 may not work with Llama
- **Mitigation**: Test and tune prompts per provider
- **Fallback**: Provider-specific prompt templates

### Low Risks

**5. Grafana Dashboard Complexity**
- **Risk**: Dashboard may be hard to design
- **Mitigation**: Reference existing dashboards, start simple
- **Fallback**: Minimal dashboard, iterate later

---

## 📈 Success Metrics

### Observability Success
- ✅ All metrics collecting and displayed in Grafana
- ✅ Dashboard updates within 15 seconds of query
- ✅ Cost estimation within 10% of actual OpenAI billing
- ✅ <1ms metric collection overhead

### Local Model Success
- ✅ Ollama integration works seamlessly
- ✅ Response quality acceptable for demos (subjective review)
- ✅ Latency <2s for local model queries
- ✅ Zero OpenAI costs when using Ollama

### Startup Script Success
- ✅ Script starts service in <60 seconds
- ✅ All dependency checks work correctly
- ✅ Clear error messages for failures
- ✅ Works on Mac M1 (primary target)

---

## 📚 Documentation Deliverables

1. **SPRINT9_PLAN.md** ← This document
2. **PROJECT_HANDOFF_SPRINT9.md** - Sprint closure document
3. **genai-assistant/README.md updates** - Local model setup instructions
4. **Root README.md updates** - Startup script reference
5. **docs/product/guides/LOCAL_MODEL_SETUP.md** - NEW - Ollama setup guide
6. **Inline code comments** - Explain metrics and provider abstraction

---

## 🎯 Sprint Priorities

### Must-Have (P0)
- ✅ Prometheus metrics instrumentation
- ✅ Grafana dashboard creation
- ✅ Metrics endpoint

### Should-Have (P1)
- ✅ Ollama provider implementation
- ✅ LLM abstraction layer
- ✅ Provider switching

### Nice-to-Have (P2)
- ✅ Startup script
- ✅ Documentation updates
- ⚠️ Provider-specific prompt tuning (if time permits)

---

## 🚀 Sprint Kickoff Checklist

Before starting Sprint 9:

### Infrastructure
- [ ] Verify Prometheus running (`http://localhost:9090`)
- [ ] Verify Grafana running (`http://localhost:3000`)
- [ ] Install Ollama (`brew install ollama`)
- [ ] Pull Llama model (`ollama pull llama3.1:8b`)
- [ ] Test Ollama works (`ollama run llama3.1:8b "test"`)

### Development Environment
- [ ] GenAI assistant runs successfully
- [ ] Java API running (`http://localhost:8081`)
- [ ] RAG service running (`http://localhost:8000`)
- [ ] Can generate test queries

### Documentation Review
- [ ] Review existing Grafana dashboards for patterns
- [ ] Review existing startup scripts for patterns
- [ ] Understand OpenAI pricing model

---

## 📊 Estimated Timeline

**Total Sprint Duration:** 12-17 hours (~2-3 days)

### Day 1: Observability (4-6h)
- Morning: Metric instrumentation (2-3h)
- Afternoon: Grafana dashboard (2-3h)
- End of day: Test and validate

### Day 2: Local Model (6-8h)
- Morning: LLM abstraction and OpenAI provider (2-3h)
- Afternoon: Ollama provider and integration (3-4h)
- Evening: Testing and prompt tuning (1h)

### Day 3: Polish (2-3h)
- Morning: Startup script (1-2h)
- Afternoon: Documentation and testing (1h)

---

## 🎓 Learning Objectives

### Technical Skills
- Prometheus metrics design and instrumentation
- Grafana dashboard creation
- LLM provider abstraction patterns
- Ollama integration
- Bash scripting best practices

### Domain Knowledge
- Observability best practices for AI services
- OpenAI pricing and cost optimization
- Local LLM deployment strategies
- Mac M1 AI tooling

---

## 🏁 Sprint Definition of Done

- [ ] All metrics collecting successfully
- [ ] Grafana dashboard displays correctly
- [ ] OpenAI provider works (existing functionality)
- [ ] Ollama provider works (new functionality)
- [ ] Can switch providers via configuration
- [ ] Startup script works end-to-end
- [ ] All documentation updated
- [ ] Manual testing complete
- [ ] Code committed and pushed to GitHub
- [ ] Sprint handoff document created

---

**Sprint Status:** 📝 **PLANNED** - Ready for kickoff
**Next Step:** Review plan, confirm scope, begin Phase 1 (Observability)
