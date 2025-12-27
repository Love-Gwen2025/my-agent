"""
LangGraph Checkpointer - PostgreSQL 实现

使用 AsyncPostgresSaver 将对话状态持久化到 PostgreSQL。
使用连接池避免每次请求都创建新连接。
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from psycopg_pool import AsyncConnectionPool

from app.core.settings import Settings

# 全局连接池
_pool: AsyncConnectionPool | None = None
_tables_initialized = False


def get_postgres_url(settings: Settings) -> str:
    """构建 PostgreSQL 连接 URL（psycopg 格式）"""
    return (
        f"postgresql://{settings.db_user}:{settings.db_password}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    )


async def init_checkpointer_pool(settings: Settings) -> None:
    """初始化全局连接池（应用启动时调用）"""
    global _pool, _tables_initialized
    if _pool is None:
        _pool = AsyncConnectionPool(
            conninfo=get_postgres_url(settings),
            min_size=2,
            max_size=10,
            open=False,
        )
        await _pool.open()
        # 初始化表结构
        async with _pool.connection() as conn:
            checkpointer = AsyncPostgresSaver(conn)
            await checkpointer.setup()
        _tables_initialized = True


async def close_checkpointer_pool() -> None:
    """关闭连接池（应用关闭时调用）"""
    global _pool
    if _pool:
        await _pool.close()
        _pool = None


@asynccontextmanager
async def create_checkpointer(settings: Settings) -> AsyncIterator[AsyncPostgresSaver]:
    """
    创建 LangGraph 异步 PostgreSQL checkpointer（复用连接池）
    """
    global _pool, _tables_initialized

    if _pool is None:
        await init_checkpointer_pool(settings)

    async with _pool.connection() as conn:
        yield AsyncPostgresSaver(conn)


# 兼容旧代码
create_redis_checkpointer = create_checkpointer
