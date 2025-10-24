"""
Prompt engineering for AI Security Assistant

Contains system prompts, few-shot examples, and prompt construction logic.
"""

from typing import List, Dict, Any


SECURITY_ASSISTANT_SYSTEM_PROMPT = """You are an expert security analyst assistant for StreamGuard, a real-time security event monitoring system.

Your role is to help security teams understand threats, anomalies, and patterns in their environment by analyzing security events and providing actionable insights.

## Your Capabilities

You have access to:
1. **Real-time security events** - login attempts, network traffic, system changes
2. **Historical threat intelligence** - known attack patterns, IOCs, MITRE ATT&CK techniques
3. **Anomaly scores** - statistical analysis of unusual behavior
4. **Threat scores** - risk assessments for each event

## Response Guidelines

When answering security queries:

1. **Be Data-Driven**
   - Cite specific events with timestamps and details
   - Reference actual threat scores and anomaly scores
   - Use concrete numbers, not vague statements

2. **Explain WHY**
   - Don't just say something is suspicious
   - Explain what makes it anomalous
   - Connect to known attack patterns

3. **Be Actionable**
   - Provide clear, prioritized recommendations
   - Focus on what to do NOW vs. later
   - Consider operational constraints

4. **Be Honest**
   - If data is insufficient, say so explicitly
   - Don't speculate beyond available evidence
   - Acknowledge uncertainty with confidence scores

5. **Use Security Language**
   - Use proper terminology (IOC, TTP, C2, lateral movement, etc.)
   - Reference frameworks (MITRE ATT&CK, Kill Chain)
   - Be precise about severity levels

## Response Structure

Always structure your answer as:

**Summary** (1-2 sentences)
Brief overview of findings

**Evidence** (bullet points)
- Specific events with timestamps and details
- Relevant threat intelligence matches
- Anomaly scores and statistical context

**Analysis** (2-3 sentences)
Why this matters, what patterns indicate, how it fits known threats

**Recommendations** (prioritized list)
1. Immediate actions (critical)
2. Short-term actions (within 24h)
3. Long-term improvements

## Example Response Style

Good: "User alice had 5 failed login attempts from IP 10.0.1.50 between 14:30-14:45 UTC. This IP is from an unusual geolocation (Russia) compared to her baseline (USA). Anomaly score: 0.87. This pattern matches credential stuffing attacks (MITRE T1110.003)."

Bad: "Some suspicious activity was detected for a user."
"""


def create_query_prompt(
    question: str,
    events: List[Dict[str, Any]],
    threat_intel: List[Dict[str, Any]],
    anomalies: Dict[str, Any]
) -> str:
    """
    Construct the user prompt with all available context

    Args:
        question: Natural language query from user
        events: List of relevant security events
        threat_intel: Matching threat intelligence from RAG
        anomalies: Anomaly detection results

    Returns:
        Formatted prompt string
    """
    return f"""
User Question: {question}

## Available Data

### Recent Security Events
{format_events(events)}

### Threat Intelligence
{format_threat_intel(threat_intel)}

### Anomaly Analysis
{format_anomalies(anomalies)}

## Task

Analyze the above data and answer the user's question following the response structure:
- Summary (brief overview)
- Evidence (cite specific data)
- Analysis (explain significance)
- Recommendations (actionable next steps)

Be precise, data-driven, and actionable. If data is insufficient, state that clearly.
"""


def format_events(events: List[Dict[str, Any]]) -> str:
    """Format security events for prompt"""
    if not events:
        return "No events found in the specified time window."

    formatted = []
    for i, event in enumerate(events[:10], 1):  # Limit to 10 events
        timestamp = event.get('timestamp', 'unknown')
        event_type = event.get('event_type', event.get('type', 'unknown'))
        user = event.get('user', 'unknown')
        severity = event.get('severity', 'MEDIUM')
        threat_score = event.get('threat_score', 0.0)
        source_ip = event.get('source_ip', event.get('sourceIp', 'unknown'))

        formatted.append(
            f"{i}. [{timestamp}] {event_type} - User: {user}, IP: {source_ip}, "
            f"Severity: {severity}, Threat Score: {threat_score:.2f}"
        )

    if len(events) > 10:
        formatted.append(f"\n... and {len(events) - 10} more events")

    return "\n".join(formatted)


def format_threat_intel(intel: List[Dict[str, Any]]) -> str:
    """Format threat intelligence for prompt"""
    if not intel:
        return "No matching threat intelligence found in knowledge base."

    formatted = []
    for i, item in enumerate(intel[:5], 1):  # Limit to 5 items
        source = item.get('source', 'unknown')
        summary = item.get('summary', 'No summary available')
        relevance = item.get('relevance_score', item.get('score', 0.0))

        formatted.append(
            f"{i}. [{source}] (Relevance: {relevance:.2f})\n"
            f"   {summary}"
        )

    return "\n".join(formatted)


def format_anomalies(anomalies: Dict[str, Any]) -> str:
    """Format anomaly detection results for prompt"""
    if not anomalies:
        return "No anomaly analysis available for this query."

    score = anomalies.get('score', anomalies.get('anomaly_score', 0.0))
    deviation = anomalies.get('deviation', 'N/A')
    baseline = anomalies.get('baseline', 'N/A')

    formatted = [
        f"Anomaly Score: {score:.2f} (0.0 = normal, 1.0 = highly anomalous)",
        f"Baseline Deviation: {deviation}",
        f"User Baseline Events: {baseline}"
    ]

    # Add contributing factors if available
    factors = anomalies.get('factors', {})
    if factors:
        formatted.append("\nContributing Factors:")
        for factor, value in factors.items():
            formatted.append(f"  - {factor}: {value}")

    return "\n".join(formatted)


# Few-shot examples for better prompting (optional enhancement)
FEW_SHOT_EXAMPLES = [
    {
        "question": "What happened with user bob in the last hour?",
        "context": "3 failed login attempts from IP 192.168.1.100",
        "good_answer": """
**Summary**: User bob had 3 failed login attempts from IP 192.168.1.100 in the past hour, which is unusual for this user's baseline.

**Evidence**:
- 10:15 UTC: LOGIN_FAILED, IP 192.168.1.100, Threat Score: 0.65
- 10:18 UTC: LOGIN_FAILED, IP 192.168.1.100, Threat Score: 0.72
- 10:21 UTC: LOGIN_FAILED, IP 192.168.1.100, Threat Score: 0.78
- Anomaly Score: 0.74 (baseline deviation: +3.2 sigma)
- IP geolocation: Unknown (not in user's typical locations)

**Analysis**: Multiple failed attempts from same IP in short timeframe suggests either forgotten password or potential brute force attempt. The increasing threat scores indicate system is flagging progressive concern. Geolocation anomaly adds suspicion.

**Recommendations**:
1. IMMEDIATE: Contact user bob to verify legitimate access attempt
2. IMMEDIATE: Block IP 192.168.1.100 if user denies attempts
3. SHORT-TERM: Force password reset for user bob
4. LONG-TERM: Enable MFA for this user account
"""
    }
]
