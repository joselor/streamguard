#!/bin/bash

# Script to create GitHub labels for StreamGuard project
# Run: chmod +x create-labels.sh && ./create-labels.sh

set -e

echo "=================================================="
echo "Creating GitHub Labels for StreamGuard"
echo "=================================================="
echo ""

# Function to create label if it doesn't exist
create_label() {
    local name=$1
    local description=$2
    local color=$3

    if gh label list | grep -q "^${name}"; then
        echo "  ✓ Label '${name}' already exists"
    else
        gh label create "${name}" --description "${description}" --color "${color}" 2>/dev/null
        echo "  ✓ Created label '${name}'"
    fi
}

echo "Creating Priority Labels..."
create_label "demo-critical" "Must complete for demo" "b60205"
create_label "demo-nice" "Enhances demo but not critical" "fbca04"
create_label "bonus-points" "Extra credit features" "0e8a16"

echo ""
echo "Creating Technology Labels..."
create_label "cpp" "C++ codebase" "1d76db"
create_label "java" "Java codebase" "5319e7"
create_label "python" "Python codebase" "3572a5"
create_label "kafka" "Apache Kafka" "000000"
create_label "storage" "RocksDB storage" "c5def5"
create_label "ai" "AI/ML features" "d4c5f9"
create_label "monitoring" "Observability/metrics" "bfd4f2"
create_label "query-api" "REST API" "0075ca"
create_label "infrastructure" "DevOps/deployment" "e99695"
create_label "testing" "Tests" "d93f0b"

echo ""
echo "Creating Sprint Labels..."
create_label "sprint-1" "Foundation sprint" "0e8a16"
create_label "sprint-2" "Tech + AI sprint" "5ebeff"
create_label "sprint-3" "Demo prep sprint" "1d76db"

echo ""
echo "Creating Special Labels..."
create_label "job-requirement" "From CrowdStrike job description" "d73a4a"
create_label "security" "Security feature" "f9d0c4"
create_label "aws" "AWS cloud" "ff7f00"
create_label "model" "Data models/schemas" "c2e0c6"
create_label "documentation" "Documentation updates" "0075ca"
create_label "performance" "Performance optimization" "fbca04"

echo ""
echo "=================================================="
echo "✓ All labels created successfully!"
echo "=================================================="
echo ""
echo "View labels: gh label list"
echo "Next step: Run ./create-sprint2-issues.sh"