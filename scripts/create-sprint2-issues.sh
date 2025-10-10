#!/bin/bash

# Simple script to create Sprint 2 GitHub issues for StreamGuard
# No labels, just title, body, and milestone
# Run: chmod +x create-sprint2-issues-simple.sh && ./create-sprint2-issues-simple.sh

set -e

echo "=================================================="
echo "Creating Sprint 2 Issues for StreamGuard"
echo "=================================================="
echo ""

MILESTONE="Sprint 2"

# Function to create issue
create_issue() {
    local number=$1
    local title=$2
    local body=$3

    echo "Creating US-${number}: ${title}"

    gh issue create \
        --title "US-${number}: ${title}" \
        --body "${body}" \
        --milestone "${MILESTONE}"

    echo "✓ Created US-${number}"
    echo ""
}

#==============================================================================

create_issue "301" "Prometheus Metrics" \
"**As a** DevOps engineer
**I want** Prometheus metrics exported from C++ processor
**So that** I can monitor system health and performance

## Acceptance Criteria
- [ ] Prometheus client library integrated in C++
- [ ] Metrics endpoint exposed (e.g., :8080/metrics)
- [ ] Counter metrics: events_processed_total, events_stored_total
- [ ] Gauge metrics: events_per_second, rocksdb_size_bytes
- [ ] Histogram metrics: processing_latency_ms, storage_latency_ms
- [ ] Metrics updated in real-time

**Estimate:** 4 hours
**Priority:** CRITICAL ⭐⭐⭐"

create_issue "302" "Grafana Dashboards" \
"**As a** operations team member
**I want** pre-configured Grafana dashboards
**So that** I can visualize system performance at a glance

## Acceptance Criteria
- [ ] Grafana dashboard JSON files in repo
- [ ] Dashboard 1: Pipeline Overview
- [ ] Dashboard 2: Threat Detection
- [ ] Dashboard 3: Storage Metrics
- [ ] Auto-refresh every 5 seconds

**Estimate:** 3 hours
**Priority:** CRITICAL ⭐⭐⭐"

create_issue "210" "LLM Threat Analysis Integration" \
"**As a** security analyst
**I want** AI-generated threat analysis for suspicious events
**So that** I can quickly understand threats without manual investigation

## Acceptance Criteria
- [ ] OpenAI API integration in C++ (using libcurl)
- [ ] GPT-4o-mini used for analysis
- [ ] Analysis triggered for events with threat_score > 0.7
- [ ] Generated analysis includes threat description, recommended actions, severity
- [ ] Results cached to avoid duplicate API calls
- [ ] Rate limiting (max 100 requests/hour)

**Estimate:** 6 hours
**Priority:** CRITICAL ⭐⭐⭐⭐⭐"

create_issue "211" "Vector Embeddings for Events" \
"**As a** security engineer
**I want** vector embeddings for events
**So that** I can find similar threats using semantic search

## Acceptance Criteria
- [ ] OpenAI embeddings API integrated
- [ ] text-embedding-3-small model used
- [ ] Embeddings stored in RocksDB
- [ ] Similarity search function implemented
- [ ] Can find top-K similar events (K=5 default)

**Estimate:** 4 hours
**Priority:** CRITICAL ⭐⭐⭐⭐"

create_issue "214" "AI Analysis Storage & Retrieval" \
"**As a** developer
**I want** efficient storage for AI analysis results
**So that** I can query and retrieve analysis quickly

## Acceptance Criteria
- [ ] New RocksDB column family \"ai_analysis\"
- [ ] Store AI analysis keyed by event_id
- [ ] Query analysis by event_id, severity, time range
- [ ] Retrieval latency < 1ms (P95)

**Estimate:** 3 hours
**Priority:** CRITICAL ⭐⭐⭐"

create_issue "212" "RAG Threat Intelligence System" \
"**As a** security analyst
**I want** AI to query threat intelligence database
**So that** I get context-aware threat analysis

## Acceptance Criteria
- [ ] ChromaDB vector database running in Docker
- [ ] Python microservice (Flask) for RAG queries
- [ ] Threat intelligence corpus seeded (100+ indicators)
- [ ] RAG endpoint: POST /rag/query with event context
- [ ] Response time < 500ms (P95)

**Estimate:** 6 hours
**Priority:** CRITICAL ⭐⭐⭐⭐⭐"

create_issue "206" "Query API Foundation" \
"**As a** API user
**I want** REST API to query processed events
**So that** I can build applications on StreamGuard

## Acceptance Criteria
- [ ] Spring Boot 3 application running
- [ ] RocksDB Java bindings connected to C++ database
- [ ] Health check endpoint: GET /actuator/health
- [ ] OpenAPI/Swagger UI accessible
- [ ] Response time < 100ms (P95)

**Estimate:** 3 hours
**Priority:** CRITICAL ⭐⭐⭐"

create_issue "207" "Key Query Endpoints" \
"**As a** threat hunter
**I want** specific query endpoints for investigations
**So that** I can quickly find and analyze threats

## Acceptance Criteria
- [ ] GET /api/events/recent?limit=N
- [ ] GET /api/events/threats?min_score=X
- [ ] GET /api/events/{id}
- [ ] GET /api/analysis/{id}
- [ ] GET /api/stats/summary
- [ ] All endpoints return JSON with pagination

**Estimate:** 3 hours
**Priority:** CRITICAL ⭐⭐⭐"

create_issue "213" "AI-Powered Query Interface" \
"**As a** security analyst
**I want** to ask questions in natural language
**So that** I don't need to write complex queries

## Acceptance Criteria
- [ ] POST /api/threats/ask endpoint
- [ ] Accepts natural language queries
- [ ] LLM translates query to API calls
- [ ] Returns results with AI summary
- [ ] Example queries work (failed logins, suspicious events, etc.)

**Estimate:** 4 hours
**Priority:** CRITICAL ⭐⭐⭐⭐"

create_issue "215" "Statistical Anomaly Detection" \
"**As a** security analyst
**I want** statistical anomaly detection
**So that** I can catch unusual behavior patterns

## Acceptance Criteria
- [ ] Baseline tracking for each user
- [ ] Detect unusual login times, new locations, spike in failures
- [ ] Anomaly score added to threat_score (0.0-1.0)
- [ ] Baselines learned from first 1000 events per user

**Estimate:** 3 hours
**Priority:** NICE-TO-HAVE ⭐⭐"

create_issue "202" "Basic Event Filtering" \
"**As a** API user
**I want** to filter events by various criteria
**So that** I can narrow down investigations

## Acceptance Criteria
- [ ] Filter by event_type, threat_score, time range, source IP, user
- [ ] Multiple filters can be combined (AND logic)

**Estimate:** 2 hours
**Priority:** NICE-TO-HAVE ⭐"

create_issue "203" "Simple Aggregations" \
"**As a** security analyst
**I want** basic aggregations over events
**So that** I can identify trends

## Acceptance Criteria
- [ ] GET /api/stats/by-type - Count by event type
- [ ] GET /api/stats/top-sources?limit=N - Top N source IPs
- [ ] GET /api/stats/avg-threat-score
- [ ] GET /api/stats/timeline?interval=1h

**Estimate:** 2 hours
**Priority:** NICE-TO-HAVE ⭐"

create_issue "305" "AWS Deployment" \
"**As a** hiring manager
**I want** to see StreamGuard running in the cloud
**So that** I can verify it works in production environment

## Acceptance Criteria
- [ ] Application deployed to AWS (ECS or EC2)
- [ ] Infrastructure as code (Terraform or CloudFormation)
- [ ] Public URL accessible for demo
- [ ] All components running
- [ ] Costs < \$5/day

**Estimate:** 6 hours
**Priority:** BONUS POINTS ⭐⭐⭐"

echo ""
echo "=================================================="
echo "✓ All 13 Sprint 2 issues created successfully!"
echo "=================================================="
echo ""
echo "View issues: gh issue list --milestone 'Sprint 2'"
echo ""
echo "Sprint 2 Summary:"
echo "  - 13 issues created"
echo "  - 9 CRITICAL (must complete)"
echo "  - 4 NICE-TO-HAVE (if time allows)"
echo "  - Total: ~49 hours (6-7 days)"
echo ""
echo "Start with US-301 (Prometheus Metrics)"
echo "Good luck! 🚀"