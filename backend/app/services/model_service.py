from collections.abc import AsyncIterator, Sequence

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI

from app.core.settings import Settings


class ModelService:
    """
    模型服务层 - 封装 LLM 调用逻辑

    职责：
    1. 管理 LLM 客户端 (ChatOpenAI)
    2. 提供同步/流式对话接口
    3. 支持工具绑定 (Function Calling / Tool Use)
    4. 为 LangGraph 提供底层 model 访问
    """

    def __init__(self, settings: Settings, tools: Sequence[BaseTool] | None = None):
        # 1. 基础模型配置 (无工具绑定)
        self.model = ChatOpenAI(
            api_key=settings.ai_deepseek_api_key or "",
            base_url=settings.ai_deepseek_base_url,
            model=settings.ai_deepseek_model_name,
            temperature=settings.ai_deepseek_temperature,
            timeout=settings.ai_deepseek_timeout,
        )

        # 2. 如果传入了工具列表，创建绑定工具的模型实例
        # bind_tools 会让 LLM 知道可以调用哪些工具，并返回结构化的 tool_calls
        self.tools = tools or []
        if self.tools:
            self.model_with_tools = self.model.bind_tools(self.tools)
        else:
            self.model_with_tools = None

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
        """
        response = await self.model.ainvoke([HumanMessage(content=content)])
        return response.content or ""

    async def chat_with_messages(self, messages: list[BaseMessage]) -> str:
        """
        2. 带上下文的对话，接收完整消息列表。
        """
        response = await self.model.ainvoke(messages)
        return response.content or ""

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
