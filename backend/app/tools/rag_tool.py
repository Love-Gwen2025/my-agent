"""
RAG 检索工具

用于在历史对话中进行语义检索，找到相关的上下文信息。
作为 LangGraph 工具供模型自主决定是否调用。

使用 LangChain 原生的 RunnableConfig 机制传递依赖，避免全局变量。
"""

from typing import TYPE_CHECKING, Annotated

from langchain_core.runnables import RunnableConfig
from langchain_core.tools import tool
from loguru import logger

if TYPE_CHECKING:
    from app.services.embedding_service import EmbeddingService


@tool
async def rag_search(
    query: Annotated[str, "搜索查询词，描述你想要查找的内容"],
    config: RunnableConfig,
    top_k: Annotated[int, "返回的最大结果数量"] = 5,
) -> str:
    """
    在历史对话中搜索与查询相关的内容。
    
    当用户提到之前讨论过的话题，或者需要查找历史对话信息时使用此工具。
    
    Returns:
        相关的历史对话内容，如果没有找到则返回提示信息
    """
    # 从 RunnableConfig 中获取注入的依赖
    configurable = config.get("configurable", {})
    embedding_service: EmbeddingService | None = configurable.get("embedding_service")
    db_session = configurable.get("db_session")
    conversation_id: int | None = configurable.get("conversation_id")

    # 检查依赖是否完整
    if not embedding_service or not db_session or not conversation_id:
        logger.warning("RAG search called without required context")
        return "RAG 检索服务未配置或当前无法使用。"

    try:
        results = await embedding_service.search_similar(
            db=db_session,
            query=query,
            conversation_id=conversation_id,
            top_k=top_k,
            similarity_threshold=0.6,
        )

        if not results:
            return "未找到相关的历史对话。"

        # 格式化结果
        formatted = []
        for i, msg in enumerate(results, 1):
            role = "用户" if msg["role"] == "user" else "助手"
            formatted.append(f"{i}. {role}: {msg['content']}")

        logger.info(f"RAG search found {len(results)} results for query: {query[:50]}...")
        return "相关历史对话:\n" + "\n".join(formatted)

    except Exception as e:
        logger.error(f"RAG search failed: {e}")
        return f"搜索时发生错误: {e}"
