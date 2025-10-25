# Sprint 10: E2E Test Issue Resolution - 100% Pass Rate

**Sprint Goal:** Fix all identified issues from Sprint 9 E2E testing to achieve 100% test pass rate

**Sprint Duration:** 1 day (October 25, 2025)

**Status:** 🚧 PLANNING → EXECUTION

---

## Sprint Context

Following comprehensive E2E integration testing of Sprint 9 deliverables, we identified 3 issues preventing a 100% pass rate:

**Current Test Results:** 80% (8/10 tests passing)

**Target:** 100% (10/10 tests passing)

---

## Issues to Resolve

### Issue #1: User Filtering Not Implemented ⚠️

**Severity:** Medium
**Test Case:** "Test user-specific query"
**Current Status:** FAIL

**Problem:**
```json
// Request
{
  "question": "Show me recent events for user alice",
  "user_id": "alice"
}

// Actual: Returns all events
// Expected: Only alice's events
```

**Root Cause:**
The `user_id` parameter is accepted by the API but not used to filter events from the Java API backend.

**Fix Options:**

**Option A: Java API Filtering (Recommended)**
- Add `?user={username}` query parameter to `/api/events/recent`
- Implement RocksDB prefix scan with user key
- Most efficient (server-side filtering)

**Option B: GenAI Assistant Filtering**
- Filter events client-side in `SecurityAssistant.answer_query()`
- Less efficient (fetch all, filter in Python)
- Simpler to implement (no Java changes)

**Decision:** Option B (GenAI Assistant filtering) for speed
- Can implement Option A in future optimization sprint
- Allows us to deliver Sprint 10 in 1 day
- Still provides correct functionality

**Estimated Effort:** 1 hour

---

### Issue #2: OpenAI Cost Tracking Not Recording ⚠️

**Severity:** Low
**Test Case:** "Validate cost tracking in metrics"
**Current Status:** FAIL

**Problem:**
```prometheus
# HELP genai_openai_cost_dollars_total Estimated OpenAI API cost in dollars
# TYPE genai_openai_cost_dollars_total counter
# (no values - counter exists but never incremented)
```

**Root Cause:**
Token usage IS being recorded, but the cost calculation/increment is not being called.

**Expected Cost:**
```
Query 1: 843 prompt + 451 completion = ~$0.0004
Query 2: 843 prompt + 451 completion = ~$0.0004
Total:   ~$0.0008 for 2 queries
```

**Fix Location:**
`genai-assistant/app/utils/metrics.py` - `record_openai_usage()` function

**Investigation Needed:**
1. Check if `OPENAI_PRICING` lookup is working
2. Verify model name matches pricing dictionary keys
3. Ensure cost counter increment is being called
4. Add debug logging to cost calculation

**Estimated Effort:** 30 minutes

---

### Issue #3: RAG Service NumPy Compatibility ⚠️

**Severity:** Low (Non-blocking)
**Test Case:** "Threat intel integration"
**Current Status:** SKIPPED (service down)

**Problem:**
```python
AttributeError: `np.float_` was removed in the NumPy 2.0 release.
Use `np.float64` instead.
```

**Root Cause:**
ChromaDB has a dependency on deprecated NumPy 1.x API that was removed in NumPy 2.0.

**Fix Options:**

**Option A: Downgrade NumPy (Temporary)**
```bash
pip install "numpy<2.0"
```
- Immediate fix
- Not future-proof
- Waiting for ChromaDB update

**Option B: Update ChromaDB (Preferred)**
```bash
pip install --upgrade chromadb
```
- Check if newer version supports NumPy 2.0
- More sustainable

**Option C: Pin Dependencies**
```txt
# requirements.txt
numpy==1.26.4  # Pin to last 1.x version
chromadb==0.4.22
```

**Decision:** Option C (Pin dependencies) with Option B fallback
- Check for ChromaDB update first
- If not available, pin NumPy to 1.26.4
- Document in requirements.txt
- Monitor ChromaDB releases for NumPy 2.0 support

**Estimated Effort:** 15 minutes

---

## Sprint 10 Deliverables

### 1. User Filtering Implementation

**Files to Modify:**
- `genai-assistant/app/services/assistant.py`
- `genai-assistant/app/services/java_api.py`

**Changes:**

#### `java_api.py` - Add user parameter support
```python
async def get_recent_events(
    self,
    limit: int = 100,
    user: Optional[str] = None  # NEW PARAMETER
) -> List[Dict[str, Any]]:
    """Fetch recent events from Java API"""
    try:
        params = {"limit": limit}
        # Note: Java API doesn't support user filtering yet
        # We'll filter client-side in assistant.py

        response = await self.client.get(
            f"{self.base_url}/api/events/recent",
            params=params
        )

        events = response.json()

        # Client-side filtering if user specified
        if user:
            events = [e for e in events if e.get('user') == user]

        return events
    except Exception as e:
        logger.error(f"Failed to fetch events: {str(e)}")
        raise
```

#### `assistant.py` - Pass user_id to Java API client
```python
async def answer_query(
    self,
    question: str,
    context_window: str = "1h",
    user_id: Optional[str] = None,
    include_threat_intel: bool = False
) -> Dict[str, Any]:
    """Answer natural language query about security events"""

    # ... existing code ...

    # Fetch events with user filtering
    events = await self.java_api.get_recent_events(
        limit=100,
        user=user_id  # PASS USER ID
    )

    # ... rest of implementation ...
```

**Test Cases:**
1. Query with `user_id="alice"` - should return only alice's events
2. Query with `user_id="bob"` - should return only bob's events
3. Query without `user_id` - should return all events
4. Query with `user_id="nonexistent"` - should return empty list

---

### 2. Cost Tracking Fix

**Files to Modify:**
- `genai-assistant/app/utils/metrics.py`

**Investigation Steps:**
```python
def record_openai_usage(model: str, prompt_tokens: int, completion_tokens: int):
    """Record OpenAI API usage metrics"""

    # Step 1: Log for debugging
    logger.debug(f"Recording usage - model: {model}, prompt: {prompt_tokens}, completion: {completion_tokens}")

    # Step 2: Record tokens (THIS IS WORKING)
    OPENAI_TOKENS.labels(model=model, token_type="prompt").inc(prompt_tokens)
    OPENAI_TOKENS.labels(model=model, token_type="completion").inc(completion_tokens)

    # Step 3: Calculate cost (NEEDS DEBUGGING)
    logger.debug(f"Available pricing models: {list(OPENAI_PRICING.keys())}")

    if model in OPENAI_PRICING:
        logger.debug(f"Found pricing for {model}")
        input_cost = prompt_tokens * OPENAI_PRICING[model]['input']
        output_cost = completion_tokens * OPENAI_PRICING[model]['output']
        total_cost = input_cost + output_cost

        logger.info(f"Cost calculated: ${total_cost:.6f} ({model})")

        # Step 4: Increment counter (CHECK IF THIS EXECUTES)
        OPENAI_COST.labels(model=model).inc(total_cost)
        logger.debug(f"Cost counter incremented")
    else:
        logger.warning(f"No pricing found for model: {model}")
```

**Likely Issue:**
Model name mismatch between OpenAI response (`gpt-4o-mini-2024-07-18`) and pricing dictionary key (`gpt-4o-mini`).

**Fix:**
```python
# In OPENAI_PRICING dictionary, add all model variants
OPENAI_PRICING = {
    'gpt-4o-mini': {'input': 0.150 / 1_000_000, 'output': 0.600 / 1_000_000},
    'gpt-4o-mini-2024-07-18': {'input': 0.150 / 1_000_000, 'output': 0.600 / 1_000_000},
    'gpt-4o': {'input': 2.50 / 1_000_000, 'output': 10.00 / 1_000_000},
    'gpt-4o-2024-08-06': {'input': 2.50 / 1_000_000, 'output': 10.00 / 1_000_000},
}

# Or use model family matching
def get_model_pricing(model: str) -> Optional[Dict[str, float]]:
    """Get pricing for model, matching by family if exact match not found"""
    if model in OPENAI_PRICING:
        return OPENAI_PRICING[model]

    # Try matching by prefix (e.g., gpt-4o-mini-2024-07-18 -> gpt-4o-mini)
    for base_model in OPENAI_PRICING.keys():
        if model.startswith(base_model):
            return OPENAI_PRICING[base_model]

    return None
```

**Test Cases:**
1. Run query and check metrics endpoint for cost values
2. Verify cost matches manual calculation
3. Test with different models (if using Ollama, cost should be 0)

---

### 3. RAG Service NumPy Fix

**Files to Modify:**
- `rag-service/requirements.txt`

**Step 1: Check for ChromaDB update**
```bash
cd rag-service
source venv/bin/activate
pip index versions chromadb | head -5
```

**Step 2: Try upgrade**
```bash
pip install --upgrade chromadb
python -c "import chromadb; print(chromadb.__version__)"
uvicorn main:app --port 8000 &
curl http://localhost:8000/health
```

**Step 3: If upgrade doesn't work, pin NumPy**
```txt
# rag-service/requirements.txt
fastapi==0.115.6
uvicorn==0.34.0
chromadb==0.4.22
numpy==1.26.4  # Pin to last 1.x version (NumPy 2.0 not compatible with ChromaDB yet)
openai==1.59.5
python-dotenv==1.0.1
httpx==0.28.1
```

**Step 4: Update documentation**
```markdown
# rag-service/README.md

## Known Issues

### NumPy 2.0 Compatibility

ChromaDB (as of v0.4.22) is not compatible with NumPy 2.0. If you encounter:

```
AttributeError: `np.float_` was removed in the NumPy 2.0 release
```

**Solution:** NumPy is pinned to 1.26.4 in requirements.txt. If you've already installed NumPy 2.0:

```bash
pip uninstall numpy
pip install numpy==1.26.4
```

**Status:** Monitoring ChromaDB releases for NumPy 2.0 support.
```

**Test Cases:**
1. RAG service starts without errors
2. Health endpoint returns success
3. Seed database operation works
4. Query operation returns results
5. GenAI Assistant can fetch threat intel

---

## Implementation Order

### Phase 1: Quick Wins (45 minutes)
1. ✅ **Cost Tracking Fix** (30 min)
   - Add debug logging
   - Fix model name matching
   - Test with sample query
   - Verify metrics display cost

2. ✅ **RAG Service Fix** (15 min)
   - Pin NumPy version
   - Restart service
   - Test health endpoint

### Phase 2: User Filtering (1 hour)
3. ✅ **Implement User Filtering** (45 min)
   - Modify `java_api.py` with client-side filtering
   - Update `assistant.py` to pass user_id
   - Add unit tests

4. ✅ **Integration Testing** (15 min)
   - Test with various users
   - Validate empty results for nonexistent users
   - Verify natural language still works

### Phase 3: Validation (30 minutes)
5. ✅ **Re-run Full E2E Test Suite** (20 min)
   - Execute all 10 test cases
   - Verify 100% pass rate
   - Generate updated test report

6. ✅ **Documentation Update** (10 min)
   - Update E2E test report
   - Add Sprint 10 notes to README
   - Create handoff document

---

## Success Criteria

### Test Results
- [ ] Issue #1 resolved: User filtering test PASSES
- [ ] Issue #2 resolved: Cost tracking test PASSES
- [ ] Issue #3 resolved: RAG service test PASSES
- [ ] Overall E2E test pass rate: 100% (10/10)

### Code Quality
- [ ] All fixes have unit tests
- [ ] No new issues introduced
- [ ] Code follows existing patterns
- [ ] Documentation updated

### Performance
- [ ] User filtering adds <50ms latency
- [ ] Cost tracking adds <1ms overhead
- [ ] RAG service response time <500ms

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Client-side filtering slow for large datasets | Medium | Low | Future: move to Java API server-side filtering |
| Model name variants keep changing | Low | Low | Robust prefix matching in pricing lookup |
| ChromaDB never supports NumPy 2.0 | Low | Low | Consider alternative vector DB (Weaviate, Milvus) |
| New issues discovered during fixes | Medium | Medium | Thorough testing after each fix |

---

## Rollback Plan

If any fix causes regressions:

1. **User Filtering:** Remove filtering logic, revert to all-events behavior
2. **Cost Tracking:** Comment out cost counter increment, keep token tracking
3. **RAG Service:** Keep service disabled, GenAI works without it

All changes are isolated and can be reverted independently.

---

## Sprint Timeline

```
Hour 0:00 - Sprint Kickoff
         ├─ Review E2E test report
         └─ Create Sprint 10 plan

Hour 0:15 - Phase 1: Quick Wins
         ├─ Fix cost tracking (debug + model matching)
         ├─ Test cost metrics
         ├─ Fix RAG NumPy dependency
         └─ Restart RAG service

Hour 1:00 - Phase 2: User Filtering
         ├─ Implement client-side filtering
         ├─ Add unit tests
         └─ Integration testing

Hour 2:00 - Phase 3: Validation
         ├─ Re-run full E2E test suite
         ├─ Verify 100% pass rate
         ├─ Update documentation
         └─ Create Sprint 10 handoff

Hour 2:30 - Sprint Complete ✅
```

---

## Dependencies

**None** - All fixes are self-contained within GenAI Assistant and RAG Service.

---

## Post-Sprint Actions

### Immediate
- [ ] Commit all changes with clear messages
- [ ] Update E2E test report with new results
- [ ] Create Sprint 10 handoff document

### Future Sprints
- [ ] **Sprint 11:** Move user filtering to Java API (server-side)
- [ ] **Sprint 11:** Add query caching to reduce OpenAI costs
- [ ] **Sprint 11:** Implement rate limiting
- [ ] **Sprint 12:** Add authentication and authorization

---

## Notes

**Philosophy:** Small, focused sprint to achieve 100% test coverage before moving forward.

**Approach:** Fix issues in order of impact and effort (quick wins first).

**Testing:** Re-run full E2E suite after all fixes to ensure no regressions.

---

**Sprint Status:** 🚀 READY TO EXECUTE

**Estimated Duration:** 2.5 hours

**Target Completion:** October 25, 2025 18:00 CDT
