#!/bin/bash
# StreamGuard Dependency Installation Script for Mac M1

set -e  # Exit on error

echo "======================================"
echo "StreamGuard Dependency Installation"
echo "======================================"
echo ""

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
    echo "❌ Homebrew not found. Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    echo ""
    echo "⚠️  Please run this script again after Homebrew installation completes."
    exit 1
fi

echo "✓ Homebrew found"
echo ""

# Update Homebrew
echo "Updating Homebrew..."
brew update
echo ""

# Install Java 17
echo "Installing Java 17..."
if ! brew list openjdk@17 &> /dev/null; then
    brew install openjdk@17
    echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
    echo 'export CPPFLAGS="-I/opt/homebrew/opt/openjdk@17/include"' >> ~/.zshrc
    echo "✓ Java 17 installed (please restart terminal or run: source ~/.zshrc)"
else
    echo "✓ Java 17 already installed"
fi
echo ""

# Install CMake
echo "Installing CMake..."
if ! brew list cmake &> /dev/null; then
    brew install cmake
    echo "✓ CMake installed"
else
    echo "✓ CMake already installed"
fi
echo ""

# Install Python 3
echo "Installing Python 3..."
if ! brew list python@3.11 &> /dev/null; then
    brew install python@3.11
    pip3 install --upgrade pip
    echo "✓ Python 3.11 installed"
else
    echo "✓ Python 3.11 already installed"
fi
echo ""

# Install Apache Kafka
echo "Installing Apache Kafka..."
if ! brew list kafka &> /dev/null; then
    brew install kafka
    echo "✓ Kafka installed"
else
    echo "✓ Kafka already installed"
fi
echo ""

# Install librdkafka (Kafka C++ client)
echo "Installing librdkafka..."
if ! brew list librdkafka &> /dev/null; then
    brew install librdkafka
    echo "✓ librdkafka installed"
else
    echo "✓ librdkafka already installed"
fi
echo ""

# Install RocksDB
echo "Installing RocksDB..."
if ! brew list rocksdb &> /dev/null; then
    brew install rocksdb
    echo "✓ RocksDB installed"
else
    echo "✓ RocksDB already installed"
fi
echo ""

# Install nlohmann-json
echo "Installing nlohmann-json..."
if ! brew list nlohmann-json &> /dev/null; then
    brew install nlohmann-json
    echo "✓ nlohmann-json installed"
else
    echo "✓ nlohmann-json already installed"
fi
echo ""

# Install Google Test
echo "Installing Google Test..."
if ! brew list googletest &> /dev/null; then
    brew install googletest
    echo "✓ Google Test installed"
else
    echo "✓ Google Test already installed"
fi
echo ""

# Install wget and curl (if not present)
echo "Installing wget and curl..."
brew list wget &> /dev/null || brew install wget
brew list curl &> /dev/null || brew install curl
echo "✓ wget and curl installed"
echo ""

# Install prometheus-cpp (for metrics)
echo "Installing prometheus-cpp..."
if ! brew list prometheus-cpp &> /dev/null; then
    brew install prometheus-cpp
    echo "✓ prometheus-cpp installed"
else
    echo "✓ prometheus-cpp already installed"
fi
echo ""

# Install Maven (for Java builds)
echo "Installing Maven..."
if ! brew list maven &> /dev/null; then
    brew install maven
    echo "✓ Maven installed"
else
    echo "✓ Maven already installed"
fi
echo ""

# Add library paths to shell config if not already present
echo "Configuring library paths..."
if ! grep -q "DYLD_LIBRARY_PATH" ~/.zshrc; then
    echo 'export DYLD_LIBRARY_PATH=/opt/homebrew/lib:$DYLD_LIBRARY_PATH' >> ~/.zshrc
    echo "✓ Library paths added to ~/.zshrc"
else
    echo "✓ Library paths already configured"
fi
echo ""

# Install Docker Desktop check
echo "Checking Docker Desktop..."
if command -v docker &> /dev/null; then
    echo "✓ Docker found"
else
    echo "⚠️  Docker Desktop not found."
    echo "   Please install Docker Desktop from: https://www.docker.com/products/docker-desktop/"
    echo "   Or run: brew install --cask docker"
fi
echo ""

echo "======================================"
echo "Installation Complete!"
echo "======================================"
echo ""
echo "⚠️  IMPORTANT: Restart your terminal or run:"
echo "   source ~/.zshrc"
echo ""
echo "Next steps:"
echo "1. Restart terminal or run: source ~/.zshrc"
echo "2. Run ./scripts/verify_setup.sh to verify installation"
echo "3. Start infrastructure: docker-compose up -d"
echo "4. Build components: cd stream-processor && mkdir build && cd build && cmake .. && make"
echo "5. See docs/final/guides/QUICK_START.md for complete setup guide"
