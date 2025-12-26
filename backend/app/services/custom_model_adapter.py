"""
中转站 API 适配器 - 将 OpenAI Responses API 封装为 LangChain 兼容接口

由于中转站只支持 Responses API (stream=True)，不支持标准的 Chat Completions API，
这里创建一个 LangChain 兼容的 ChatModel 适配器。

使用方式:
    from app.services.custom_model_adapter import CustomChatModel

    model = CustomChatModel(
        api_key="your_key",
        base_url="https://ai.love-gwen.top/openai",
        model="gpt-5.1-codex-max",
    )

    # 像普通 LangChain ChatModel 一样使用
    response = await model.ainvoke([HumanMessage(content="你好")])
"""

from collections.abc import AsyncIterator, Iterator, Sequence
from typing import Any

from langchain_core.callbacks import CallbackManagerForLLMRun
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import (
    AIMessage,
    AIMessageChunk,
    BaseMessage,
    HumanMessage,
    SystemMessage,
)
from langchain_core.outputs import ChatGeneration, ChatGenerationChunk, ChatResult
from openai import OpenAI


class CustomChatModel(BaseChatModel):
    """
    自定义 ChatModel 适配器

    将中转站的 Responses API 封装为 LangChain 兼容的 ChatModel。
    支持同步/异步调用和流式输出。
    """

    # 配置参数
    api_key: str
    base_url: str
    model: str = "gpt-5.1-codex-max"
    temperature: float = 0.7
    max_tokens: int | None = None
    default_headers: dict[str, str] | None = None

    # 内部客户端（延迟初始化）
    _client: OpenAI | None = None

    class Config:
        """Pydantic 配置"""

        arbitrary_types_allowed = True

    @property
    def _llm_type(self) -> str:
        """返回 LLM 类型标识"""
        return "custom-responses-api"

    @property
    def client(self) -> OpenAI:
        """获取或创建 OpenAI 客户端"""
        if self._client is None:
            headers = self.default_headers or {
                "User-Agent": "OpenAI-Codex/0.77.0",
                "X-Client-Name": "codex-cli",
                "X-Client-Version": "0.77.0",
            }
            self._client = OpenAI(
                api_key=self.api_key,
                base_url=self.base_url,
                default_headers=headers,
            )
        return self._client

    def _convert_messages_to_input(self, messages: list[BaseMessage]) -> list[dict[str, str]]:
        """
        将 LangChain 消息格式转换为 Responses API 的 input 格式

        LangChain: [HumanMessage(content="..."), AIMessage(content="...")]
        Responses API: [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
        """
        result = []
        for msg in messages:
            if isinstance(msg, SystemMessage):
                # Responses API 可能使用 "system" 或 "developer" 角色
                result.append({"role": "system", "content": str(msg.content)})
            elif isinstance(msg, HumanMessage):
                result.append({"role": "user", "content": str(msg.content)})
            elif isinstance(msg, AIMessage):
                result.append({"role": "assistant", "content": str(msg.content)})
            else:
                # 未知类型作为 user 处理
                result.append({"role": "user", "content": str(msg.content)})
        return result

    def _generate(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: CallbackManagerForLLMRun | None = None,
        **kwargs: Any,
    ) -> ChatResult:
        """
        同步生成回复 (使用流式 API 收集完整响应)

        注意: Responses API 必须用流式模式，这里收集所有 chunk 后返回完整结果
        """
        input_messages = self._convert_messages_to_input(messages)

        # 调用流式 Responses API
        response = self.client.responses.create(
            model=self.model,
            input=input_messages,
            stream=True,  # 必须开启流式！
        )

        # 收集所有输出文本
        full_text = []
        for event in response:
            if hasattr(event, "delta") and event.delta:
                full_text.append(event.delta)
            elif hasattr(event, "output_text") and event.output_text:
                full_text.append(event.output_text)

        content = "".join(full_text)
        message = AIMessage(content=content)
        generation = ChatGeneration(message=message)

        return ChatResult(generations=[generation])

    def _stream(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: CallbackManagerForLLMRun | None = None,
        **kwargs: Any,
    ) -> Iterator[ChatGenerationChunk]:
        """
        同步流式生成回复
        """
        input_messages = self._convert_messages_to_input(messages)

        response = self.client.responses.create(
            model=self.model,
            input=input_messages,
            stream=True,
        )

        for event in response:
            text = ""
            if hasattr(event, "delta") and event.delta:
                text = event.delta
            elif hasattr(event, "output_text") and event.output_text:
                text = event.output_text

            if text:
                chunk = ChatGenerationChunk(message=AIMessageChunk(content=text))
                if run_manager:
                    run_manager.on_llm_new_token(text)
                yield chunk

    async def _agenerate(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: CallbackManagerForLLMRun | None = None,
        **kwargs: Any,
    ) -> ChatResult:
        """
        异步生成回复

        注意: OpenAI SDK 的 responses.create 目前没有原生异步版本，
        这里使用 asyncio.to_thread 包装同步调用
        """
        import asyncio

        return await asyncio.to_thread(self._generate, messages, stop, run_manager, **kwargs)

    async def _astream(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager: CallbackManagerForLLMRun | None = None,
        **kwargs: Any,
    ) -> AsyncIterator[ChatGenerationChunk]:
        """
        异步流式生成回复

        使用 asyncio 包装同步流式迭代器
        """
        import asyncio

        input_messages = self._convert_messages_to_input(messages)

        # 在线程中运行同步 API 调用
        def create_stream():
            return self.client.responses.create(
                model=self.model,
                input=input_messages,
                stream=True,
            )

        response = await asyncio.to_thread(create_stream)

        # 异步迭代流式响应
        for event in response:
            text = ""
            if hasattr(event, "delta") and event.delta:
                text = event.delta
            elif hasattr(event, "output_text") and event.output_text:
                text = event.output_text

            if text:
                chunk = ChatGenerationChunk(message=AIMessageChunk(content=text))
                yield chunk
                # 给其他协程执行机会
                await asyncio.sleep(0)

    def bind_tools(
        self,
        tools: Sequence[Any],
        **kwargs: Any,
    ) -> "CustomChatModel":
        """
        绑定工具 (工具调用)

        注意: Responses API 的工具调用格式可能与 Chat Completions API 不同，
        需要根据实际 API 文档进行适配。目前返回 self，暂不支持工具调用。
        """
        # TODO: 根据中转站 Responses API 的工具调用格式实现
        # 目前先返回自身，工具调用功能待后续实现
        return self
