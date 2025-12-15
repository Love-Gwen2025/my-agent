from collections.abc import AsyncIterator

from langchain_core.messages import HumanMessage
from langchain_openai import ChatOpenAI

from app.core.settings import Settings


class ModelService:
    """
    1. 封装 DeepSeek 聊天模型的调用，基于 LangChain OpenAI 接口适配。
    """

    def __init__(self, settings: Settings):
        # 1. 使用 OpenAI 兼容的 DeepSeek 端点与模型配置
        self.model = ChatOpenAI(
            api_key=settings.ai_deepseek_api_key or "",
            base_url=settings.ai_deepseek_base_url,
            model=settings.ai_deepseek_model_name,
            temperature=settings.ai_deepseek_temperature,
            timeout=settings.ai_deepseek_timeout,
        )

    async def chat(self, content: str) -> str:
        """
        1. 同步式获取完整回复。
        """
        response = await self.model.ainvoke([HumanMessage(content=content)])
        return response.content or ""

    async def stream(self, content: str) -> AsyncIterator[str]:
        """
        1. 流式获取回复分片，逐步 yielding token 内容。
        """
        async for chunk in self.model.astream([HumanMessage(content=content)]):
            if chunk and chunk.content:
                yield str(chunk.content)
