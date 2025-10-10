#!/bin/bash

# GitHub Labels Creation Script for StreamGuard
# Creates all labels for project management and categorization

echo "🏷️  Creating GitHub Labels for StreamGuard"
echo "=========================================="
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ Error: GitHub CLI (gh) not found"
    echo "Install it: https://cli.github.com/"
    exit 1
fi

# Check if we're in a git repo
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository"
    exit 1
fi

echo "📍 Repository: $(git remote get-url origin)"
echo ""

# Priority Labels (Red/Orange/Gray)
echo "Creating Priority Labels..."

gh label create "demo-critical" \
    --description "Must complete for interview demo" \
    --color "d73a4a" \
    --force

gh label create "demo-nice" \
    --description "Include if time permits" \
    --color "fbca04" \
    --force

gh label create "future" \
    --description "Post-interview enhancements" \
    --color "e4e669" \
    --force

gh label create "obsolete" \
    --description "Marked as out-of-scope" \
    --color "d4c5f9" \
    --force

echo "✅ Priority labels created"
echo ""

# Technology Labels (Blue shades)
echo "Creating Technology Labels..."

gh label create "ai" \
    --description "AI/ML features (LLM, embeddings, RAG)" \
    --color "0e8a16" \
    --force

gh label create "monitoring" \
    --description "Prometheus/Grafana monitoring" \
    --color "1d76db" \
    --force

gh label create "query-api" \
    --description "REST API features" \
    --color "0075ca" \
    --force

gh label create "storage" \
    --description "RocksDB storage features" \
    --color "5319e7" \
    --force

gh label create "security" \
    --description "Security-specific features" \
    --color "b60205" \
    --force

gh label create "kafka" \
    --description "Apache Kafka streaming" \
    --color "006b75" \
    --force

gh label create "c++" \
    --description "C++ stream processor" \
    --color "f9d0c4" \
    --force

gh label create "java" \
    --description "Java components" \
    --color "b07219" \
    --force

gh label create "infrastructure" \
    --description "Docker, AWS, deployment" \
    --color "bfd4f2" \
    --force

echo "✅ Technology labels created"
echo ""

# Sprint Labels (Purple shades)
echo "Creating Sprint Labels..."

gh label create "sprint-1" \
    --description "Sprint 1: Foundation (Complete)" \
    --color "8b4789" \
    --force

gh label create "sprint-2" \
    --description "Sprint 2: Tech Stack + AI" \
    --color "c5def5" \
    --force

gh label create "sprint-3" \
    --description "Sprint 3: Demo preparation" \
    --color "bfdadc" \
    --force

echo "✅ Sprint labels created"
echo ""

# Type Labels (Standard GitHub colors)
echo "Creating Type Labels..."

gh label create "enhancement" \
    --description "New feature or request" \
    --color "a2eeef" \
    --force

gh label create "bug" \
    --description "Something isn't working" \
    --color "d73a4a" \
    --force

gh label create "documentation" \
    --description "Improvements or additions to documentation" \
    --color "0075ca" \
    --force

gh label create "testing" \
    --description "Unit tests, integration tests" \
    --color "d4c5f9" \
    --force

echo "✅ Type labels created"
echo ""

# Job Requirements Labels (Green shades)
echo "Creating Job Requirement Labels..."

gh label create "job-requirement" \
    --description "Directly maps to job description requirement" \
    --color "0e8a16" \
    --force

gh label create "bonus-points" \
    --description "Bonus points from job description" \
    --color "7057ff" \
    --force

echo "✅ Job requirement labels created"
echo ""

# Special Labels
echo "Creating Special Labels..."

gh label create "blocked" \
    --description "Blocked by another issue or external factor" \
    --color "d93f0b" \
    --force

gh label create "help-wanted" \
    --description "Extra attention needed" \
    --color "008672" \
    --force

gh label create "good-first-issue" \
    --description "Good for newcomers" \
    --color "7057ff" \
    --force

echo "✅ Special labels created"
echo ""

echo "=========================================="
echo "✅ All labels created successfully!"
echo ""
echo "View labels: gh label list"
echo "Or visit: https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]\(.*\)\.git/\1/')/labels"