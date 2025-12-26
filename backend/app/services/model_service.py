from collections.abc import AsyncIterator, Sequence

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from loguru import logger

from app.core.settings import Settings


class ModelService:
    """
    模型服务层 - 封装 LLM 调用逻辑

    职责：
    1. 管理 LLM 客户端 (支持多提供商: DeepSeek / OpenAI / 中转站)
    2. 提供同步/流式对话接口
    3. 支持工具绑定 (Function Calling / Tool Use)
    4. 为 LangGraph 提供底层 model 访问

    提供商选择:
    - deepseek: 使用 DeepSeek API (默认)
    - openai: 使用 OpenAI API
    - custom: 使用中转站 Responses API
    """

    def __init__(self, settings: Settings, tools: Sequence[BaseTool] | None = None):
        """
        初始化模型服务

        根据 settings.ai_provider 配置选择不同的模型提供商:
        - deepseek: 使用 ChatOpenAI 连接 DeepSeek API
        - openai: 使用 ChatOpenAI 连接 OpenAI API
        - custom: 使用 CustomChatModel 连接中转站 Responses API
        """
        self.model = self._create_model(settings)
        logger.info(f"ModelService initialized with provider: {settings.ai_provider}")

        # 如果传入了工具列表，创建绑定工具的模型实例
        # bind_tools 会让 LLM 知道可以调用哪些工具，并返回结构化的 tool_calls
        self.tools = tools or []
        if self.tools:
            self.model_with_tools = self.model.bind_tools(self.tools)
        else:
            self.model_with_tools = None

    def _create_model(self, settings: Settings) -> BaseChatModel:
        """
        根据配置创建对应的模型实例

        Returns:
            BaseChatModel: LangChain 兼容的 ChatModel 实例
        """
        provider = settings.ai_provider.lower()

        if provider == "custom" and settings.ai_custom_api_key:
            # 中转站 API (使用 Responses API 适配器)
            from app.services.custom_model_adapter import CustomChatModel

            logger.info(
                f"Creating CustomChatModel: base_url={settings.ai_custom_base_url}, "
                f"model={settings.ai_custom_model_name}"
            )
            return CustomChatModel(
                api_key=settings.ai_custom_api_key,
                base_url=settings.ai_custom_base_url or "",
                model=settings.ai_custom_model_name,
                temperature=settings.ai_custom_temperature,
            )

        elif provider == "openai" and settings.ai_openai_api_key:
            # OpenAI API
            logger.info(f"Creating ChatOpenAI (OpenAI): model={settings.ai_openai_deployment_name}")
            return ChatOpenAI(
                api_key=settings.ai_openai_api_key,
                base_url=settings.ai_openai_base_url,
                model=settings.ai_openai_deployment_name or "gpt-4",
                temperature=settings.ai_openai_temperature,
                timeout=settings.ai_openai_timeout,
            )

        elif provider == "gemini" and settings.ai_gemini_api_key:
            # Google Gemini API
            from langchain_google_genai import ChatGoogleGenerativeAI

            logger.info(f"Creating ChatGoogleGenerativeAI: model={settings.ai_gemini_model_name}")
            return ChatGoogleGenerativeAI(
                model=settings.ai_gemini_model_name,
                google_api_key=settings.ai_gemini_api_key,
                temperature=settings.ai_gemini_temperature,
            )

        else:
            # 默认: DeepSeek API
            logger.info(f"Creating ChatOpenAI (DeepSeek): model={settings.ai_deepseek_model_name}")
            return ChatOpenAI(
                api_key=settings.ai_deepseek_api_key or "",
                base_url=settings.ai_deepseek_base_url,
                model=settings.ai_deepseek_model_name,
                temperature=settings.ai_deepseek_temperature,
                timeout=settings.ai_deepseek_timeout,
            )

    def get_model(self, with_tools: bool = False) -> ChatOpenAI:
        """
        获取底层模型实例，供 LangGraph 或其他高级用途使用。

        Args:
            with_tools: 是否返回绑定了工具的模型

        Returns:
            ChatOpenAI 实例
        """
        if with_tools and self.model_with_tools:
            return self.model_with_tools
        return self.model

    async def chat(self, content: str) -> str:
        """
        1. 同步式获取完整回复（无上下文）。

        注意: 不同模型返回的 content 格式可能不同:
        - OpenAI/DeepSeek: 字符串
        - Gemini: 列表 [{'type': 'text', 'text': '...', 'index': 0}]
        """
        response = await self.model.ainvoke([HumanMessage(content=content)])
        return self._extract_content(response.content)

    def _extract_content(self, content) -> str:
        """
        从模型响应中提取文本内容

        处理不同模型的返回格式：
        - 字符串: 直接返回
        - 列表[dict]: Gemini 格式，提取 'text' 字段
        - 列表[str]: 拼接返回
        """
        if content is None:
            return ""

        if isinstance(content, str):
            return content

        if isinstance(content, list):
            texts = []
            for part in content:
                if isinstance(part, dict):
                    # Gemini 格式: {'type': 'text', 'text': '...'}
                    if part.get("type") == "text" and "text" in part:
                        texts.append(part["text"])
                elif isinstance(part, str):
                    texts.append(part)
                else:
                    texts.append(str(part))
            return "".join(texts)

        return str(content)

    async def chat_with_messages(self, messages: list[BaseMessage]) -> str:
        """
        2. 带上下文的对话，接收完整消息列表。
        """
        response = await self.model.ainvoke(messages)
        return self._extract_content(response.content)

    async def invoke_with_tools(self, messages: list[BaseMessage]) -> AIMessage:
        """
        3. 带工具调用的对话，返回完整 AIMessage（可能包含 tool_calls）。

        当 LLM 决定调用工具时，返回的 AIMessage 会包含 tool_calls 字段，
        调用方需要执行工具并将结果追加到消息历史中继续对话。

        Returns:
            AIMessage: 包含 content 和可能的 tool_calls
        """
        if self.model_with_tools:
            return await self.model_with_tools.ainvoke(messages)
        else:
            # 没有绑定工具时，使用普通模型
            return await self.model.ainvoke(messages)

    async def stream(self, content: str) -> AsyncIterator[str]:
        """
        4. 流式获取回复分片，逐步 yielding token 内容。
        """
        async for chunk in self.model.astream([HumanMessage(content=content)]):
            if chunk and chunk.content:
                yield str(chunk.content)

    async def stream_with_messages(self, messages: list[BaseMessage]) -> AsyncIterator[str]:
        """
        5. 带上下文的流式对话。
        """
        async for chunk in self.model.astream(messages):
            if chunk and chunk.content:
                yield str(chunk.content)

    async def stream_with_tools(self, messages: list[BaseMessage]) -> AsyncIterator[AIMessage]:
        """
        6. 带工具的流式对话。

        注意: 流式模式下 tool_calls 可能分散在多个 chunk 中，
        通常需要在所有 chunk 接收完毕后才能得到完整的 tool_calls。
        建议在需要 tool_calls 时使用 invoke_with_tools 同步调用。

        Yields:
            AIMessage chunks (每个 chunk 可能包含部分 content 或 tool_calls)
        """
        model = self.model_with_tools if self.model_with_tools else self.model
        async for chunk in model.astream(messages):
            yield chunk
