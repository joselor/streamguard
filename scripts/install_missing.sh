#!/bin/bash
# Install only missing dependencies for StreamGuard

set -e

echo "======================================"
echo "Installing Missing Dependencies"
echo "======================================"
echo ""

# Install CMake
echo "📦 Installing CMake..."
brew install cmake
echo "✅ CMake installed"
echo ""

# Install RocksDB
echo "📦 Installing RocksDB..."
brew install rocksdb
echo "✅ RocksDB installed"
echo ""

# Install librdkafka (Kafka C++ client)
echo "📦 Installing librdkafka..."
brew install librdkafka
echo "✅ librdkafka installed"
echo ""

# Install nlohmann-json
echo "📦 Installing nlohmann-json..."
brew install nlohmann-json
echo "✅ nlohmann-json installed"
echo ""

# Install Google Test
echo "📦 Installing Google Test..."
brew install googletest
echo "✅ Google Test installed"
echo ""

# Install Apache Kafka (for local development/testing)
echo "📦 Installing Apache Kafka..."
brew install kafka
echo "✅ Kafka installed"
echo ""

# Add library paths to shell config
echo "🔧 Configuring environment..."
if ! grep -q "DYLD_LIBRARY_PATH" ~/.zshrc 2>/dev/null; then
    echo 'export DYLD_LIBRARY_PATH=/opt/homebrew/lib:$DYLD_LIBRARY_PATH' >> ~/.zshrc
    echo "✅ Library paths added to ~/.zshrc"
else
    echo "✅ Library paths already configured"
fi
echo ""

echo "======================================"
echo "✅ Installation Complete!"
echo "======================================"
echo ""
echo "⚠️  IMPORTANT NEXT STEPS:"
echo ""
echo "1. Install Docker Desktop manually:"
echo "   https://www.docker.com/products/docker-desktop/"
echo "   Or run: brew install --cask docker"
echo ""
echo "2. Restart your terminal or run:"
echo "   source ~/.zshrc"
echo ""
echo "3. Then run: ./scripts/verify-setup.sh"
echo ""
echo "======================================"
