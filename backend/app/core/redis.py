from collections.abc import AsyncIterator

from redis.asyncio import Redis

from app.core.settings import Settings, get_settings


def create_redis_client(cfg: Settings) -> Redis:
    """
    1. 创建 Redis 异步客户端，统一 decode 响应。
    """
    return Redis(
        host=cfg.redis_host,
        port=cfg.redis_port,
        password=cfg.redis_password,
        db=cfg.redis_db,
        decode_responses=True,
    )


settings = get_settings()
redis_client = create_redis_client(settings)


async def get_redis() -> AsyncIterator[Redis]:
    """
    1. FastAPI 依赖项：获取 Redis 客户端实例。
    """
    yield redis_client
