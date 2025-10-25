#!/bin/bash
# StreamGuard GenAI Assistant Startup Script
# This script ensures proper environment configuration before starting

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENAI_DIR="$PROJECT_ROOT/genai-assistant"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}StreamGuard GenAI Assistant Startup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo -e "${GREEN}[Startup]${NC} Loading configuration from .env..."
    set -a  # Auto-export all variables
    # shellcheck disable=SC1090
    source "$PROJECT_ROOT/.env"
    set +a
else
    echo -e "${RED}[Error]${NC} No .env file found at $PROJECT_ROOT/.env"
    echo -e "${YELLOW}[Info]${NC} Please create .env from .env.example"
    exit 1
fi

# Set defaults if not provided
: "${LLM_PROVIDER:=openai}"
: "${SERVICE_HOST:=0.0.0.0}"
: "${SERVICE_PORT:=8002}"
: "${JAVA_API_URL:=http://localhost:8081}"
: "${RAG_SERVICE_URL:=http://localhost:8000}"

echo -e "${GREEN}[Startup]${NC} Configuration:"
echo "  - LLM Provider: $LLM_PROVIDER"
echo "  - Service: $SERVICE_HOST:$SERVICE_PORT"
echo "  - Java API: $JAVA_API_URL"
echo "  - RAG Service: $RAG_SERVICE_URL"
echo ""

# Check Docker
echo -e "${BLUE}[Check]${NC} Verifying Docker..."
if ! docker ps > /dev/null 2>&1; then
    echo -e "${RED}[Error]${NC} Docker is not running"
    echo -e "${YELLOW}[Action]${NC} Please start Docker Desktop"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Docker is running"

# Check Java API
echo -e "${BLUE}[Check]${NC} Verifying Java API..."
if ! curl -s -f "$JAVA_API_URL/health" > /dev/null 2>&1; then
    echo -e "${YELLOW}[Warning]${NC} Java API not reachable at $JAVA_API_URL"
    echo -e "${YELLOW}[Action]${NC} Start it first: ./scripts/start-query-api.sh"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}[OK]${NC} Java API is reachable"
fi

# Check RAG Service (optional)
echo -e "${BLUE}[Check]${NC} Verifying RAG service..."
if ! curl -s -f "$RAG_SERVICE_URL/health" > /dev/null 2>&1; then
    echo -e "${YELLOW}[Warning]${NC} RAG service not reachable (optional)"
else
    echo -e "${GREEN}[OK]${NC} RAG service is reachable"
fi
echo ""

# Check Python
echo -e "${BLUE}[Check]${NC} Verifying Python..."
if ! command -v python3 > /dev/null 2>&1; then
    echo -e "${RED}[Error]${NC} Python 3 not found"
    echo -e "${YELLOW}[Action]${NC} Please install Python 3.11+"
    exit 1
fi

PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
echo -e "${GREEN}[OK]${NC} Python $PYTHON_VERSION found"

# Validate LLM Provider Configuration
echo ""
echo -e "${BLUE}[Check]${NC} Validating LLM provider configuration..."

if [ "$LLM_PROVIDER" = "openai" ]; then
    echo -e "${GREEN}[Config]${NC} Using OpenAI GPT-4o-mini"

    if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "sk-your-key-here" ]; then
        echo -e "${RED}[Error]${NC} OPENAI_API_KEY not set or using example value"
        echo -e "${YELLOW}[Action]${NC} Set OPENAI_API_KEY in .env file"
        exit 1
    fi

    echo -e "${GREEN}[OK]${NC} OpenAI API key configured"
    echo "  - Model: ${OPENAI_MODEL:-gpt-4o-mini}"

elif [ "$LLM_PROVIDER" = "ollama" ]; then
    echo -e "${GREEN}[Config]${NC} Using Ollama (local model)"

    OLLAMA_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"
    echo "  - Base URL: $OLLAMA_URL"
    echo "  - Model: ${OLLAMA_MODEL:-llama3.2:latest}"

    # Check if Ollama is running
    if ! curl -s -f "$OLLAMA_URL/api/tags" > /dev/null 2>&1; then
        echo -e "${RED}[Error]${NC} Ollama not running at $OLLAMA_URL"
        echo -e "${YELLOW}[Action]${NC} Start Ollama: ollama serve"
        exit 1
    fi

    echo -e "${GREEN}[OK]${NC} Ollama is running"

    # Check if model is available
    OLLAMA_MODEL_NAME="${OLLAMA_MODEL:-llama3.2:latest}"
    if ! curl -s "$OLLAMA_URL/api/tags" | grep -q "\"name\":\"$OLLAMA_MODEL_NAME\""; then
        echo -e "${YELLOW}[Warning]${NC} Model '$OLLAMA_MODEL_NAME' not found"
        echo -e "${YELLOW}[Info]${NC} Available models:"
        curl -s "$OLLAMA_URL/api/tags" | grep -o '"name":"[^"]*"' | cut -d'"' -f4 | sed 's/^/    - /'
        echo ""
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "${GREEN}[OK]${NC} Model '$OLLAMA_MODEL_NAME' is available"
    fi

else
    echo -e "${RED}[Error]${NC} Unknown LLM_PROVIDER: $LLM_PROVIDER"
    echo -e "${YELLOW}[Action]${NC} Set LLM_PROVIDER to 'openai' or 'ollama' in .env"
    exit 1
fi

# Setup Python virtual environment
echo ""
echo -e "${BLUE}[Setup]${NC} Preparing Python environment..."
cd "$GENAI_DIR"

if [ ! -d "venv" ]; then
    echo -e "${GREEN}[Setup]${NC} Creating virtual environment..."
    python3 -m venv venv
fi

echo -e "${GREEN}[Setup]${NC} Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo -e "${GREEN}[Setup]${NC} Installing dependencies..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

echo ""
echo -e "${GREEN}[Ready]${NC} Starting GenAI Assistant..."
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Service:${NC}     http://$SERVICE_HOST:$SERVICE_PORT"
echo -e "${GREEN}API Docs:${NC}    http://localhost:$SERVICE_PORT/docs"
echo -e "${GREEN}Metrics:${NC}     http://localhost:$SERVICE_PORT/metrics"
echo -e "${GREEN}Provider:${NC}    $LLM_PROVIDER"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop the service${NC}"
echo ""

# Start service
exec uvicorn app.main:app --host "$SERVICE_HOST" --port "$SERVICE_PORT"
