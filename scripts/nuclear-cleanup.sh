#!/bin/bash
# Nuclear cleanup script - removes ALL StreamGuard data and artifacts

echo "🧹 Starting StreamGuard Nuclear Cleanup..."
echo ""
echo "⚠️  WARNING: This will delete data, and optionally builds and Docker volumes!"
echo ""
echo "Choose cleanup level:"
echo "  1) Light cleanup - Keep built applications (JARs/binaries)"
echo "  2) Full cleanup - Remove everything including builds"
echo ""
read -r -p "Enter choice (1 or 2): " cleanup_level

if [[ "$cleanup_level" != "1" && "$cleanup_level" != "2" ]]; then
    echo "Invalid choice. Cleanup cancelled."
    exit 0
fi

if [ "$cleanup_level" = "1" ]; then
    echo ""
    echo "Light cleanup: Will keep target/ and build/ directories"
    SKIP_BUILDS=true
else
    echo ""
    echo "Full cleanup: Will remove ALL artifacts including builds"
    SKIP_BUILDS=false
fi

echo ""
read -r -p "Are you sure you want to proceed? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

# Stop all running processes
echo "[1/8] Stopping running processes..."
pkill -f stream-processor
pkill -f query-api
sleep 2

# Stop and remove Docker containers
echo "[2/8] Stopping Docker containers..."
docker-compose down -v 2>/dev/null || true

# Remove Docker volumes
echo "[3/8] Removing Docker volumes..."
docker volume rm streamguard_kafka-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-data 2>/dev/null || true
docker volume rm streamguard_zookeeper-logs 2>/dev/null || true
docker volume rm streamguard_prometheus-data 2>/dev/null || true
docker volume rm streamguard_grafana-data 2>/dev/null || true
docker volume rm streamguard_spark-data 2>/dev/null || true

# Remove RocksDB database
echo "[4/8] Removing RocksDB database..."
rm -rf data/events.db
rm -rf stream-processor/build/data

# Remove C++ build artifacts
if [ "$SKIP_BUILDS" = false ]; then
    echo "[5/8] Removing C++ build artifacts..."
    rm -rf stream-processor/build
    rm -rf stream-processor/cmake-build-debug
    rm -rf stream-processor/cmake-build-release
else
    echo "[5/8] Skipping C++ build artifacts (keeping stream-processor/build/)"
fi

# Remove Java build artifacts
if [ "$SKIP_BUILDS" = false ]; then
    echo "[6/8] Removing Java build artifacts..."
    rm -rf query-api/target
    rm -rf event-generator/target
else
    echo "[6/8] Skipping Java build artifacts (keeping target/ directories)"
fi

# Remove Spark output
echo "[7/8] Removing Spark ML pipeline output..."
rm -rf spark-ml-pipeline/output
rm -rf spark-ml-pipeline/venv
rm -rf spark-ml-pipeline/__pycache__
rm -rf spark-ml-pipeline/src/__pycache__

# Remove logs and temporary files
echo "[8/8] Removing logs and temporary files..."
rm -rf logs/
rm -f ./*.log

echo "✅ Nuclear cleanup complete!"
echo ""
if [ "$SKIP_BUILDS" = true ]; then
    echo "To start fresh (builds preserved):"
    echo "  1. Verify .env is configured"
    echo "  2. docker-compose up -d"
    echo "  3. ./scripts/start-stream-processor.sh"
    echo "  4. ./scripts/start-event-generator.sh"
    echo "  5. ./scripts/start-query-api.sh"
else
    echo "To start fresh (full rebuild needed):"
    echo "  1. cp .env.example .env"
    echo "  2. docker-compose up -d"
    echo "  3. Build C++ processor: cd stream-processor && cmake -B build && cmake --build build"
    echo "  4. Build Java apps: mvn clean package -DskipTests"
    echo "  5. Start services with ./scripts/start-*.sh"
fi
