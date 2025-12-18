"""
LangGraph Checkpointer 工厂模块

提供 Redis 状态持久化能力，使用 LangGraph 官方的 RedisSaver。

功能：
1. 自动保存对话状态到 Redis
2. 支持断点续传
3. 按 thread_id (conversation_id) 隔离不同会话
"""

from langgraph.checkpoint.redis import AsyncRedisSaver

from app.core.settings import Settings


def get_redis_url(settings: Settings) -> str:
    """
    根据配置构建 Redis 连接 URL
    """
    if settings.redis_password:
        return f"redis://:{settings.redis_password}@{settings.redis_host}:{settings.redis_port}/{settings.redis_db}"
    return f"redis://{settings.redis_host}:{settings.redis_port}/{settings.redis_db}"


async def create_redis_checkpointer(settings: Settings) -> AsyncRedisSaver:
    """
    创建 LangGraph 异步 Redis checkpointer

    Args:
        settings: 应用配置

    Returns:
        AsyncRedisSaver 实例，用于传入 graph.compile()
    """
    redis_url = get_redis_url(settings)
    # 使用 async context manager 创建
    return AsyncRedisSaver.from_conn_string(redis_url)
