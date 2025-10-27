"""
Client for StreamGuard Java API (Spring Boot on port 8081)

Provides async methods to query events, anomalies, and threat analyses.
"""

import httpx
from typing import List, Optional, Dict, Any
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class JavaAPIClient:
    """
    Async HTTP client for StreamGuard Java API

    Handles connection pooling, retries, and error handling.
    """

    def __init__(self):
        self.base_url = settings.java_api_url
        self.timeout = settings.http_timeout
        self.max_retries = settings.max_retries

    async def get_events(
        self,
        limit: int = 100,
        time_window: Optional[str] = None,
        event_type: Optional[str] = None,
        min_threat_score: Optional[float] = None
    ) -> List[Dict[str, Any]]:
        """
        Fetch recent security events

        Args:
            limit: Maximum number of events to return
            time_window: Time window (e.g., "1h", "24h", "7d")
            event_type: Filter by event type (e.g., "LOGIN_FAILED")
            min_threat_score: Minimum threat score (0.0-1.0)

        Returns:
            List of security events
        """
        params = {"limit": limit}
        if time_window:
            params["window"] = time_window
        if event_type:
            params["type"] = event_type
        if min_threat_score is not None:
            params["min_score"] = min_threat_score

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/events",
                    params=params
                )
                response.raise_for_status()
                return response.json()

        except httpx.HTTPStatusError as e:
            logger.error(f"Java API returned error: {e.response.status_code}")
            return []
        except httpx.RequestError as e:
            logger.error(f"Failed to connect to Java API: {str(e)}")
            return []
        except Exception as e:
            logger.error(f"Unexpected error querying Java API: {str(e)}")
            return []

    async def get_events_by_user(
        self,
        user_id: str,
        limit: int = 100,
        time_window: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Fetch events for a specific user (client-side filtering)

        Note: Java API doesn't support server-side user filtering yet,
        so we fetch recent events and filter client-side.

        Args:
            user_id: User identifier
            limit: Maximum number of events (applied after filtering)
            time_window: Time window filter

        Returns:
            List of user's security events
        """
        logger.debug(f"Fetching events for user: {user_id}")

        # Fetch more events than needed since we're filtering client-side
        fetch_limit = min(limit * 5, 500)  # Fetch 5x to ensure enough results after filtering

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                params = {"limit": fetch_limit}
                response = await client.get(
                    f"{self.base_url}/api/events/recent",
                    params=params
                )
                response.raise_for_status()
                all_events = response.json()

                # Client-side filtering by user
                user_events = [
                    event for event in all_events
                    if event.get('user') == user_id
                ]

                logger.info(f"Filtered {len(user_events)} events for user '{user_id}' from {len(all_events)} total events")

                # Return only requested number of events
                return user_events[:limit]

        except httpx.HTTPStatusError as e:
            logger.error(f"Java API returned error for user {user_id}: {e.response.status_code}")
            return []
        except Exception as e:
            logger.error(f"Error fetching events for user {user_id}: {str(e)}")
            return []

    async def get_anomalies(
        self,
        limit: int = 50,
        min_score: float = 0.7
    ) -> List[Dict[str, Any]]:
        """
        Fetch recent anomalies

        Args:
            limit: Maximum number of anomalies
            min_score: Minimum anomaly score threshold

        Returns:
            List of anomaly records
        """
        params = {
            "limit": limit,
            "min_score": min_score
        }

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/anomalies/high-score",
                    params=params
                )
                response.raise_for_status()
                return response.json()

        except Exception as e:
            logger.error(f"Error fetching anomalies: {str(e)}")
            return []

    async def get_ai_analyses(
        self,
        severity: Optional[str] = None,
        limit: int = 50
    ) -> List[Dict[str, Any]]:
        """
        Fetch AI threat analyses

        Args:
            severity: Filter by severity (CRITICAL, HIGH, MEDIUM, LOW)
            limit: Maximum number of analyses

        Returns:
            List of AI threat analyses
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                if severity:
                    url = f"{self.base_url}/api/analyses/severity/{severity}"
                else:
                    url = f"{self.base_url}/api/analyses/recent"

                response = await client.get(url, params={"limit": limit})
                response.raise_for_status()
                return response.json()

        except Exception as e:
            logger.error(f"Error fetching AI analyses: {str(e)}")
            return []

    async def get_stats_summary(self) -> Dict[str, Any]:
        """
        Fetch aggregated statistics

        Returns:
            Statistics summary (event counts, anomaly counts, etc.)
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(f"{self.base_url}/api/stats/summary")
                response.raise_for_status()
                return response.json()

        except Exception as e:
            logger.error(f"Error fetching stats: {str(e)}")
            return {}

    async def health_check(self) -> bool:
        """
        Check if Java API is healthy

        Returns:
            True if API is responding, False otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/actuator/health")
                return response.status_code == 200
        except Exception:
            return False

    async def get_batch_anomaly(self, user_id: str) -> Optional[Dict[str, Any]]:
        """
        Fetch batch ML anomaly detection for user (Sprint 12)

        Args:
            user_id: User ID to lookup

        Returns:
            Batch anomaly data if user flagged, None otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/training-data/anomalies/{user_id}"
                )

                if response.status_code == 200:
                    logger.info(f"Batch anomaly found for user: {user_id}")
                    return response.json()
                elif response.status_code == 404:
                    logger.debug(f"No batch anomaly for user: {user_id}")
                    return None
                else:
                    logger.warning(f"Unexpected status fetching batch anomaly: {response.status_code}")
                    return None

        except Exception as e:
            logger.warning(f"Batch anomaly fetch failed for {user_id}: {e}")
            return None

    async def get_batch_anomaly_report(self) -> Optional[Dict[str, Any]]:
        """
        Fetch batch ML anomaly detection summary report (Sprint 12)

        Returns:
            Anomaly report with total users and top anomalies
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/training-data/report"
                )

                if response.status_code == 200:
                    logger.debug("Batch anomaly report fetched successfully")
                    return response.json()
                else:
                    logger.warning(f"Anomaly report not available: {response.status_code}")
                    return None

        except Exception as e:
            logger.warning(f"Anomaly report fetch failed: {e}")
            return None

    async def check_training_data_health(self) -> bool:
        """
        Check if batch training data is available (Sprint 12)

        Returns:
            True if training data is available, False otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/api/training-data/health")
                return response.status_code == 200
        except Exception:
            return False
