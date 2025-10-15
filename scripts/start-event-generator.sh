#!/bin/bash
# StreamGuard Event Generator Startup Script
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
: "${KAFKA_BROKER:=localhost:9092}"
: "${KAFKA_TOPIC:=security-events}"
: "${EVENT_RATE:=100}"
: "${EVENT_DURATION:=0}"

echo "[Startup] Configuration:"
echo "  - Kafka broker: $KAFKA_BROKER"
echo "  - Topic: $KAFKA_TOPIC"
echo "  - Event rate: $EVENT_RATE events/sec"
if [ "$EVENT_DURATION" -gt 0 ]; then
    echo "  - Duration: $EVENT_DURATION seconds"
else
    echo "  - Duration: unlimited (Ctrl+C to stop)"
fi

# Find the event-generator JAR
JAR_FILE=""
if [ -f "$PROJECT_ROOT/event-generator/target/event-generator-1.0-SNAPSHOT.jar" ]; then
    JAR_FILE="$PROJECT_ROOT/event-generator/target/event-generator-1.0-SNAPSHOT.jar"
else
    echo "[Error] event-generator JAR not found!"
    echo "[Error] Please build it first: cd event-generator && mvn package"
    exit 1
fi

echo "[Startup] Starting event generator..."
echo "[Startup] JAR: $JAR_FILE"
echo ""

# Build arguments
ARGS="--broker $KAFKA_BROKER --topic $KAFKA_TOPIC --rate $EVENT_RATE"
if [ "$EVENT_DURATION" -gt 0 ]; then
    ARGS="$ARGS --duration $EVENT_DURATION"
fi

# Start the event generator
cd "$PROJECT_ROOT"
# shellcheck disable=SC2086
exec java -jar "$JAR_FILE" $ARGS
