#!/bin/bash
# Create all 22 StreamGuard issues
set -e

echo "🚀 Creating all StreamGuard GitHub Issues..."
echo ""

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI not found!"
    exit 1
fi

# Create labels first
echo "🏷️  Creating labels..."
gh label create "sprint-1" --color "0E8A16" --description "Sprint 1 issues" 2>/dev/null || true
gh label create "sprint-2" --color "1D76DB" --description "Sprint 2 issues" 2>/dev/null || true
gh label create "sprint-3" --color "5319E7" --description "Sprint 3 issues" 2>/dev/null || true
gh label create "ai-ml" --color "8B5CF6" --description "AI/ML features" 2>/dev/null || true
gh label create "stream-processor" --color "3B82F6" --description "C++ stream processor" 2>/dev/null || true
gh label create "event-generator" --color "10B981" --description "Java event generator" 2>/dev/null || true
gh label create "query-api" --color "F59E0B" --description "REST API" 2>/dev/null || true
gh label create "monitoring" --color "EC4899" --description "Prometheus/Grafana" 2>/dev/null || true
gh label create "infrastructure" --color "6B7280" --description "Infrastructure setup" 2>/dev/null || true
gh label create "database" --color "EF4444" --description "RocksDB" 2>/dev/null || true
gh label create "testing" --color "059669" --description "Testing" 2>/dev/null || true
gh label create "documentation" --color "D97706" --description "Documentation" 2>/dev/null || true
gh label create "high-priority" --color "B91C1C" --description "High priority" 2>/dev/null || true
gh label create "optional" --color "9CA3AF" --description "Optional enhancement" 2>/dev/null || true
gh label create "demo" --color "BE185D" --description "Demo materials" 2>/dev/null || true
gh label create "data-model" --color "FBCA04" --description "Data modeling" 2>/dev/null || true
gh label create "integration-test" --color "0075CA" --description "Integration testing" 2>/dev/null || true
gh label create "performance" --color "D93F0B" --description "Performance optimization" 2>/dev/null || true
gh label create "reliability" --color "C5DEF5" --description "Reliability & resilience" 2>/dev/null || true
gh label create "deployment" --color "BFD4F2" --description "Deployment" 2>/dev/null || true
gh label create "ml-pipeline" --color "7057FF" --description "ML pipeline" 2>/dev/null || true
gh label create "polish" --color "E99695" --description "Final polish" 2>/dev/null || true
echo "✅ Labels created"
echo ""

echo "📋 Creating Sprint 1 issues..."

# gh issue create --title "US-101: Development Environment Setup" --body "**Sprint**: 1 | **Estimate**: 4h | **Status**: ✅ COMPLETE

# **Goal**: Set up complete development environment for Mac M1

# **Acceptance Criteria**:
# - [x] All dependencies installed (Java 21, CMake, RocksDB, librdkafka)
# - [x] Docker infrastructure running (Kafka, Prometheus, Grafana)
# - [x] Project structure created
# - [x] Git repository initialized
# - [x] All verification checks pass

# **Completed**: ✅" --label "sprint-1,infrastructure"

gh issue create --title "US-102: Event Data Model" --body "**Sprint**: 1 | **Estimate**: 3h

**Goal**: Define event schema and create data models

**Acceptance Criteria**:
- [ ] Event schema defined (JSON)
- [ ] Java POJO classes created
- [ ] C++ struct definitions created
- [ ] Serialization/deserialization utilities
- [ ] Sample event fixtures for testing" --label "sprint-1,data-model"

gh issue create --title "US-103: Event Generator Implementation" --body "**Sprint**: 1 | **Estimate**: 5h

**Goal**: Build Java application to generate realistic security events

**Acceptance Criteria**:
- [ ] Java application producing events to Kafka
- [ ] Configurable event rate (events/second)
- [ ] Multiple event types (auth, network, file, process, DNS)
- [ ] Realistic data distributions
- [ ] Command-line arguments for configuration
- [ ] Graceful shutdown handling" --label "sprint-1,event-generator"

gh issue create --title "US-104: Basic C++ Kafka Consumer" --body "**Sprint**: 1 | **Estimate**: 6h

**Goal**: Create C++ consumer to read events from Kafka

**Acceptance Criteria**:
- [ ] C++ application consuming from Kafka
- [ ] Proper consumer group configuration
- [ ] JSON deserialization
- [ ] Offset management
- [ ] Graceful shutdown with commit
- [ ] Basic error handling" --label "sprint-1,stream-processor"

gh issue create --title "US-105: RocksDB Integration" --body "**Sprint**: 1 | **Estimate**: 5h

**Goal**: Integrate RocksDB for state storage

**Acceptance Criteria**:
- [ ] RocksDB initialized in C++ processor
- [ ] Events written to RocksDB with timestamp key
- [ ] Basic get/put operations working
- [ ] Proper resource cleanup (RAII)
- [ ] Unit tests for DB operations" --label "sprint-1,stream-processor,database"

gh issue create --title "US-106: End-to-End Pipeline Test" --body "**Sprint**: 1 | **Estimate**: 4h

**Goal**: Verify complete data flow

**Acceptance Criteria**:
- [ ] Generator produces events to Kafka
- [ ] C++ processor consumes and stores in RocksDB
- [ ] Can verify events in RocksDB
- [ ] All components run via Docker Compose
- [ ] Basic throughput metrics logged" --label "sprint-1,testing"

echo "✅ Sprint 1 issues created (6 issues)"
echo ""

echo "📋 Creating Sprint 2 issues..."

gh issue create --title "US-201: Multi-threaded Stream Processing" --body "**Sprint**: 2 | **Estimate**: 6h

**Goal**: Implement parallel event processing

**Acceptance Criteria**:
- [ ] One consumer thread per Kafka partition
- [ ] Thread-safe access to shared resources
- [ ] Proper thread lifecycle management
- [ ] Throughput > 10K events/sec
- [ ] No data races (verified with thread sanitizer)" --label "sprint-2,stream-processor"

gh issue create --title "US-202: Event Filtering" --body "**Sprint**: 2 | **Estimate**: 4h

**Goal**: Implement event filtering capabilities

**Acceptance Criteria**:
- [ ] Filter by event type
- [ ] Filter by threat score threshold
- [ ] Filter by IP address/CIDR range
- [ ] Filter by time range
- [ ] Composable filters (AND/OR logic)" --label "sprint-2,stream-processor"

gh issue create --title "US-203: Aggregations" --body "**Sprint**: 2 | **Estimate**: 5h

**Goal**: Implement aggregation operations

**Acceptance Criteria**:
- [ ] Count events per IP address
- [ ] Calculate average threat score per user
- [ ] Top N sources by event count
- [ ] Time-based aggregations (hourly, daily)
- [ ] Incremental updates (no full recomputation)" --label "sprint-2,stream-processor"

gh issue create --title "US-204: Sliding Windows" --body "**Sprint**: 2 | **Estimate**: 5h

**Goal**: Implement time-based sliding windows

**Acceptance Criteria**:
- [ ] Maintain last 5 minutes of events
- [ ] Maintain last 1 hour of events
- [ ] Maintain last 24 hours of events
- [ ] Old data automatically expires
- [ ] Fast lookups within windows" --label "sprint-2,stream-processor"

gh issue create --title "US-205: Pattern Detection" --body "**Sprint**: 2 | **Estimate**: 6h

**Goal**: Detect suspicious patterns using joins

**Acceptance Criteria**:
- [ ] Detect: Multiple failed auth → successful auth from same IP
- [ ] Detect: Unusual access patterns
- [ ] Detect: Rapid-fire events (potential DDoS)
- [ ] Patterns stored with timestamps
- [ ] Low false positive rate" --label "sprint-2,stream-processor,ai-ml"

gh issue create --title "US-206: Query API Foundation" --body "**Sprint**: 2 | **Estimate**: 5h

**Goal**: Build REST API for querying events

**Acceptance Criteria**:
- [ ] Spring Boot application running
- [ ] Health check endpoint
- [ ] OpenAPI/Swagger documentation
- [ ] Basic CRUD endpoints for events
- [ ] Pagination support
- [ ] CORS configuration" --label "sprint-2,query-api"

gh issue create --title "US-208: Anomaly Detection (Statistical)" --body "**Sprint**: 2 | **Estimate**: 5h | **Priority**: HIGH 🔥

**Goal**: Implement AI-powered anomaly detection

**Acceptance Criteria**:
- [ ] Track baseline behavior per user
- [ ] Calculate anomaly score (0-1) for each event
- [ ] Store anomalies in separate RocksDB column family
- [ ] Flag high-score anomalies (>0.7)
- [ ] Expose anomaly metrics to Prometheus
- [ ] Documentation of scoring algorithm" --label "sprint-2,stream-processor,ai-ml,high-priority"

gh issue create --title "US-209: ML Training Data Export Pipeline" --body "**Sprint**: 2 | **Estimate**: 3h

**Goal**: Export data for ML model training

**Acceptance Criteria**:
- [ ] Events exported in Parquet format
- [ ] Features extracted and normalized
- [ ] Labels included (is_anomaly, attack_type)
- [ ] Batch export to S3 or local filesystem
- [ ] User data anonymized
- [ ] Schema documented" --label "sprint-2,ai-ml"

echo "✅ Sprint 2 issues created (8 issues)"
echo ""

echo "📋 Creating Sprint 3 issues..."

gh issue create --title "US-301: Prometheus Metrics" --body "**Sprint**: 3 | **Estimate**: 4h

**Goal**: Implement comprehensive metrics collection

**Acceptance Criteria**:
- [ ] Prometheus metrics endpoint (/metrics)
- [ ] Throughput metrics (events/sec)
- [ ] Latency histograms (P50, P95, P99)
- [ ] RocksDB operation metrics
- [ ] Consumer lag metrics
- [ ] Anomaly detection metrics" --label "sprint-3,monitoring"

gh issue create --title "US-302: Grafana Dashboards" --body "**Sprint**: 3 | **Estimate**: 5h

**Goal**: Create visualization dashboards

**Acceptance Criteria**:
- [ ] System overview dashboard
- [ ] Processor performance dashboard
- [ ] API performance dashboard
- [ ] AI/ML metrics dashboard
- [ ] Anomaly detection dashboard
- [ ] Pre-configured alerts
- [ ] Shareable dashboard JSON" --label "sprint-3,monitoring"

gh issue create --title "US-303: Load Testing" --body "**Sprint**: 3 | **Estimate**: 5h

**Goal**: Stress test system performance

**Acceptance Criteria**:
- [ ] Sustained 10K events/sec for 10 minutes
- [ ] P99 latency < 5ms under load
- [ ] No memory leaks detected
- [ ] Graceful degradation under overload
- [ ] Performance report documented" --label "sprint-3,testing"

gh issue create --title "US-304: Error Handling & Resilience" --body "**Sprint**: 3 | **Estimate**: 4h

**Goal**: Implement robust error handling

**Acceptance Criteria**:
- [ ] Kafka connection retry logic
- [ ] RocksDB write failure handling
- [ ] Circuit breakers for API calls
- [ ] Dead letter queue for unparseable events
- [ ] Graceful shutdown on all signals
- [ ] All errors logged with context" --label "sprint-3,stream-processor,query-api"

gh issue create --title "US-306: Documentation" --body "**Sprint**: 3 | **Estimate**: 5h

**Goal**: Create comprehensive documentation

**Acceptance Criteria**:
- [ ] README with project overview
- [ ] Architecture diagrams
- [ ] API documentation (OpenAPI)
- [ ] AI/ML components documentation
- [ ] Deployment guide
- [ ] Troubleshooting guide" --label "sprint-3,documentation"

gh issue create --title "US-307: Demo Video" --body "**Sprint**: 3 | **Estimate**: 5h

**Goal**: Create polished demo video

**Acceptance Criteria**:
- [ ] 6-8 minute video
- [ ] Architecture overview (1 min)
- [ ] Live demo with AI features (4 min)
- [ ] Code walkthrough (2 min)
- [ ] Professional quality
- [ ] Uploaded to YouTube" --label "sprint-3,demo"

gh issue create --title "US-308: Blog Post" --body "**Sprint**: 3 | **Estimate**: 5h

**Goal**: Write technical blog post

**Acceptance Criteria**:
- [ ] 1500-2000 word blog post
- [ ] Published on Medium or personal blog
- [ ] Covers architecture decisions
- [ ] Discusses trade-offs
- [ ] Includes code snippets
- [ ] Links to GitHub repo" --label "sprint-3,documentation,demo"

gh issue create --title "US-309: Final Polish" --body "**Sprint**: 3 | **Estimate**: 3h

**Goal**: Polish project for presentation

**Acceptance Criteria**:
- [ ] All tests passing
- [ ] No compiler warnings
- [ ] Code formatted consistently
- [ ] Git history cleaned up
- [ ] GitHub repo looks professional
- [ ] Demo ready to show" --label "sprint-3"

echo "✅ Sprint 3 issues created (8 issues)"
echo ""

echo "📋 Creating optional/future issues..."

gh issue create --title "US-207: Additional Query Endpoints" --body "**Sprint**: Future | **Estimate**: 3h | **Optional**

**Goal**: Add advanced query endpoints

**Acceptance Criteria**:
- [ ] GET /events/aggregate/top-sources
- [ ] GET /events/patterns
- [ ] GET /events/{id}
- [ ] All endpoints return < 100ms (P95)" --label "query-api,optional"

gh issue create --title "US-311: ML Model Integration (ONNX)" --body "**Sprint**: Future | **Estimate**: 4h | **Optional**

**Goal**: Integrate trained ML model

**Acceptance Criteria**:
- [ ] Isolation Forest model trained
- [ ] Model exported to ONNX format
- [ ] ONNX Runtime integrated in C++
- [ ] Model inference in real-time (<1ms)
- [ ] Comparison with statistical baseline
- [ ] Documentation of trade-offs" --label "ai-ml,optional"

gh issue create --title "US-310: LLM-Powered Threat Narratives" --body "**Sprint**: 3 | **Estimate**: 5h | **Priority**: HIGH 🔥

**Goal**: Generate AI threat summaries using LLM

**Acceptance Criteria**:
- [ ] REST endpoint: GET /threats/{id}/narrative
- [ ] LLM integration (OpenAI GPT-4 or Anthropic Claude)
- [ ] Generates human-readable summary
- [ ] Provides recommended actions
- [ ] Explains deviation from normal behavior
- [ ] Response caching to minimize API costs
- [ ] Fallback for API failures
- [ ] Response time < 3 seconds" --label "sprint-3,query-api,ai-ml,high-priority"

gh issue create --title "US-305: Cloud Deployment (AWS)" --body "**Sprint**: Future | **Estimate**: 6h | **Optional**

**Goal**: Deploy to AWS

**Acceptance Criteria**:
- [ ] All components deployed to AWS
- [ ] Public endpoint for Query API
- [ ] Grafana dashboard publicly accessible
- [ ] Infrastructure as Code (Terraform)
- [ ] Deployment documentation
- [ ] Cost under $15/month" --label "infrastructure,optional"

echo "✅ Optional issues created (4 issues)"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All 22 issues created successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Summary:"
echo "  • Sprint 1: 6 issues"
echo "  • Sprint 2: 8 issues"
echo "  • Sprint 3: 8 issues (including US-310)"
echo "  • Future: 4 optional issues"
echo ""
echo "Next steps:"
echo "1. View issues: gh issue list"
echo "2. Go to: https://github.com/$(gh repo view --json owner,name -q '.owner.login + \"/\" + .name')/issues"
echo "3. Manually assign issues to milestones in the web UI"
echo "4. Set up GitHub Project board"
echo "5. Close US-101 as completed"
echo ""
echo "Ready to start US-102! 🚀"
