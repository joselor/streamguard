#!/bin/bash
# StreamGuard Local Environment Startup Script

set -e

echo "🚀 Starting StreamGuard local environment..."
echo ""

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ docker-compose.yml not found!"
    echo "   Please run this script from the project root directory."
    exit 1
fi

# Start Docker services
echo "Starting Docker infrastructure..."
echo "  - Zookeeper"
echo "  - Kafka"
echo "  - Prometheus"
echo "  - Grafana"
echo "  - Kafka UI"
echo ""
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 15

# Check if Kafka is ready
echo "Checking Kafka status..."
max_attempts=10
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker exec streamguard-kafka kafka-topics --bootstrap-server localhost:9092 --list &> /dev/null; then
        echo "✓ Kafka is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Waiting for Kafka... (attempt $attempt/$max_attempts)"
    sleep 3
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Kafka failed to start properly"
    echo "   Check logs: docker-compose logs kafka"
    exit 1
fi

echo ""
echo "Creating Kafka topics..."

# Create security-events topic
docker exec streamguard-kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --create --if-not-exists \
    --topic security-events \
    --partitions 4 \
    --replication-factor 1 \
    --config retention.ms=86400000
echo "✓ Created topic: security-events (4 partitions)"

# Create anomalies topic
docker exec streamguard-kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --create --if-not-exists \
    --topic anomalies \
    --partitions 2 \
    --replication-factor 1 \
    --config retention.ms=604800000
echo "✓ Created topic: anomalies (2 partitions)"

# Create patterns topic
docker exec streamguard-kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --create --if-not-exists \
    --topic patterns \
    --partitions 2 \
    --replication-factor 1 \
    --config retention.ms=604800000
echo "✓ Created topic: patterns (2 partitions)"

echo ""
echo "✅ StreamGuard infrastructure is ready!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Access Points:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🔍 Prometheus:  http://localhost:9090"
echo "  📈 Grafana:     http://localhost:3000 (admin/admin)"
echo "  📨 Kafka:       localhost:9092"
echo "  🎛️  Kafka UI:    http://localhost:8090"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📝 Topics Created:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker exec streamguard-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -E "security-events|anomalies|patterns" | sed 's/^/  ✓ /'
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 Next Steps:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  1. Build stream-processor:"
echo "     cd stream-processor"
echo "     mkdir build && cd build"
echo "     cmake .."
echo "     make"
echo ""
echo "  2. Build event-generator:"
echo "     cd event-generator"
echo "     mvn clean package"
echo ""
echo "  3. Build query-api:"
echo "     cd query-api"
echo "     mvn clean package"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🛑 To stop all services:"
echo "   docker-compose down"
echo ""
echo "🔄 To view logs:"
echo "   docker-compose logs -f [service-name]"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
