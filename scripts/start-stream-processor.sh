#!/bin/bash
# StreamGuard Stream Processor Startup Script
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
: "${KAFKA_BROKER:=localhost:9092}"
: "${KAFKA_TOPIC:=security-events}"
: "${KAFKA_GROUP_ID:=streamguard-processor}"
: "${STREAM_PROCESSOR_METRICS_PORT:=8080}"
: "${OPENAI_API_KEY:=}"

# Convert relative path to absolute path from project root
if [[ "$ROCKSDB_PATH" != /* ]]; then
    ROCKSDB_PATH="$PROJECT_ROOT/$ROCKSDB_PATH"
fi

echo "[Startup] Configuration:"
echo "  - Database path: $ROCKSDB_PATH"
echo "  - Kafka broker: $KAFKA_BROKER"
echo "  - Topic: $KAFKA_TOPIC"
echo "  - Metrics port: $STREAM_PROCESSOR_METRICS_PORT"

# Create data directory if it doesn't exist
DB_DIR="$(dirname "$ROCKSDB_PATH")"
if [ ! -d "$DB_DIR" ]; then
    echo "[Startup] Creating database directory: $DB_DIR"
    mkdir -p "$DB_DIR"
fi

# Find the stream-processor executable
EXECUTABLE=""
if [ -f "$PROJECT_ROOT/stream-processor/build/stream-processor" ]; then
    EXECUTABLE="$PROJECT_ROOT/stream-processor/build/stream-processor"
elif [ -f "$PROJECT_ROOT/stream-processor/cmake-build-debug/stream-processor" ]; then
    EXECUTABLE="$PROJECT_ROOT/stream-processor/cmake-build-debug/stream-processor"
else
    echo "[Error] stream-processor executable not found!"
    echo "[Error] Please build it first: cd stream-processor && mkdir build && cd build && cmake .. && make"
    exit 1
fi

echo "[Startup] Starting stream-processor..."
echo "[Startup] Executable: $EXECUTABLE"

# Start the processor with environment variables
export OPENAI_API_KEY
cd "$PROJECT_ROOT"
exec "$EXECUTABLE" \
    --broker "$KAFKA_BROKER" \
    --topic "$KAFKA_TOPIC" \
    --group "$KAFKA_GROUP_ID" \
    --db "$ROCKSDB_PATH" \
    --metrics-port "$STREAM_PROCESSOR_METRICS_PORT"
