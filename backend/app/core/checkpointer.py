"""
LangGraph Checkpointer 工厂模块

提供 Redis 状态持久化能力，使用 LangGraph 官方的 RedisSaver。

功能：
1. 自动保存对话状态到 Redis
2. 支持断点续传
3. 按 thread_id (conversation_id) 隔离不同会话
"""

from contextlib import asynccontextmanager
from typing import AsyncIterator

from langgraph.checkpoint.redis import AsyncRedisSaver

from app.core.settings import Settings


def get_redis_url(settings: Settings) -> str:
    """
    根据配置构建 Redis 连接 URL

    注意：LangGraph AsyncRedisSaver 使用 Redis Search 功能，
    而 Redis Search 只能在 db=0 上创建索引，因此这里强制使用 db=0
    """
    if settings.redis_password:
        return f"redis://:{settings.redis_password}@{settings.redis_host}:{settings.redis_port}/0"
    return f"redis://{settings.redis_host}:{settings.redis_port}/0"


@asynccontextmanager
async def create_redis_checkpointer(settings: Settings) -> AsyncIterator[AsyncRedisSaver]:
    """
    创建 LangGraph 异步 Redis checkpointer（上下文管理器版本）

    Args:
        settings: 应用配置

    Yields:
        AsyncRedisSaver 实例，用于传入 graph.compile()

    Usage:
        async with create_redis_checkpointer(settings) as checkpointer:
            graph = create_agent_graph(model, tools, checkpointer)
            await graph.ainvoke(...)
    """
    redis_url = get_redis_url(settings)
    # AsyncRedisSaver.from_conn_string() 返回异步上下文管理器
    async with AsyncRedisSaver.from_conn_string(redis_url) as checkpointer:
        yield checkpointer

