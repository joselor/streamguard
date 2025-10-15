#!/bin/bash
# Nuclear cleanup script - removes ALL StreamGuard data and artifacts

echo "🧹 Starting StreamGuard Nuclear Cleanup..."
echo "⚠️  WARNING: This will delete ALL data, builds, and Docker volumes!"
read -r -p "Are you sure? (yes/no): " confirm

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
echo "[5/8] Removing C++ build artifacts..."
rm -rf stream-processor/build
rm -rf stream-processor/cmake-build-debug
rm -rf stream-processor/cmake-build-release

# Remove Java build artifacts
echo "[6/8] Removing Java build artifacts..."
rm -rf query-api/target
rm -rf event-generator/target

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
echo "To start fresh:"
echo "  1. cp .env.example .env"
echo "  2. docker-compose up -d"
echo "  3. Build and start components"
