"""
Client for direct RocksDB queries (Optional)

Currently not implemented - all data accessed via Java API.
Future enhancement: Direct RocksDB connections for performance.
"""

from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class RocksDBClient:
    """
    Direct RocksDB client (placeholder)

    Note: Currently not implemented. All queries go through Java API.
    This is a future optimization for direct database access.
    """

    def __init__(self):
        logger.info("RocksDB direct client not implemented - using Java API instead")

    async def get_user_baseline(self, user_id: str) -> Dict[str, Any]:
        """Get user baseline statistics (not implemented)"""
        logger.warning("Direct RocksDB access not implemented")
        return {}

    async def get_aggregated_stats(self) -> Dict[str, Any]:
        """Get aggregated statistics (not implemented)"""
        logger.warning("Direct RocksDB access not implemented")
        return {}
