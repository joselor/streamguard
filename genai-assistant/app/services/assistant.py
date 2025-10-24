"""
Core AI Security Assistant Logic

Orchestrates data gathering from multiple sources and LLM synthesis.
"""

from openai import AsyncOpenAI
from app.config import settings
from app.services.java_api import JavaAPIClient
from app.services.rag_client import RAGClient
from app.prompts.system_prompts import (
    SECURITY_ASSISTANT_SYSTEM_PROMPT,
    create_query_prompt
)
import time
import logging
import re

logger = logging.getLogger(__name__)


class SecurityAssistant:
    """
    AI Security Assistant orchestrator

    Coordinates:
    1. Data gathering from Java API and RAG service
    2. Prompt construction with context
    3. OpenAI API calls for synthesis
    4. Response parsing and structuring
    """

    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai_api_key)
        self.java_api = JavaAPIClient()
        self.rag_client = RAGClient()

    async def answer_query(
        self,
        question: str,
        context_window: str = "1h",
        user_id: str = None,
        include_threat_intel: bool = True
    ) -> dict:
        """
        Main entry point for answering security queries

        Args:
            question: Natural language security question
            context_window: Time window for query (e.g., "1h", "24h")
            user_id: Optional user ID to scope query
            include_threat_intel: Whether to query RAG service

        Returns:
            Structured response with answer, evidence, and recommendations
        """
        start_time = time.time()
        logger.info(f"Processing query: '{question}' with context: {context_window}")

        try:
            # Phase 1: Gather context from multiple sources (parallel where possible)
            events, threat_intel = await self._gather_context(
                question,
                context_window,
                user_id,
                include_threat_intel
            )

            # Phase 2: Get anomaly information if available
            anomalies = await self._get_anomaly_context(user_id)

            # Phase 3: Build prompt with all context
            prompt = create_query_prompt(question, events, threat_intel, anomalies)

            # Phase 4: Call OpenAI for synthesis
            logger.info("Calling OpenAI API for answer synthesis")
            response = await self.openai_client.chat.completions.create(
                model=settings.openai_model,
                messages=[
                    {"role": "system", "content": SECURITY_ASSISTANT_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=settings.openai_temperature,
                max_tokens=settings.openai_max_tokens
            )

            answer = response.choices[0].message.content
            query_time_ms = int((time.time() - start_time) * 1000)

            # Phase 5: Extract structured information from answer
            recommendations = self._extract_recommendations(answer)
            confidence = self._estimate_confidence(events, threat_intel, anomalies)

            # Phase 6: Build response
            result = {
                "answer": answer,
                "confidence": confidence,
                "supporting_events": self._format_supporting_events(events),
                "threat_intel": self._format_threat_intel(threat_intel),
                "recommended_actions": recommendations,
                "query_time_ms": query_time_ms,
                "sources_used": self._get_sources_used(events, threat_intel)
            }

            logger.info(f"Query completed in {query_time_ms}ms")
            return result

        except Exception as e:
            logger.error(f"Error processing query: {str(e)}", exc_info=True)
            raise

    async def _gather_context(
        self,
        question: str,
        context_window: str,
        user_id: str,
        include_threat_intel: bool
    ) -> tuple:
        """Gather context from Java API and RAG service"""

        # Fetch events
        if user_id:
            events = await self.java_api.get_events_by_user(
                user_id,
                limit=settings.max_supporting_events,
                time_window=context_window
            )
        else:
            events = await self.java_api.get_events(
                limit=settings.max_supporting_events,
                time_window=context_window
            )

        # Fetch threat intelligence (if enabled)
        threat_intel = []
        if include_threat_intel:
            threat_intel = await self.rag_client.query_threat_intel(
                query=question,
                top_k=settings.max_threat_intel_results
            )

        logger.info(f"Gathered {len(events)} events and {len(threat_intel)} threat intel results")
        return events, threat_intel

    async def _get_anomaly_context(self, user_id: str) -> dict:
        """Get anomaly detection information"""
        if not user_id:
            return {}

        try:
            anomalies = await self.java_api.get_anomalies(limit=10, min_score=0.7)
            # Filter to user if available
            user_anomalies = [
                a for a in anomalies
                if a.get('user') == user_id
            ]

            if user_anomalies:
                # Return summary of highest anomaly
                top_anomaly = max(user_anomalies, key=lambda x: x.get('anomaly_score', 0))
                return {
                    "score": top_anomaly.get('anomaly_score', 0),
                    "deviation": top_anomaly.get('deviation', 'N/A'),
                    "baseline": top_anomaly.get('baseline_events', 'N/A')
                }

            return {}

        except Exception as e:
            logger.error(f"Error fetching anomalies: {str(e)}")
            return {}

    def _extract_recommendations(self, answer: str) -> list:
        """
        Extract actionable recommendations from LLM response

        Looks for numbered lists, bullet points, or "Recommendations:" sections
        """
        recommendations = []

        # Try to find "Recommendations" section
        rec_match = re.search(r'\*\*Recommendations\*\*:(.*?)(?:\*\*|$)', answer, re.DOTALL | re.IGNORECASE)
        if rec_match:
            rec_section = rec_match.group(1)
        else:
            rec_section = answer

        # Extract numbered items or bullet points
        patterns = [
            r'^\d+\.\s+(.+)$',  # 1. Item
            r'^[-*]\s+(.+)$',   # - Item or * Item
        ]

        for line in rec_section.split('\n'):
            line = line.strip()
            for pattern in patterns:
                match = re.match(pattern, line)
                if match:
                    recommendation = match.group(1).strip()
                    if len(recommendation) > 10:  # Filter out too-short items
                        recommendations.append(recommendation)
                    break

        # Limit to top 5 recommendations
        return recommendations[:5]

    def _estimate_confidence(self, events: list, threat_intel: list, anomalies: dict) -> float:
        """
        Estimate confidence in answer based on available data

        More data = higher confidence
        """
        confidence = 0.5  # Base confidence

        # Boost for events
        if events:
            confidence += min(0.2, len(events) * 0.02)

        # Boost for threat intel
        if threat_intel:
            avg_relevance = sum(t.get('relevance_score', 0) for t in threat_intel) / len(threat_intel)
            confidence += avg_relevance * 0.2

        # Boost for anomaly data
        if anomalies:
            confidence += 0.1

        return min(0.99, confidence)

    def _format_supporting_events(self, events: list) -> list:
        """Format events for response model"""
        formatted = []
        for event in events[:settings.max_supporting_events]:
            formatted.append({
                "event_id": event.get("eventId", event.get("event_id", "unknown")),
                "timestamp": event.get("timestamp"),
                "event_type": event.get("type", event.get("event_type", "unknown")),
                "severity": event.get("severity", "MEDIUM"),
                "user": event.get("user"),
                "source_ip": event.get("sourceIp", event.get("source_ip")),
                "threat_score": event.get("threatScore", event.get("threat_score", 0.0)),
                "details": event.get("metadata", {})
            })
        return formatted

    def _format_threat_intel(self, threat_intel: list) -> list:
        """Format threat intelligence for response model"""
        return threat_intel[:settings.max_threat_intel_results]

    def _get_sources_used(self, events: list, threat_intel: list) -> list:
        """Determine which sources were successfully queried"""
        sources = ["openai"]  # OpenAI always used

        if events:
            sources.append("java_api")

        if threat_intel:
            sources.append("rag_service")

        return sources
