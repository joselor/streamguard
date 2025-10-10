# AI-Powered Threat Analysis

## Overview

StreamGuard integrates OpenAI GPT-4o-mini to provide natural language threat analysis for high-risk security events. This feature provides security analysts with actionable insights and recommendations based on MITRE ATT&CK framework tactics.

## Features

- **Automated threat classification** - Identifies attack types (Brute Force, DDoS, Malware, etc.)
- **Natural language explanations** - 2-3 sentence descriptions of the threat in plain English
- **Actionable recommendations** - Specific steps to mitigate the threat
- **Confidence scoring** - AI confidence level (0.0-1.0) for each analysis
- **Severity mapping** - Maps threat scores to severity levels (LOW/MEDIUM/HIGH/CRITICAL)
- **Cost-effective** - Uses GPT-4o-mini for optimal cost-performance balance

## Configuration

### Enable AI Analysis

Pass your OpenAI API key via command-line:

```bash
./stream-processor \
  --broker localhost:9092 \
  --topic security-events \
  --group my-consumer \
  --openai-key "sk-..."
```

### Behavior

- **Without API key**: AI analysis is disabled, processor runs normally
- **With API key**: AI analysis is enabled for events with `threat_score > 0.7`

## Analysis Trigger Criteria

AI analysis is performed automatically for events meeting:
- **Threat score > 0.7** (medium, high, or critical threats)
- **All event types** (auth, network, file, process, DNS)

## Output Format

When a high-threat event is detected, you'll see:

```
[AI Analysis] Event evt_abc123
  Attack Type: Brute Force Authentication Attack
  Severity: HIGH
  Description: This appears to be a credential stuffing attack targeting the authentication system from IP 192.168.1.100. Multiple failed login attempts indicate an automated attack tool.
  Confidence: 0.85
  Recommendations:
    - Implement rate limiting on authentication endpoints
    - Enable account lockout after 5 failed attempts
    - Review and block source IP 192.168.1.100 at firewall
    - Enable MFA for all user accounts
    - Monitor for lateral movement attempts
```

## MITRE ATT&CK Integration

The AI analyzer is prompted to consider MITRE ATT&CK tactics and techniques:

- **TA0001** - Initial Access
- **TA0006** - Credential Access
- **TA0010** - Exfiltration
- **TA0040** - Impact
- **TA0007** - Discovery

## Technical Details

### Implementation

- **Model**: `gpt-4o-mini` (OpenAI)
- **Temperature**: 0.3 (focused, deterministic responses)
- **Max tokens**: 500 (concise analysis)
- **Timeout**: 30 seconds
- **HTTP client**: libcurl

### Prompt Engineering

The system sends contextual event details to the LLM:
- Event ID
- Event type (auth_attempt, network_connection, etc.)
- User
- Source IP / Destination IP
- File paths, domains, processes (when applicable)
- Threat score (0.0-1.0)
- Timestamp

### Error Handling

- Graceful degradation: Processor continues if AI fails
- Statistics tracking: `getSuccessCount()` and `getErrorCount()`
- Detailed logging for troubleshooting

## Cost Optimization

GPT-4o-mini pricing (as of 2024):
- **Input**: $0.15 / 1M tokens
- **Output**: $0.60 / 1M tokens

Estimated cost per analysis:
- Input: ~200 tokens ($0.00003)
- Output: ~150 tokens ($0.00009)
- **Total: ~$0.00012 per event**

For 10,000 high-threat events/day:
- **Daily cost: ~$1.20**
- **Monthly cost: ~$36**

## Future Enhancements (Sprint 2)

- **US-214**: Store AI analysis in RocksDB
- **US-211**: Generate vector embeddings for similarity search
- **US-212**: RAG-based threat intelligence (historical context)
- **US-213**: Natural language query interface for analysis

## Example Use Cases

1. **Brute Force Detection**: Identify credential stuffing attacks
2. **Lateral Movement**: Detect privilege escalation patterns
3. **Data Exfiltration**: Recognize abnormal file access or network transfers
4. **Malware Execution**: Analyze suspicious process executions
5. **DNS Tunneling**: Identify covert channels via DNS queries

## Integration with Metrics

AI analysis statistics are available via Prometheus:
- `streamguard_ai_analysis_total` - Total analyses performed
- `streamguard_ai_success_total` - Successful analyses
- `streamguard_ai_errors_total` - Failed analyses
- `streamguard_ai_latency_seconds` - Analysis latency

## Security Considerations

- **API Key Storage**: Pass via command-line or environment variable (never commit to git)
- **Data Privacy**: Event data is sent to OpenAI (review their data policy)
- **Rate Limiting**: OpenAI enforces rate limits (tier-based)
- **Fallback**: System continues without AI if unavailable

## Troubleshooting

### AI not analyzing events

Check:
1. API key is provided via `--openai-key`
2. Events have `threat_score > 0.7`
3. Network connectivity to `api.openai.com`
4. API key has credits/quota remaining

### High latency

- Typical latency: 500-2000ms per analysis
- Check network connectivity
- Consider caching common attack patterns (future enhancement)

### Parse errors

- AI response format issues are logged to stderr
- System gracefully continues processing events
- Check `streamguard_ai_errors_total` metric

## References

- [OpenAI GPT-4o-mini Documentation](https://platform.openai.com/docs/models/gpt-4o-mini)
- [MITRE ATT&CK Framework](https://attack.mitre.org/)
- [OpenAI Pricing](https://openai.com/api/pricing/)
