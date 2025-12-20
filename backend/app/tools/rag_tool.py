"""
RAG 检索工具

用于在历史对话中进行语义检索，找到相关的上下文信息。
作为 LangGraph 工具供模型自主决定是否调用。
"""

import logging
from typing import TYPE_CHECKING

from langchain_core.tools import tool

if TYPE_CHECKING:
    from app.services.embedding_service import EmbeddingService

logger = logging.getLogger(__name__)

# 全局变量，由 ChatService 注入
_embedding_service: "EmbeddingService | None" = None
_db_session = None
_conversation_id: int | None = None


def set_rag_context(
    embedding_service: "EmbeddingService | None",
    db_session,
    conversation_id: int | None,
) -> None:
    """
    设置 RAG 工具的上下文。
    在每次调用图之前由 ChatService 调用。
    """
    global _embedding_service, _db_session, _conversation_id
    _embedding_service = embedding_service
    _db_session = db_session
    _conversation_id = conversation_id


@tool
async def rag_search(query: str, top_k: int = 5) -> str:
    """
    在历史对话中搜索与查询相关的内容。
    
    当用户提到之前讨论过的话题，或者需要查找历史对话信息时使用此工具。
    
    Args:
        query: 搜索查询词，描述你想要查找的内容
        top_k: 返回的最大结果数量，默认5条
    
    Returns:
        相关的历史对话内容，如果没有找到则返回提示信息
    """
    global _embedding_service, _db_session, _conversation_id
    
    if not _embedding_service or not _db_session or not _conversation_id:
        return "RAG 检索服务未配置或当前无法使用。"
    
    try:
        results = await _embedding_service.search_similar(
            db=_db_session,
            query=query,
            conversation_id=_conversation_id,
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
        
        return "相关历史对话:\n" + "\n".join(formatted)
        
    except Exception as e:
        logger.error(f"RAG search failed: {e}")
        return f"搜索时发生错误: {e}"
