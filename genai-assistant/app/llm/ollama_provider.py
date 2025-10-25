"""
Ollama Provider Implementation

Implements the BaseLLMProvider interface for Ollama's local LLM API.
"""

from typing import List, Dict, Optional
import httpx
import logging

from app.llm.base import BaseLLMProvider, LLMResponse

logger = logging.getLogger(__name__)


class OllamaProvider(BaseLLMProvider):
    """
    Ollama LLM provider implementation

    Communicates with a local Ollama server via HTTP API.
    Supports models like llama3.2, mistral, deepseek-r1, phi3, etc.
    """

    def __init__(
        self,
        model: str = "llama3.2:latest",
        base_url: str = "http://localhost:11434",
        timeout: float = 120.0,
        **kwargs
    ):
        """
        Initialize Ollama provider

        Args:
            model: Ollama model name (e.g., "llama3.2:latest", "mistral:7b")
            base_url: Ollama API base URL (default: http://localhost:11434)
            timeout: Request timeout in seconds (default: 120s for local inference)
            **kwargs: Additional configuration
        """
        super().__init__(model, **kwargs)

        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.logger.info(
            f"Initialized Ollama provider: model={model}, base_url={base_url}"
        )

    async def complete(
        self,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: int = 1000,
        **kwargs
    ) -> LLMResponse:
        """
        Generate completion using Ollama API

        Args:
            messages: Chat messages in standard format
            temperature: Sampling temperature (0.0-2.0)
            max_tokens: Maximum tokens to generate (Ollama calls this num_predict)
            **kwargs: Additional Ollama-specific options

        Returns:
            LLMResponse with generated content and token usage

        Raises:
            httpx.HTTPError: If the API call fails
        """
        self._validate_messages(messages)

        try:
            self.logger.debug(
                f"Calling Ollama API: model={self.model}, "
                f"temperature={temperature}, max_tokens={max_tokens}"
            )

            # Ollama API payload
            payload = {
                "model": self.model,
                "messages": messages,
                "stream": False,  # Get complete response at once
                "options": {
                    "temperature": temperature,
                    "num_predict": max_tokens,
                    **kwargs.get("options", {})
                }
            }

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/chat",
                    json=payload
                )
                response.raise_for_status()
                data = response.json()

            # Extract response data
            content = data.get("message", {}).get("content", "")

            # Ollama provides token counts in the response
            prompt_tokens = data.get("prompt_eval_count", 0)
            completion_tokens = data.get("eval_count", 0)
            total_tokens = prompt_tokens + completion_tokens

            result = LLMResponse(
                content=content,
                model=self.model,
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
                total_tokens=total_tokens,
                finish_reason=data.get("done_reason", "stop")
            )

            self.logger.debug(
                f"Ollama API call successful: "
                f"tokens={result.total_tokens}, finish={result.finish_reason}"
            )

            return result

        except httpx.HTTPError as e:
            self.logger.error(f"Ollama API error: {str(e)}", exc_info=True)
            raise
        except Exception as e:
            self.logger.error(f"Unexpected error calling Ollama: {str(e)}", exc_info=True)
            raise

    async def health_check(self) -> bool:
        """
        Check if Ollama server is accessible and the model is available

        Returns:
            True if server is accessible and model exists, False otherwise
        """
        try:
            # Check if Ollama server is running
            async with httpx.AsyncClient(timeout=5.0) as client:
                # List available models
                response = await client.get(f"{self.base_url}/api/tags")
                response.raise_for_status()
                data = response.json()

                # Check if our model is in the list
                models = data.get("models", [])
                model_names = [m.get("name", "") for m in models]

                # Support both "llama3.2:latest" and "llama3.2" formats
                model_base = self.model.split(":")[0]
                model_available = any(
                    name == self.model or name.startswith(f"{model_base}:")
                    for name in model_names
                )

                if not model_available:
                    self.logger.warning(
                        f"Model '{self.model}' not found. Available models: {model_names}"
                    )
                    return False

                self.logger.debug(f"Ollama health check passed: model '{self.model}' available")
                return True

        except Exception as e:
            self.logger.warning(f"Ollama health check failed: {str(e)}")
            return False

    def get_available_models(self) -> List[str]:
        """
        Get list of available models from Ollama server

        Returns:
            List of model names, or empty list if server unreachable

        Note: This is a synchronous helper method for setup/debugging
        """
        try:
            import httpx
            with httpx.Client(timeout=5.0) as client:
                response = client.get(f"{self.base_url}/api/tags")
                response.raise_for_status()
                data = response.json()
                return [m.get("name", "") for m in data.get("models", [])]
        except Exception as e:
            self.logger.error(f"Failed to get Ollama models: {str(e)}")
            return []
