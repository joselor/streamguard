"""
Base LLM Provider Interface

Abstract base class that all LLM providers must implement.
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
import logging

logger = logging.getLogger(__name__)


@dataclass
class LLMResponse:
    """
    Standardized response from LLM providers

    Attributes:
        content: The generated text response
        model: Model name that generated the response
        prompt_tokens: Number of tokens in the prompt
        completion_tokens: Number of tokens in the completion
        total_tokens: Total tokens used
        finish_reason: Reason the generation stopped (e.g., 'stop', 'length')
    """
    content: str
    model: str
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    finish_reason: str = "stop"


class BaseLLMProvider(ABC):
    """
    Abstract base class for LLM providers

    All LLM providers (OpenAI, Ollama, etc.) must implement this interface
    to ensure consistent behavior across different backends.
    """

    def __init__(self, model: str, **kwargs):
        """
        Initialize the LLM provider

        Args:
            model: The model name to use
            **kwargs: Provider-specific configuration
        """
        self.model = model
        self.logger = logging.getLogger(f"{__name__}.{self.__class__.__name__}")

    @abstractmethod
    async def complete(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1000,
        **kwargs
    ) -> LLMResponse:
        """
        Generate a completion from the LLM

        Args:
            messages: List of message dicts with 'role' and 'content' keys
                     Example: [{"role": "system", "content": "..."},
                              {"role": "user", "content": "..."}]
            temperature: Sampling temperature (0.0-2.0, higher = more random)
            max_tokens: Maximum tokens to generate
            **kwargs: Provider-specific options

        Returns:
            LLMResponse with generated content and usage statistics

        Raises:
            Exception: If the LLM call fails
        """
        pass

    @abstractmethod
    async def health_check(self) -> bool:
        """
        Check if the LLM provider is available and healthy

        Returns:
            True if provider is healthy, False otherwise
        """
        pass

    def get_model_name(self) -> str:
        """Get the model name for this provider"""
        return self.model

    def _validate_messages(self, messages: List[Dict[str, str]]) -> None:
        """
        Validate message format

        Args:
            messages: List of message dictionaries to validate

        Raises:
            ValueError: If messages are invalid
        """
        if not messages:
            raise ValueError("Messages list cannot be empty")

        for i, msg in enumerate(messages):
            if not isinstance(msg, dict):
                raise ValueError(f"Message {i} must be a dictionary")

            if 'role' not in msg:
                raise ValueError(f"Message {i} missing 'role' field")

            if 'content' not in msg:
                raise ValueError(f"Message {i} missing 'content' field")

            if msg['role'] not in ['system', 'user', 'assistant']:
                raise ValueError(
                    f"Message {i} has invalid role: {msg['role']}. "
                    "Must be 'system', 'user', or 'assistant'"
                )
