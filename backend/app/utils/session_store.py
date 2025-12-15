import json
import time

from redis.asyncio import Redis

from app.core.settings import Settings


class SessionStore:
    """
    1. 封装 Redis 会话存取逻辑，保持与 Java 版的键格式一致。
    """

    def __init__(self, redis: Redis, settings: Settings):
        self.redis = redis
        self.settings = settings

    def session_key(self, user_id: str, token: str) -> str:
        """
        1. 按 Java 版的 agent:user:{userId}:session:{token} 生成键。
        """
        return f"agent:user:{{{user_id}}}:session:{token}"

    def index_key(self, user_id: str) -> str:
        """
        1. 生成 ZSet 索引键，存放用户所有会话。
        """
        return f"agent:user:{{{user_id}}}"

    async def save_session(self, payload: dict, ttl_seconds: int) -> None:
        """
        1. 将会话写入 Redis，并同步写入 ZSet 索引。
        """
        user_id = str(payload.get("id"))
        token = payload.get("token", "")
        session_key = self.session_key(user_id, token)
        index_key = self.index_key(user_id)
        session_json = json.dumps(payload, ensure_ascii=False)
        now_ms = int(time.time() * 1000)
        await self.redis.set(session_key, session_json, ex=ttl_seconds)
        await self.redis.zadd(index_key, {session_key: now_ms})
        await self.redis.expire(index_key, ttl_seconds)

    async def load_session(self, user_id: str, token: str) -> dict | None:
        """
        1. 读取并解析会话，不存在返回 None。
        """
        session_key = self.session_key(user_id, token)
        raw = await self.redis.get(session_key)
        if raw is None:
            return None
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return None

    async def remove_session(self, user_id: str, token: str) -> None:
        """
        1. 删除会话并同步索引。
        """
        session_key = self.session_key(user_id, token)
        index_key = self.index_key(user_id)
        await self.redis.delete(session_key)
        await self.redis.zrem(index_key, session_key)
