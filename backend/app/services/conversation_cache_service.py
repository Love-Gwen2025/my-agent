"""
对话缓存服务 - Redis 短期记忆管理
"""

import json
from typing import Any

from redis.asyncio import Redis

from app.core.settings import Settings


class ConversationCacheService:
    """
    1. 缓存最近 N 条对话消息到 Redis
    2. 作为 LLM 的短期上下文记忆
    """

    def __init__(self, redis: Redis, settings: Settings):
        self.redis = redis
        self.ttl = settings.conversation_cache_ttl
        self.max_messages = settings.conversation_cache_max_messages

    def _cache_key(self, conversation_id: int) -> str:
        """生成 Redis key"""
        return f"conv:messages:{conversation_id}"

    async def add_message(
        self,
        conversation_id: int,
        role: str,
        content: str,
        message_id: int | None = None,
    ) -> None:
        """
        添加消息到缓存，保持最近 N 条
        """
        key = self._cache_key(conversation_id)
        message = json.dumps(
            {
                "id": message_id,
                "role": role,
                "content": content,
            },
            ensure_ascii=False,
        )

        # LPUSH + LTRIM 保持固定长度
        await self.redis.lpush(key, message)
        await self.redis.ltrim(key, 0, self.max_messages - 1)
        await self.redis.expire(key, self.ttl)

    async def get_recent_messages(
        self,
        conversation_id: int,
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        """
        获取最近的消息列表 (按时间正序)
        """
        key = self._cache_key(conversation_id)
        count = limit or self.max_messages

        # LRANGE 返回的是倒序 (最新在前)，需要反转
        messages = await self.redis.lrange(key, 0, count - 1)

        result = []
        for msg in reversed(messages):  # 反转为正序
            try:
                result.append(json.loads(msg))
            except json.JSONDecodeError:
                continue

        return result

    async def get_messages_for_llm(
        self,
        conversation_id: int,
        limit: int | None = None,
    ) -> list[dict[str, str]]:
        """
        获取适用于 LLM 的消息格式

        返回: [{"role": "user", "content": "..."}, ...]
        """
        messages = await self.get_recent_messages(conversation_id, limit)
        return [{"role": msg["role"], "content": msg["content"]} for msg in messages]

    async def clear_cache(self, conversation_id: int) -> None:
        """
        清除会话缓存
        """
        key = self._cache_key(conversation_id)
        await self.redis.delete(key)

    async def get_cache_size(self, conversation_id: int) -> int:
        """
        获取缓存的消息数量
        """
        key = self._cache_key(conversation_id)
        return await self.redis.llen(key)
