# StreamGuard Sprint Backlog & Project Roadmap

**Version:** 2.0 (AI-Focused Strategy)  
**Last Updated:** October 9, 2025  
**Project Goal:** Get CrowdStrike Interview

---

## Table of Contents

1. [Sprint Overview](#sprint-overview)
2. [Sprint 1: Foundation ✅](#sprint-1-foundation-)
3. [Sprint 2: Tech Stack + AI 🚧](#sprint-2-tech-stack--ai-)
4. [Sprint 3: Demo Preparation 📅](#sprint-3-demo-preparation-)
5. [Backlog Priorities](#backlog-priorities)
6. [Issue Labels Guide](#issue-labels-guide)
7. [Velocity & Capacity Planning](#velocity--capacity-planning)

---

## Sprint Overview

### Timeline

| Sprint | Dates | Goal | Status |
|--------|-------|------|--------|
| **Sprint 1** | Oct 8-9 | Foundation & Infrastructure | ✅ COMPLETE |
| **Sprint 2** | Oct 10-16 | Tech Stack Coverage + AI | 🚧 IN PROGRESS |
| **Sprint 3** | Oct 17-19 | Demo Preparation | 📅 PLANNED |
| **Launch** | Oct 19 | Application Submission | 🎯 TARGET |

### Sprint Goals

**Sprint 1 (Complete):** Build working event pipeline
- ✅ Event generation (Java/Kafka)
- ✅ Stream processing (C++/RocksDB)
- ✅ End-to-end testing
- ✅ Documentation

**Sprint 2 (Current):** Touch EVERY technology + Add AI
- 🚧 Monitoring (Prometheus + Grafana)
- 🚧 AI Integration (LLMs, Embeddings, RAG)
- 🚧 Query API (Spring Boot)
- 🚧 Anomaly Detection
- 🚧 AWS Deployment (optional)

**Sprint 3 (Next):** Demo Preparation
- 📅 Demo video (5 minutes)
- 📅 Blog post (1500 words)
- 📅 GitHub polish
- 📅 Application materials

---

## Sprint 1: Foundation ✅

**Status:** COMPLETE  
**Duration:** Oct 8-9, 2025 (2 days)  
**Velocity:** 6 user stories, 12 hours

### Completed User Stories

#### ✅ US-101: Development Environment Setup
- **Effort:** 2 hours
- **Outcome:** Docker Compose with Kafka, Zookeeper, Prometheus, Grafana
- **Labels:** `infrastructure`, `sprint-1`

#### ✅ US-102: Event Data Model
- **Effort:** 1.5 hours
- **Outcome:** Unified event schema, Java POJOs, C++ structs
- **Labels:** `model`, `sprint-1`

#### ✅ US-103: Event Generator (Java)
- **Effort:** 2 hours
- **Outcome:** Kafka producer, realistic event generation, CLI
- **Labels:** `java`, `kafka`, `sprint-1`

#### ✅ US-104: Basic C++ Kafka Consumer
- **Effort:** 2 hours
- **Outcome:** librdkafka++ consumer, event processing
- **Labels:** `cpp`, `kafka`, `sprint-1`

#### ✅ US-105: RocksDB Integration
- **Effort:** 2.5 hours
- **Outcome:** EventStore with time-series keys, efficient queries
- **Labels:** `cpp`, `storage`, `sprint-1`

#### ✅ US-106: End-to-End Pipeline Test
- **Effort:** 2 hours
- **Outcome:** Automated test script, full validation
- **Labels:** `testing`, `sprint-1`

### Sprint 1 Metrics

- **Stories Completed:** 6/6 (100%)
- **Tests Passing:** 20/20 (100%)
- **Pipeline Success:** 183 events generated, 97 stored
- **Documentation:** Architecture, setup, event schema complete

---

## Sprint 2: Tech Stack + AI 🚧

**Status:** READY TO START  
**Duration:** Oct 10-16, 2025 (7 days)  
**Planned Velocity:** 13 user stories, ~49 hours  
**Buffer:** 1 day for catch-up

### Sprint 2 Priorities

#### 🔴 DEMO-CRITICAL (Must Complete - 9 stories)

These are essential for the interview demo and cover job requirements:

| ID | Title | Effort | Priority | Tech |
|----|-------|--------|----------|------|
| **US-301** | Prometheus Metrics | 4h | ⭐⭐⭐ | Prometheus, C++ |
| **US-302** | Grafana Dashboards | 3h | ⭐⭐⭐ | Grafana |
| **US-210** | LLM Threat Analysis | 6h | ⭐⭐⭐⭐⭐ | OpenAI, C++ |
| **US-211** | Vector Embeddings | 4h | ⭐⭐⭐⭐ | OpenAI, C++ |
| **US-214** | AI Analysis Storage | 3h | ⭐⭐⭐ | RocksDB, C++ |
| **US-212** | RAG Threat Intelligence | 6h | ⭐⭐⭐⭐⭐ | ChromaDB, Python |
| **US-206** | Query API Foundation | 3h | ⭐⭐⭐ | Spring Boot, Java |
| **US-207** | Key Query Endpoints | 3h | ⭐⭐⭐ | Spring Boot, Java |
| **US-213** | AI Query Interface | 4h | ⭐⭐⭐⭐ | OpenAI, Java |

**Subtotal:** 36 hours (4.5 days @ 8h/day)

#### 🟡 DEMO-NICE (Complete if Time - 4 stories)

These enhance the demo but aren't critical:

| ID | Title | Effort | Priority | Tech |
|----|-------|--------|----------|------|
| **US-215** | Statistical Anomaly Detection | 3h | ⭐⭐ | C++ |
| **US-202** | Basic Event Filtering | 2h | ⭐ | Java |
| **US-203** | Simple Aggregations | 2h | ⭐ | Java |
| **US-305** | AWS Deployment | 6h | ⭐⭐⭐ | AWS, Terraform |

**Subtotal:** 13 hours (1.5 days)

**Total Sprint 2:** 49 hours (6 days with buffer)

### Sprint 2 Daily Plan

#### Day 1 (Oct 10) - Monitoring Foundation 🔧
**Goal:** Observability infrastructure
- Morning: US-301 (Prometheus metrics) - 4h
- Afternoon: US-302 (Grafana dashboards) - 3h
- Evening: Order OpenAI API credits

**Deliverables:**
- Metrics endpoint in C++ processor
- 3 Grafana dashboards (Pipeline, Threats, Storage)
- Prometheus scraping working

#### Day 2 (Oct 11) - AI Core 🤖
**Goal:** LLM integration foundation
- Morning: US-210 (LLM threat analysis) - 6h
- Afternoon: US-214 (AI storage) - 3h

**Deliverables:**
- OpenAI GPT-4o-mini integrated
- Threat analysis for high-risk events
- AI results stored in RocksDB

#### Day 3 (Oct 12) - Embeddings & Query API 📊
**Goal:** Semantic search + REST API
- Morning: US-211 (Vector embeddings) - 4h
- Afternoon: US-206 (Query API setup) - 3h

**Deliverables:**
- Event embeddings generation
- Similarity search working
- Spring Boot API running
- Swagger UI accessible

#### Day 4 (Oct 13) - Advanced AI 🚀
**Goal:** RAG system + query endpoints
- Morning: US-212 (RAG system) - 6h
- Afternoon: US-207 (Query endpoints) - 3h

**Deliverables:**
- ChromaDB with threat intelligence
- Python RAG microservice
- REST API endpoints for queries

#### Day 5 (Oct 14) - AI Polish ✨
**Goal:** Natural language queries + anomaly detection
- Morning: US-213 (AI query interface) - 4h
- Afternoon: US-215 (Anomaly detection) - 3h

**Deliverables:**
- Natural language query endpoint
- Statistical baseline tracking
- Anomaly detection working

#### Day 6 (Oct 15) - Deploy ☁️ (Optional)
**Goal:** Cloud deployment
- Morning: US-305 (AWS deployment) - 6h
- Afternoon: Integration testing

**Deliverables:**
- Infrastructure as code (Terraform)
- Application deployed to AWS
- Public demo URL

#### Day 7 (Oct 16) - Buffer 🛡️
**Goal:** Catch-up and polish
- Incomplete items from earlier days
- Bug fixes and integration testing
- Documentation updates
- Code cleanup

### Sprint 2 Definition of Done

**Required for Sprint Completion:**
- [ ] All 9 demo-critical issues closed
- [ ] Prometheus + Grafana monitoring operational
- [ ] AI features working (LLM, embeddings, RAG)
- [ ] Query API functional with 5+ endpoints
- [ ] End-to-end integration test passing
- [ ] Documentation updated
- [ ] No critical bugs

**Nice-to-Have:**
- [ ] 2-4 demo-nice issues completed
- [ ] AWS deployment working
- [ ] Blog post draft started

---

## Sprint 3: Demo Preparation 📅

**Status:** PLANNED  
**Duration:** Oct 17-19, 2025 (3 days)  
**Goal:** Package for maximum interview impact

### Demo Preparation Tasks

#### Day 1 (Oct 17) - Video Production 🎥
**Goal:** Record impressive demo video

**Tasks:**
- [ ] Write demo script (5 minutes)
- [ ] Practice demo 3x
- [ ] Record demo with screen capture
- [ ] Edit video (add titles, captions)
- [ ] Upload to YouTube (unlisted)
- [ ] Test video playback

**Deliverables:**
- 5-7 minute demo video
- Clear audio and video
- Professional presentation

#### Day 2 (Oct 18) - Content Creation 📝
**Goal:** Write blog post and polish GitHub

**Morning - Blog Post (4h):**
- [ ] Write 1500-word technical blog post
- [ ] Include architecture diagrams
- [ ] Explain AI integration decisions
- [ ] Add code samples
- [ ] Publish on Medium/personal blog

**Afternoon - GitHub Polish (3h):**
- [ ] Update README with demo video
- [ ] Add badges (build, tests, coverage)
- [ ] Professional screenshots
- [ ] Clean commit history
- [ ] Review all documentation

**Deliverables:**
- Published blog post
- Polished GitHub repository
- Professional presentation

#### Day 3 (Oct 19) - Final Review & Submission 🎯
**Goal:** Submit application

**Morning - Final Checks (3h):**
- [ ] Test entire system end-to-end
- [ ] Verify all links work
- [ ] Review documentation for typos
- [ ] Practice demo presentation
- [ ] Prepare interview talking points

**Afternoon - Application (2h):**
- [ ] Write cover letter (mention StreamGuard)
- [ ] Update resume
- [ ] Update LinkedIn
- [ ] Submit application
- [ ] LinkedIn post about project

**Deliverables:**
- Application submitted
- All materials ready for interview
- Project complete! 🎉

---

## Backlog Priorities

### Technology Coverage Checklist

Track which job requirements are demonstrated:

#### Required Technologies (From Job Description)

- [x] **C++** - Stream processor (US-104, US-105)
- [x] **Java** - Event generator (US-103), Query API (US-206-207)
- [x] **Kafka** - Event streaming (US-103, US-104)
- [x] **RocksDB** - Embedded storage (US-105)
- [x] **Git** - Version control (all commits)
- [x] **Unix/Linux** - macOS development environment

#### Bonus Technologies (Bonus Points)

- [ ] **Prometheus** - Metrics collection (US-301) 🚧
- [ ] **Grafana** - Visualization (US-302) 🚧
- [ ] **Spring Boot** - REST API (US-206-207) 🚧
- [ ] **AWS** - Cloud deployment (US-305) 📅
- [x] **CMake** - C++ build system ✅

#### AI/ML Technologies (Differentiator)

- [ ] **LLMs (GPT-4)** - Threat analysis (US-210) 🚧
- [ ] **Vector Embeddings** - Similarity search (US-211) 🚧
- [ ] **RAG** - Threat intelligence (US-212) 🚧
- [ ] **ChromaDB** - Vector database (US-212) 🚧

**Current Coverage:** 60% → **Target:** 100% ✅

### Obsolete Issues (Closed)

These were part of the original plan but don't align with the demo strategy:

- ❌ **US-201:** Multi-threaded Processing - Over-engineering for demo
- ❌ **US-204:** Sliding Windows - Too complex, not needed
- ❌ **US-205:** Pattern Detection - Replaced by AI
- ❌ **US-209:** ML Training Data Export - Nice-to-have, not critical
- ❌ **US-303:** Load Testing - Performance not focus
- ❌ **US-304:** Advanced Error Handling - Basic is sufficient
- ❌ **US-310:** Old LLM Narratives - Replaced by US-210
- ❌ **US-311:** ONNX ML Model - Too complex for timeline

**Reason:** Focus on technology breadth and AI features, not production optimization.

---

## Issue Labels Guide

### Priority Labels

| Label | Meaning | When to Use |
|-------|---------|-------------|
| `demo-critical` | Must complete for demo | Core features, job requirements, AI |
| `demo-nice` | Enhances demo | Polish, nice-to-have features |
| `bonus-points` | Extra credit | AWS, advanced features |

### Technology Labels

| Label | Meaning | Examples |
|-------|---------|----------|
| `cpp` | C++ codebase | Stream processor, RocksDB integration |
| `java` | Java codebase | Event generator, Query API |
| `python` | Python codebase | RAG microservice |
| `kafka` | Apache Kafka | Producers, consumers |
| `storage` | RocksDB | Storage, queries |
| `ai` | AI/ML features | LLM, embeddings, RAG |
| `monitoring` | Observability | Prometheus, Grafana |
| `query-api` | REST API | Spring Boot endpoints |
| `infrastructure` | DevOps | Docker, AWS, Terraform |
| `testing` | Tests | Unit, integration, e2e |

### Status Labels

| Label | Meaning | When to Use |
|-------|---------|-------------|
| `blocked` | Cannot proceed | Waiting on dependency, API key, etc. |
| `in-progress` | Currently working | Active development |
| `needs-review` | Ready for review | Code complete, needs validation |
| `bug` | Defect | Something broken |

### Sprint Labels

| Label | Meaning |
|-------|---------|
| `sprint-1` | Foundation sprint (Oct 8-9) |
| `sprint-2` | Tech + AI sprint (Oct 10-16) |
| `sprint-3` | Demo prep sprint (Oct 17-19) |

### Special Labels

| Label | Meaning | When to Use |
|-------|---------|-------------|
| `job-requirement` | From job description | Mandatory tech from posting |
| `documentation` | Docs update needed | README, guides, diagrams |
| `security` | Security feature | Threat detection, anomaly detection |
| `performance` | Performance related | Optimization, benchmarking |

---

## Velocity & Capacity Planning

### Sprint 1 Velocity (Actual)

| Metric | Value |
|--------|-------|
| **Duration** | 2 days |
| **Stories Completed** | 6 |
| **Total Effort** | 12 hours |
| **Average Story Size** | 2 hours |
| **Daily Capacity** | 6 hours |

### Sprint 2 Capacity (Planned)

| Metric | Value |
|--------|-------|
| **Duration** | 7 days |
| **Available Hours** | 56 hours (8h/day) |
| **Planned Effort** | 49 hours |
| **Buffer** | 7 hours (12.5%) |
| **Stories Planned** | 13 |
| **Average Story Size** | 3.8 hours |

### Capacity Assumptions

**Daily Capacity:**
- Full-time job: -8 hours
- Sleep/meals: -8 hours
- Personal time: -2 hours
- **Available:** 6 hours/day weekdays

**Weekend Boost:**
- Available: 12 hours/day (Sat/Sun)
- Use for catch-up if behind

### Risk Mitigation

**If Behind Schedule:**
1. **Cut scope:** Drop nice-to-have issues (US-202, US-203, US-215)
2. **Simplify:** Reduce AI features (skip RAG, keep LLM only)
3. **Skip AWS:** Deploy can wait until after interview
4. **Work weekend:** Use Saturday/Sunday for catch-up

**If Ahead of Schedule:**
1. Polish documentation
2. Add AWS deployment
3. Write blog post early
4. Record demo video early
5. Practice demo presentation

---

## Success Metrics

### Technical Metrics

**Sprint 2 Goals:**
- [ ] Monitoring working (Prometheus + Grafana)
- [ ] AI analysis generating for threats
- [ ] Query API responding < 100ms (P95)
- [ ] End-to-end test passing
- [ ] Documentation complete

**Demo Metrics:**
- [ ] 5-minute video recorded
- [ ] All technologies demonstrated
- [ ] AI features impressive ("wow factor")
- [ ] GitHub looks professional

### Interview Metrics

**Primary Goal:** Get first interview ✅
**Secondary Goals:**
- Demonstrate technical depth
- Show leadership thinking
- Prove domain expertise
- Stand out from candidates

---

## Quick Reference

### Issue Creation

```bash
# Create new issue
gh issue create \
  --title "US-XXX: Title" \
  --body "Description" \
  --label "demo-critical,cpp,ai" \
  --milestone "Sprint 2"

# List sprint issues
gh issue list --milestone "Sprint 2"

# View issue details
gh issue view 123
```

### Issue Workflow

```bash
# Start working on issue
gh issue develop 123 --checkout

# Mark in progress
gh issue edit 123 --add-label "in-progress"

# Complete and close
git commit -m "feat: Complete US-123 - Title (#123)"
git push
gh issue close 123 --comment "Completed"
```

### Sprint Queries

```bash
# Show sprint progress
gh issue list --milestone "Sprint 2" --json number,title,state

# Show blocked issues
gh issue list --label "blocked"

# Show critical issues
gh issue list --label "demo-critical" --state open
```

---

**Last Updated:** October 9, 2025  
**Author:** Jose Ortuno  
**Status:** Sprint 2 Ready to Start 🚀  
**Next Sprint:** Begins October 10, 2025