#!/bin/bash
# StreamGuard Query API Startup Script
# This script ensures proper environment configuration before starting

set -e  # Exit on error

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "[Startup] Loading configuration from .env..."
    set -a  # Auto-export all variables
    # shellcheck disable=SC1090
    source "$PROJECT_ROOT/.env"
    set +a
else
    echo "[Warning] No .env file found at $PROJECT_ROOT/.env"
    echo "[Warning] Using default configuration"
fi

# Set defaults if not provided
: "${ROCKSDB_PATH:=./data/events.db}"
: "${QUERY_API_PORT:=8081}"
: "${OPENAI_API_KEY:=}"

# Convert relative path to absolute path from project root
if [[ "$ROCKSDB_PATH" != /* ]]; then
    ROCKSDB_PATH="$PROJECT_ROOT/$ROCKSDB_PATH"
fi

echo "[Startup] Configuration:"
echo "  - Database path: $ROCKSDB_PATH (read-only)"
echo "  - API port: $QUERY_API_PORT"

# Check if database exists
if [ ! -d "$ROCKSDB_PATH" ]; then
    echo "[Error] RocksDB database not found at: $ROCKSDB_PATH"
    echo "[Error] Please start stream-processor first to create the database"
    exit 1
fi

# Find the query-api JAR
JAR_FILE=""
if [ -f "$PROJECT_ROOT/query-api/target/query-api-1.0.0.jar" ]; then
    JAR_FILE="$PROJECT_ROOT/query-api/target/query-api-1.0.0.jar"
else
    echo "[Error] query-api JAR not found!"
    echo "[Error] Please build it first: cd query-api && mvn package"
    exit 1
fi

echo "[Startup] Starting query-api..."
echo "[Startup] JAR: $JAR_FILE"

# Start the API with environment variables
export ROCKSDB_PATH
export OPENAI_API_KEY
cd "$PROJECT_ROOT"
exec java -jar "$JAR_FILE"
