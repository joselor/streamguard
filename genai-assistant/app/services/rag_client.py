"""
Client for StreamGuard RAG Service (FastAPI + ChromaDB on port 8000)

Provides async methods to query threat intelligence knowledge base.
"""

import httpx
from typing import List, Dict, Any
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class RAGClient:
    """
    Async HTTP client for RAG (Retrieval-Augmented Generation) service

    Queries vector database for relevant threat intelligence.
    """

    def __init__(self):
        self.base_url = settings.rag_service_url
        self.timeout = settings.http_timeout
        self.max_results = settings.max_threat_intel_results

    async def query_threat_intel(
        self,
        query: str,
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """
        Query threat intelligence knowledge base

        Args:
            query: Natural language query or event description
            top_k: Number of results to return

        Returns:
            List of relevant threat intelligence matches
        """
        payload = {
            "event_context": query,
            "top_k": min(top_k, self.max_results)
        }

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/rag/query",
                    json=payload
                )
                response.raise_for_status()
                data = response.json()

                # Extract and format results
                results = data.get("results", [])
                return self._format_results(results)

        except httpx.HTTPStatusError as e:
            logger.error(f"RAG service returned error: {e.response.status_code}")
            return []
        except httpx.RequestError as e:
            logger.error(f"Failed to connect to RAG service: {str(e)}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error querying RAG service: {str(e)}")
            return []

    def _format_results(self, results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Format RAG results to match expected schema

        Args:
            results: Raw results from RAG service

        Returns:
            Formatted threat intelligence records
        """
        formatted = []

        for result in results:
            # Handle different response formats
            metadata = result.get("metadata", {})
            document = result.get("document", result.get("text", ""))
            distance = result.get("distance", 1.0)

            # Convert distance to relevance score (lower distance = higher relevance)
            relevance_score = max(0.0, min(1.0, 1.0 - distance))

            formatted.append({
                "source": metadata.get("source", "Unknown"),
                "summary": document[:500] if len(document) > 500 else document,
                "relevance_score": relevance_score,
                "iocs": metadata.get("iocs", []),
                "tactics": metadata.get("tactics", []),
                "techniques": metadata.get("techniques", [])
            })

        return formatted

    async def health_check(self) -> bool:
        """
        Check if RAG service is healthy

        Returns:
            True if service is responding, False otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/health")
                return response.status_code == 200
        except Exception:
            return False
