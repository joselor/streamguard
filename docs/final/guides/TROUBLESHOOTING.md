# StreamGuard Troubleshooting Guide

Comprehensive troubleshooting guide for common issues and their solutions.

## Table of Contents

1. [Stream Processor Issues](#stream-processor-issues)
2. [Query API Issues](#query-api-issues)
3. [Kafka Issues](#kafka-issues)
4. [RocksDB Issues](#rocksdb-issues)
5. [AI/ML Issues](#aiml-issues)
6. [Performance Issues](#performance-issues)
7. [Deployment Issues](#deployment-issues)

---

## Stream Processor Issues

### Issue: "Failed to connect to Kafka"

**Symptoms:**
```
[Error] Failed to connect to Kafka broker: Connection refused
[Error] rd_kafka_consumer_poll: Local: Broker transport failure
```

**Causes:**
- Kafka broker not running
- Incorrect broker address
- Network connectivity issues
- Firewall blocking port 9092

**Solutions:**

1. **Verify Kafka is running:**
   ```bash
   docker-compose ps kafka
   # or
   systemctl status kafka
   ```

2. **Test connectivity:**
   ```bash
   telnet localhost 9092
   # or
   nc -zv localhost 9092
   ```

3. **Check broker address:**
   ```bash
   # Verify broker in config
   cat stream-processor/config.json

   # Use correct address for Docker:
   # - From host: localhost:9092
   # - From container: kafka:9092
   ```

4. **Restart Kafka:**
   ```bash
   docker-compose restart kafka
   sleep 30  # Wait for startup
   ```

---

### Issue: "Stream processor doesn't respond to kill signals (SIGINT/SIGTERM)"

**Symptoms:**
```bash
$ ./stream-processor &
[1] 12345

$ kill -SIGTERM 12345
# No response, process keeps running

$ kill -SIGINT 12345
# Still no response

# Only kill -9 works (force kill)
$ kill -9 12345
[1]+  Killed                  ./stream-processor
```

**Causes:**
- Old version before Sprint 6 signal handling fix
- Signal handler not properly connected to consumer loop
- Running in non-interactive mode where signals are blocked

**Solutions:**

1. **Verify you have latest version with signal handling fix:**
   ```bash
   # Check git commit
   git log --oneline -5 | grep signal

   # Should see commit like:
   # abc1234 fix: Fix signal handling in stream-processor for graceful shutdown

   # If not found, pull latest
   git pull origin main
   cd stream-processor && mkdir -p build && cd build
   cmake .. && make
   ```

2. **Use Ctrl+C to send SIGINT:**
   ```bash
   # Start in foreground
   ./stream-processor

   # Press Ctrl+C
   ^C
   [Main] Received signal 2, shutting down gracefully...
   [KafkaConsumer] Consumer loop ended
   [KafkaConsumer] Shutting down...
   [Main] Stream processor terminated successfully
   ```

3. **Send SIGTERM properly:**
   ```bash
   # Get PID
   PID=$(pgrep stream-processor)

   # Send SIGTERM (signal 15)
   kill -SIGTERM $PID
   # or just:
   kill $PID

   # Wait a few seconds for graceful shutdown
   sleep 3

   # Check if still running
   ps -p $PID
   ```

4. **Check logs for shutdown message:**
   ```bash
   # Look for shutdown messages
   grep "shutting down" stream-processor/logs/output.log

   # Should see:
   # [Main] Received signal 15, shutting down gracefully...
   # [KafkaConsumer] Consumer loop ended
   # [KafkaConsumer] Closing consumer and committing offsets...
   # [Main] Stream processor terminated successfully
   ```

5. **If signals still don't work (Docker/systemd):**
   ```bash
   # In Docker
   docker stop streamguard-stream-processor --time=10
   # Sends SIGTERM, waits 10s, then SIGKILL

   # In systemd
   systemctl stop streamguard-processor
   # Uses configured TimeoutStopSec
   ```

6. **Last resort - force kill (not recommended):**
   ```bash
   # Force kill without cleanup
   kill -9 $(pgrep stream-processor)

   # Note: This skips:
   # - Kafka offset commits (may reprocess events)
   # - RocksDB flush (may lose recent writes)
   # - Metrics final export
   ```

**How Sprint 6 Fix Works:**
```cpp
// Global atomic flag coordinated with signal handler
std::atomic<bool> running_(true);

void signalHandler(int signal) {
    running_ = false;  // Signal all components to stop
}

// Consumer checks external flag in poll loop
consumer.start(&running_);  // Pass external flag

// Inside KafkaConsumer::start()
while (running_ && (externalRunning == nullptr || *externalRunning)) {
    // Poll and process...
    // Now checks both internal AND external running flags
}
```

---

### Issue: "RocksDB: Corruption detected"

**Symptoms:**
```
[Error] RocksDB::Open failed: Corruption: block checksum mismatch
[Fatal] Cannot open database
```

**Causes:**
- Disk corruption
- Improper shutdown
- Disk full
- Hardware failure

**Solutions:**

1. **Check disk space:**
   ```bash
   df -h
   # Ensure >20% free space
   ```

2. **Try repair (may lose data):**
   ```bash
   cd stream-processor/build

   # Backup first!
   cp -r data/events.db data/events.db.backup

   # Attempt repair
   ./rocksdb_repair --db=./data/events.db
   ```

3. **Restore from backup:**
   ```bash
   # Remove corrupted DB
   rm -rf data/events.db

   # Restore from backup
   tar -xzf /backups/events.db.20241008.tar.gz -C data/

   # Reset Kafka offset to backup timestamp
   kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group streamguard-processor \
     --topic security-events \
     --reset-offsets --to-datetime 2024-10-08T12:00:00.000 \
     --execute
   ```

4. **Rebuild from Kafka (last resort):**
   ```bash
   rm -rf data/events.db
   mkdir -p data

   # Start processor from beginning
   ./stream-processor \
     --broker localhost:9092 \
     --topic security-events \
     --group fresh-rebuild \
     --db ./data/events.db
   ```

---

### Issue: "High memory usage"

**Symptoms:**
```
[Warning] Memory usage: 8.5GB / 8GB
[Error] std::bad_alloc
Process killed (OOM)
```

**Causes:**
- Too many user baselines in memory
- Large RocksDB block cache
- Memory leaks

**Solutions:**

1. **Check current usage:**
   ```bash
   top -p $(pgrep stream-processor)

   # Detailed memory breakdown
   pmap -x $(pgrep stream-processor)
   ```

2. **Reduce RocksDB cache:**
   ```cpp
   // In event_store.cpp
   options.block_cache = rocksdb::NewLRUCache(256 * 1024 * 1024);  // 256MB instead of 512MB
   ```

3. **Limit baseline retention:**
   ```cpp
   // In anomaly_detector.cpp
   // Add baseline eviction for inactive users
   if (baselines_.size() > 10000) {
       evictOldestBaselines(1000);
   }
   ```

4. **Increase system limits:**
   ```bash
   # Increase available memory
   # Add to /etc/security/limits.conf
   * soft memlock unlimited
   * hard memlock unlimited

   # Or use Docker with more memory
   docker run --memory=16g ...
   ```

---

## Query API Issues

### Issue: "Cannot read RocksDB database"

**Symptoms:**
```java
org.rocksdb.RocksDBException: IO error: No such file or directory
```

**Causes:**
- Incorrect ROCKSDB_PATH
- Database not created yet
- Permission issues

**Solutions:**

1. **Verify path is correct:**
   ```bash
   echo $ROCKSDB_PATH
   # Should be absolute path like: /Users/user/streamguard/stream-processor/build/data/events.db

   ls -la $ROCKSDB_PATH
   ```

2. **Check permissions:**
   ```bash
   # Query API needs read access
   chmod -R 755 /path/to/events.db
   ```

3. **Ensure database exists:**
   ```bash
   # Start stream processor first to create DB
   cd stream-processor/build
   ./stream-processor --broker localhost:9092 --topic security-events --group init

   # Wait for database creation
   ls -la data/events.db
   ```

4. **Verify column families:**
   ```bash
   # List column families
   ./rocksdb_ldb --db=./data/events.db list_column_families

   # Should show: default, ai_analysis, embeddings, anomalies
   ```

---

### Issue: "Query returns empty results"

**Symptoms:**
- API responds 200 OK but empty array `[]`
- Event count shows 0

**Causes:**
- No events processed yet
- Querying wrong column family
- Time range filter too restrictive

**Solutions:**

1. **Check event count:**
   ```bash
   curl http://localhost:8081/api/events/count
   # Should be > 0
   ```

2. **Verify events in RocksDB:**
   ```bash
   cd stream-processor/build
   ./rocksdb_ldb --db=./data/events.db scan --max_keys=10
   ```

3. **Send test events:**
   ```bash
   python3 scripts/generate_test_data.py \
     --broker localhost:9092 \
     --events 100

   # Wait a few seconds
   sleep 5

   # Query again
   curl http://localhost:8081/api/events?limit=10
   ```

4. **Check logs for errors:**
   ```bash
   # Query API logs
   tail -f query-api/logs/spring.log

   # Look for RocksDB errors
   grep "RocksDB" logs/spring.log
   ```

---

## Kafka Issues

### Issue: "Consumer lag increasing"

**Symptoms:**
```
Consumer group lag: 50000 messages
Lag increasing continuously
```

**Causes:**
- Processing too slow
- Not enough consumer instances
- Slow AI API responses

**Solutions:**

1. **Check current lag:**
   ```bash
   kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group streamguard-processor \
     --describe
   ```

2. **Increase parallelism:**
   ```bash
   # Add more partitions (one-time operation)
   kafka-topics.sh \
     --bootstrap-server localhost:9092 \
     --topic security-events \
     --alter --partitions 8

   # Start more processor instances (match partition count)
   ./stream-processor --group streamguard-processor &
   ./stream-processor --group streamguard-processor &
   ```

3. **Optimize processing:**
   ```cpp
   // Disable slow AI analysis temporarily
   if (false) {  // Toggle AI analysis
       auto analysis = ai_analyzer.analyze(event, context);
   }

   // Or increase timeout
   ai_analyzer.setTimeout(2000);  // 2s instead of 5s
   ```

4. **Monitor metrics:**
   ```bash
   curl http://localhost:8080/metrics | grep processing_latency
   ```

---

### Issue: "Messages being reprocessed"

**Symptoms:**
```
[Warning] Duplicate event ID: evt_001
Same events processed multiple times
```

**Causes:**
- Consumer crash before offset commit
- Manual offset reset
- Consumer rebalancing

**Solutions:**

1. **Enable auto-commit (if acceptable):**
   ```cpp
   rd_kafka_conf_set(conf, "enable.auto.commit", "true", ...);
   rd_kafka_conf_set(conf, "auto.commit.interval.ms", "5000", ...);
   ```

2. **Implement idempotency:**
   ```cpp
   bool EventStore::putEvent(const Event& event) {
       // Check if event exists first
       if (getEvent(event.event_id).has_value()) {
           LOG_WARN("Event already exists, skipping: " << event.event_id);
           return true;  // Idempotent - no error
       }

       // Proceed with write
       return db_->Put(write_options, default_cf_, key, value).ok();
   }
   ```

3. **Check consumer group status:**
   ```bash
   kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group streamguard-processor \
     --describe

   # Look for multiple instances on same partition
   ```

---

## RocksDB Issues

### Issue: "Write stalls"

**Symptoms:**
```
[Warning] RocksDB write stall detected
[Metrics] p95 latency: 2000ms (normally 50ms)
```

**Causes:**
- Too many Level 0 files
- Slow compaction
- Disk I/O bottleneck

**Solutions:**

1. **Check compaction stats:**
   ```bash
   cd stream-processor/build
   ./rocksdb_ldb --db=./data/events.db dump_live_files
   ```

2. **Tune compaction settings:**
   ```cpp
   options.level0_file_num_compaction_trigger = 2;  // Trigger earlier
   options.max_background_jobs = 8;  // More parallel compaction
   options.max_subcompactions = 4;
   ```

3. **Increase write buffer:**
   ```cpp
   options.write_buffer_size = 128 * 1024 * 1024;  // 128MB
   options.max_write_buffer_number = 4;  // More buffers
   ```

4. **Monitor disk I/O:**
   ```bash
   iostat -x 5  # Check %util column
   # If near 100%, consider faster disk (SSD)
   ```

---

### Issue: "Database size growing rapidly"

**Symptoms:**
```
events.db size: 500GB (expected: 100GB)
Disk space running out
```

**Causes:**
- Compression disabled
- No TTL/retention policy
- Accumulating old data

**Solutions:**

1. **Enable compression:**
   ```cpp
   options.compression = rocksdb::kLZ4Compression;
   options.bottommost_compression = rocksdb::kZSTDCompression;
   ```

2. **Check compression ratio:**
   ```bash
   ./rocksdb_ldb --db=./data/events.db dump_stats | grep Compression
   ```

3. **Implement TTL (requires code change):**
   ```cpp
   // Add TTL to options (30 days)
   rocksdb::Options options;
   options.ttl = 30 * 24 * 60 * 60;  // 30 days in seconds
   ```

4. **Manual cleanup:**
   ```bash
   # Backup first!
   tar -czf events.db.backup.tar.gz data/events.db

   # Compact database
   ./rocksdb_ldb --db=./data/events.db compact
   ```

---

## AI/ML Issues

### Issue: "OpenAI API errors"

**Symptoms:**
```
[Error] OpenAI API error: 401 Unauthorized
[Error] OpenAI API error: 429 Too Many Requests
[Error] OpenAI API timeout after 5000ms
```

**Solutions:**

**401 Unauthorized:**
```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Test API key
curl https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "test"}],
    "max_tokens": 10
  }'
```

**429 Rate Limit:**
```cpp
// Implement exponential backoff
int retry_count = 0;
int max_retries = 5;

while (retry_count < max_retries) {
    try {
        return callOpenAIAPI(prompt);
    } catch (const RateLimitException& e) {
        int backoff_ms = std::pow(2, retry_count) * 1000;  // 1s, 2s, 4s, 8s, 16s
        std::this_thread::sleep_for(std::chrono::milliseconds(backoff_ms));
        retry_count++;
    }
}
```

**Timeouts:**
```cpp
// Increase timeout
http_client_->set_read_timeout(10);  // 10 seconds

// Or skip AI analysis during high load (it's selective anyway)
if (current_load > threshold) {
    LOG_WARN("High load, skipping AI analysis");
    return std::nullopt;
}
```

---

### Issue: "AI analysis not running (selective mode)"

**Symptoms:**
```
curl http://localhost:8081/api/analyses/count
0

# All events processed but no AI analyses
```

**Causes:**
- AI disabled at startup (default behavior)
- No events meet trigger criteria (threat < 0.7 AND not anomalous)
- OPENAI_API_KEY not set

**Solutions:**

1. **Check if AI was enabled at startup:**
   ```bash
   # Look in stream-processor logs
   grep "\[AI\]" stream-processor/logs/output.log

   # Should see:
   # [AI] ✓ AI analysis ENABLED - Will analyze high-threat and anomalous events
   # Or:
   # [AI] AI analysis DISABLED (default)
   ```

2. **Verify trigger conditions are being met:**
   ```bash
   # Check for high-threat events
   curl 'http://localhost:8081/api/events/threat?minScore=0.7&limit=10'

   # Check for anomalies
   curl 'http://localhost:8081/api/anomalies/recent?limit=10'

   # If both return empty, AI won't trigger (by design)
   ```

3. **Enable AI at startup:**
   ```bash
   # Restart stream-processor
   ./scripts/start-stream-processor.sh

   # When prompted:
   [AI] Enable AI-powered threat analysis? (yes/no) [default: no]: yes
   ```

4. **Check OPENAI_API_KEY:**
   ```bash
   # Verify environment variable
   echo $OPENAI_API_KEY

   # Should start with: sk-proj-...

   # Set if missing
   export OPENAI_API_KEY="sk-proj-your-key-here"
   ```

5. **Generate events that trigger AI:**
   ```bash
   # Send high-threat event
   curl -X POST http://localhost:9092/topics/security-events \
     -H "Content-Type: application/json" \
     -d '{
       "event_id": "test-high-threat",
       "user": "alice",
       "timestamp": '$(date +%s000)',
       "type": "LOGIN_FAILED",
       "source_ip": "192.168.99.99",
       "geo_location": "Unknown",
       "threat_score": 0.85
     }'

   # Wait a few seconds, then check
   sleep 5
   curl http://localhost:8081/api/analyses/recent?limit=1
   ```

6. **Cost optimization is working as designed:**
   ```
   ✓ AI analyzes only 3-5% of events (high-threat or anomalous)
   ✓ Default: disabled unless user explicitly enables
   ✓ This saves 95%+ on API costs
   ```

---

### Issue: "Anomaly detection not working"

**Symptoms:**
```
Anomaly count: 0 (expected: >0)
All anomaly scores: 0.0
```

**Causes:**
- User baseline not ready
- Threshold too high
- Normal behavior patterns

**Solutions:**

1. **Check baseline status:**
   ```cpp
   // Add logging
   if (!baseline.is_baseline_ready) {
       LOG_INFO("Baseline not ready for user: " << user
                << " (events: " << baseline.total_events << "/100)");
   }
   ```

2. **Lower threshold temporarily:**
   ```cpp
   anomaly_detector.setThreshold(0.5);  // Instead of 0.7
   ```

3. **Verify scoring logic:**
   ```bash
   # Check metrics
   curl http://localhost:8080/metrics | grep anomaly_score

   # Should show distribution across buckets
   streamguard_anomaly_score_bucket{le="0.5"} 150
   streamguard_anomaly_score_bucket{le="0.7"} 45
   ```

4. **Generate anomalous events:**
   ```bash
   # Send events with unusual characteristics
   python3 << 'EOF'
   from kafka import KafkaProducer
   import json, time

   producer = KafkaProducer(bootstrap_servers=['localhost:9092'],
                           value_serializer=lambda v: json.dumps(v).encode())

   # Unusual event (new IP, late hour, multiple failures)
   event = {
       "event_id": f"evt_{int(time.time())}_anomaly",
       "user": "alice",
       "timestamp": int(time.time() * 1000),
       "type": "LOGIN_FAILED",
       "source_ip": "192.168.99.99",  # Never seen before
       "geo_location": "Unknown",
       "threat_score": 0.95
   }

   for _ in range(5):  # Send 5 failures
       producer.send('security-events', event)
       time.sleep(1)

   producer.flush()
   EOF
   ```

---

## Performance Issues

### Issue: "High CPU usage"

**Symptoms:**
```
CPU usage: 100% sustained
Stream processor using all cores
```

**Causes:**
- Too many events per second
- Inefficient processing
- Compaction overhead

**Solutions:**

1. **Profile with perf:**
   ```bash
   # Install perf
   sudo apt-get install linux-tools-generic

   # Profile for 30 seconds
   sudo perf record -g -p $(pgrep stream-processor) -- sleep 30

   # Analyze
   sudo perf report
   ```

2. **Optimize hot paths:**
   ```cpp
   // Use string_view to avoid copies
   std::string_view key(iterator->key().data(), iterator->key().size());

   // Reserve vector capacity
   std::vector<Event> events;
   events.reserve(limit);

   // Move instead of copy
   events.push_back(std::move(event));
   ```

3. **Reduce AI analysis frequency:**
   ```cpp
   // Only analyze high threat events
   if (event.threat_score > 0.7) {
       auto analysis = ai_analyzer.analyze(event, context);
   }
   ```

4. **Scale horizontally:**
   ```bash
   # Add more processor instances
   # Each handles subset of partitions
   ```

---

### Issue: "Query API slow responses"

**Symptoms:**
```
GET /api/anomalies?limit=100 - 5000ms (expected: <100ms)
```

**Causes:**
- Large result sets
- Missing indexes (RocksDB uses keys as index)
- Cold cache

**Solutions:**

1. **Add pagination:**
   ```java
   @GetMapping("/anomalies")
   public ResponseEntity<List<AnomalyResult>> getAnomalies(
       @RequestParam(defaultValue = "0") int offset,
       @RequestParam(defaultValue = "20") int limit) {

       // Implement offset-based pagination
       return ResponseEntity.ok(queryService.getAnomalies(offset, limit));
   }
   ```

2. **Warm up cache:**
   ```bash
   # Run common queries on startup
   curl 'http://localhost:8081/api/events?limit=100'
   curl 'http://localhost:8081/api/anomalies/high-score?threshold=0.7'
   ```

3. **Use appropriate limits:**
   ```java
   // Enforce maximum limit
   if (limit > 1000) limit = 1000;
   ```

4. **Add caching layer:**
   ```java
   @Cacheable(value = "anomalies", key = "#threshold + '-' + #limit")
   public List<AnomalyResult> getHighScoreAnomalies(double threshold, int limit) {
       // ...
   }
   ```

---

## Deployment Issues

### Issue: "Kubernetes pods crashlooping"

**Symptoms:**
```
NAME                                     READY   STATUS             RESTARTS
streamguard-processor-7d6c5f9b8d-abc12   0/1     CrashLoopBackOff   5
```

**Solutions:**

1. **Check logs:**
   ```bash
   kubectl logs streamguard-processor-7d6c5f9b8d-abc12 -n streamguard
   kubectl logs streamguard-processor-7d6c5f9b8d-abc12 -n streamguard --previous
   ```

2. **Describe pod:**
   ```bash
   kubectl describe pod streamguard-processor-7d6c5f9b8d-abc12 -n streamguard
   # Look for OOMKilled, ImagePullBackOff, etc.
   ```

3. **Common fixes:**
   ```yaml
   # Increase resource limits
   resources:
     limits:
       memory: "8Gi"  # Was 4Gi
       cpu: "4000m"   # Was 2000m

   # Fix liveness probe
   livenessProbe:
     initialDelaySeconds: 60  # Was 30
     periodSeconds: 30        # Was 10
   ```

---

## Diagnostic Commands

### Health Check Script

```bash
#!/bin/bash
# streamguard-healthcheck.sh

echo "=== StreamGuard Health Check ==="

# 1. Kafka
echo -n "Kafka: "
nc -zv localhost 9092 2>&1 | grep succeeded && echo "OK" || echo "FAIL"

# 2. Stream Processor
echo -n "Stream Processor: "
curl -s http://localhost:8080/metrics > /dev/null && echo "OK" || echo "FAIL"

# 3. Query API
echo -n "Query API: "
curl -s http://localhost:8081/actuator/health | grep UP && echo "OK" || echo "FAIL"

# 4. Event Count
echo -n "Event Count: "
curl -s http://localhost:8081/api/events/count

# 5. Anomaly Count
echo -n "Anomaly Count: "
curl -s http://localhost:8081/api/anomalies/count

# 6. Disk Space
echo "Disk Space:"
df -h | grep -E '(Filesystem|/data|events.db)'

# 7. RocksDB Size
echo "RocksDB Size:"
du -sh stream-processor/build/data/events.db

echo "=== End Health Check ==="
```

Run with: `bash streamguard-healthcheck.sh`

---

## Getting Help

If issues persist:

1. **Enable debug logging:**
   ```cpp
   // C++ processor
   LOG_LEVEL = DEBUG;
   ```

   ```properties
   # Java query API
   logging.level.com.streamguard=DEBUG
   logging.level.org.rocksdb=DEBUG
   ```

2. **Collect diagnostics:**
   ```bash
   # System info
   uname -a > diagnostics.txt
   cat /etc/os-release >> diagnostics.txt

   # Process info
   ps aux | grep stream-processor >> diagnostics.txt

   # Logs
   tail -1000 stream-processor/logs/* >> diagnostics.txt

   # Metrics snapshot
   curl http://localhost:8080/metrics >> diagnostics.txt
   ```

3. **Open GitHub issue:**
   - https://github.com/yourusername/streamguard/issues
   - Include diagnostics.txt
   - Describe expected vs actual behavior

4. **Community support:**
   - Slack: streamguard-community.slack.com
   - Discord: discord.gg/streamguard
