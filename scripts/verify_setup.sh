#!/bin/bash
# StreamGuard Setup Verification Script for Mac M1

echo "==================================="
echo "StreamGuard Setup Verification"
echo "==================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "  ${GREEN}✓${NC} $2: OK"
        return 0
    else
        echo -e "  ${RED}✗${NC} $2: MISSING"
        return 1
    fi
}

check_file() {
    if [ -f "$1" ] || [ -d "$1" ]; then
        echo -e "  ${GREEN}✓${NC} $2: OK"
        return 0
    else
        echo -e "  ${RED}✗${NC} $2: MISSING"
        return 1
    fi
}

# Check Java
echo "Checking Java..."
java -version 2>&1 | grep -q "version \"17"
if [ $? -eq 0 ]; then
    echo -e "  ${GREEN}✓${NC} Java 17: OK"
else
    echo -e "  ${RED}✗${NC} Java 17: MISSING or wrong version"
fi
echo ""

# Check C++ tools
echo "Checking C++ tools..."
check_command "clang++" "clang++"
check_command "cmake" "cmake"
echo ""

# Check libraries
echo "Checking libraries..."
pkg-config --exists rocksdb 2>/dev/null && echo -e "  ${GREEN}✓${NC} RocksDB: OK" || echo -e "  ${RED}✗${NC} RocksDB: MISSING"
pkg-config --exists rdkafka 2>/dev/null && echo -e "  ${GREEN}✓${NC} librdkafka: OK" || echo -e "  ${RED}✗${NC} librdkafka: MISSING"
check_file "/opt/homebrew/include/nlohmann/json.hpp" "nlohmann-json"
check_file "/opt/homebrew/include/gtest/gtest.h" "Google Test"
echo ""

# Check Docker
echo "Checking Docker..."
check_command "docker" "Docker"
check_command "docker-compose" "Docker Compose"
echo ""

# Check Python (optional)
echo "Checking Python (optional for ML)..."
check_command "python3" "Python 3"
echo ""

# Check Homebrew
echo "Checking Homebrew..."
check_command "brew" "Homebrew"
echo ""

echo "==================================="
echo "Verification Complete"
echo "==================================="
echo ""
echo "Next steps:"
echo "1. Install any missing dependencies"
echo "2. Run ./scripts/init-project.sh to create project structure"
echo "3. Run docker-compose up -d to start infrastructure"
