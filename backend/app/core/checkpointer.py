"""
LangGraph Checkpointer - PostgreSQL 实现

使用 AsyncPostgresSaver 将对话状态持久化到 PostgreSQL。
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

from app.core.settings import Settings

# 标记是否已初始化表结构
_tables_initialized = False


def get_postgres_url(settings: Settings) -> str:
    """构建 PostgreSQL 连接 URL（psycopg 格式）"""
    return (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )


@asynccontextmanager
async def create_checkpointer(settings: Settings) -> AsyncIterator[AsyncPostgresSaver]:
    """
    创建 LangGraph 异步 PostgreSQL checkpointer

    Args:
        settings: 应用配置

    Yields:
        AsyncPostgresSaver 实例
    """
    global _tables_initialized

    postgres_url = get_postgres_url(settings)
    async with AsyncPostgresSaver.from_conn_string(postgres_url) as checkpointer:
        # 首次使用时创建表结构
        if not _tables_initialized:
            await checkpointer.setup()
            _tables_initialized = True
        yield checkpointer


# 兼容旧代码
create_redis_checkpointer = create_checkpointer
