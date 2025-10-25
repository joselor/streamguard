"""
OpenAI Provider Implementation

Implements the BaseLLMProvider interface for OpenAI's API.
"""

from typing import List, Dict, Optional
from openai import AsyncOpenAI, OpenAIError
import logging

from app.llm.base import BaseLLMProvider, LLMResponse

logger = logging.getLogger(__name__)


class OpenAIProvider(BaseLLMProvider):
    """
    OpenAI LLM provider implementation

    Uses the official OpenAI Python SDK to communicate with OpenAI's API.
    Supports models like gpt-4o-mini, gpt-4o, gpt-4-turbo, etc.
    """

    def __init__(
        self,
        model: str = "gpt-4o-mini",
        api_key: Optional[str] = None,
        **kwargs
    ):
        """
        Initialize OpenAI provider

        Args:
            model: OpenAI model name (default: gpt-4o-mini)
            api_key: OpenAI API key (required)
            **kwargs: Additional OpenAI client configuration
        """
        super().__init__(model, **kwargs)

        if not api_key:
            raise ValueError("OpenAI API key is required")

        self.client = AsyncOpenAI(api_key=api_key, **kwargs)
        self.logger.info(f"Initialized OpenAI provider with model: {model}")

    async def complete(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1000,
        **kwargs
    ) -> LLMResponse:
        """
        Generate completion using OpenAI API

        Args:
            messages: Chat messages in OpenAI format
            temperature: Sampling temperature (0.0-2.0)
            max_tokens: Maximum tokens to generate
            **kwargs: Additional OpenAI-specific parameters

        Returns:
            LLMResponse with generated content and token usage

        Raises:
            OpenAIError: If the API call fails
        """
        self._validate_messages(messages)

        try:
            self.logger.debug(
                f"Calling OpenAI API: model={self.model}, "
                f"temperature={temperature}, max_tokens={max_tokens}"
            )

            response = await self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                **kwargs
            )

            choice = response.choices[0]
            usage = response.usage

            result = LLMResponse(
                content=choice.message.content,
                model=response.model,
                prompt_tokens=usage.prompt_tokens,
                completion_tokens=usage.completion_tokens,
                total_tokens=usage.total_tokens,
                finish_reason=choice.finish_reason
            )

            self.logger.debug(
                f"OpenAI API call successful: "
                f"tokens={result.total_tokens}, finish={result.finish_reason}"
            )

            return result

        except OpenAIError as e:
            self.logger.error(f"OpenAI API error: {str(e)}", exc_info=True)
            raise

    async def health_check(self) -> bool:
        """
        Check if OpenAI API is accessible

        Returns:
            True if API is accessible, False otherwise
        """
        try:
            # Try a minimal API call to check connectivity
            test_messages = [{"role": "user", "content": "test"}]
            await self.client.chat.completions.create(
                model=self.model,
                messages=test_messages,
                max_tokens=1
            )
            return True
        except Exception as e:
            self.logger.warning(f"OpenAI health check failed: {str(e)}")
            return False
