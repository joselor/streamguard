"""
LLM Provider Factory

Factory class for creating LLM provider instances based on configuration.
"""

from typing import Optional
import logging

from app.llm.base import BaseLLMProvider
from app.llm.openai_provider import OpenAIProvider
from app.llm.ollama_provider import OllamaProvider

logger = logging.getLogger(__name__)


class LLMFactory:
    """
    Factory for creating LLM provider instances

    Supports creating providers based on provider type and configuration.
    """

    @staticmethod
    def create(
        provider_type: str,
        model: Optional[str] = None,
        **config
    ) -> BaseLLMProvider:
        """
        Create an LLM provider instance

        Args:
            provider_type: Type of provider ("openai" or "ollama")
            model: Model name (provider-specific default if not specified)
            **config: Provider-specific configuration

        Returns:
            Initialized LLM provider instance

        Raises:
            ValueError: If provider_type is unknown or required config is missing

        Examples:
            >>> # Create OpenAI provider
            >>> provider = LLMFactory.create(
            ...     provider_type="openai",
            ...     model="gpt-4o-mini",
            ...     api_key="sk-..."
            ... )

            >>> # Create Ollama provider
            >>> provider = LLMFactory.create(
            ...     provider_type="ollama",
            ...     model="llama3.2:latest",
            ...     base_url="http://localhost:11434"
            ... )
        """
        provider_type = provider_type.lower()

        logger.info(
            f"Creating LLM provider: type={provider_type}, model={model}"
        )

        if provider_type == "openai":
            return LLMFactory._create_openai(model, **config)
        elif provider_type == "ollama":
            return LLMFactory._create_ollama(model, **config)
        else:
            raise ValueError(
                f"Unknown provider type: '{provider_type}'. "
                "Supported: 'openai', 'ollama'"
            )

    @staticmethod
    def _create_openai(
        model: Optional[str] = None,
        **config
    ) -> OpenAIProvider:
        """
        Create OpenAI provider

        Args:
            model: OpenAI model name (default: gpt-4o-mini)
            **config: OpenAI configuration (must include 'api_key')

        Returns:
            Initialized OpenAI provider

        Raises:
            ValueError: If api_key is missing
        """
        if "api_key" not in config:
            raise ValueError("OpenAI provider requires 'api_key' in configuration")

        # Default model if not specified
        if model is None:
            model = "gpt-4o-mini"

        logger.info(f"Creating OpenAI provider with model: {model}")

        return OpenAIProvider(
            model=model,
            api_key=config["api_key"],
            **{k: v for k, v in config.items() if k != "api_key"}
        )

    @staticmethod
    def _create_ollama(
        model: Optional[str] = None,
        **config
    ) -> OllamaProvider:
        """
        Create Ollama provider

        Args:
            model: Ollama model name (default: llama3.2:latest)
            **config: Ollama configuration (base_url, timeout, etc.)

        Returns:
            Initialized Ollama provider
        """
        # Default model if not specified
        if model is None:
            model = "llama3.2:latest"

        # Default base URL if not specified
        if "base_url" not in config:
            config["base_url"] = "http://localhost:11434"

        logger.info(
            f"Creating Ollama provider: model={model}, "
            f"base_url={config['base_url']}"
        )

        return OllamaProvider(
            model=model,
            base_url=config.get("base_url", "http://localhost:11434"),
            timeout=config.get("timeout", 120.0),
            **{k: v for k, v in config.items() if k not in ["base_url", "timeout"]}
        )

    @staticmethod
    def create_from_settings(settings) -> BaseLLMProvider:
        """
        Create provider from application settings

        Convenience method for creating provider from pydantic settings object.

        Args:
            settings: Application settings object (from app.config)

        Returns:
            Initialized LLM provider based on settings

        Example:
            >>> from app.config import settings
            >>> provider = LLMFactory.create_from_settings(settings)
        """
        provider_type = settings.llm_provider

        if provider_type == "openai":
            return LLMFactory.create(
                provider_type="openai",
                model=settings.openai_model,
                api_key=settings.openai_api_key
            )
        elif provider_type == "ollama":
            return LLMFactory.create(
                provider_type="ollama",
                model=settings.ollama_model,
                base_url=settings.ollama_base_url,
                timeout=settings.http_timeout
            )
        else:
            raise ValueError(
                f"Unknown LLM provider in settings: {provider_type}"
            )
