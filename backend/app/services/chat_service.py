import json
from collections.abc import AsyncIterator

from app.services.conversation_service import ConversationService
from app.services.model_service import ModelService


class ChatService:
    """
    1. 处理同步与流式对话，当前以占位回显实现，便于前端联调。
    """

    def __init__(self, conversation_service: ConversationService, model_service: ModelService | None = None):
        self.conversation_service = conversation_service
        self.model_service = model_service

    async def chat(self, user_id: int, conversation_id: int, content: str, model_code: str | None) -> tuple[str, int]:
        """
        1. 同步对话：校验会话归属，记录用户消息与助手回复。
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)
        # 2. 记录用户消息
        await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )
        # 3. 调用模型生成回复（若未配置则回显占位）
        if self.model_service:
            reply = await self.model_service.chat(content)
        else:
            reply = f"暂未接入模型，回显: {content}"
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply),
        )
        return reply, assistant_message.id

    async def stream(self, user_id: int, conversation_id: int, content: str, model_code: str | None) -> AsyncIterator[str]:
        """
        1. 流式对话：简单将回复拆分为多段 SSE token。
        """
        reply_full, message_id = await self.chat(user_id, conversation_id, content, model_code)
        tokens = [reply_full[i : i + 10] for i in range(0, len(reply_full), 10)] if not self.model_service else None
        if tokens is not None:
            for token in tokens:
                yield json.dumps(
                    {
                        "type": "chunk",
                        "content": token,
                        "conversationId": conversation_id,
                        "messageId": message_id,
                    },
                    ensure_ascii=False,
                )
        else:
            async for chunk in self.model_service.stream(content):
                yield json.dumps(
                    {
                        "type": "chunk",
                        "content": chunk,
                        "conversationId": conversation_id,
                        "messageId": message_id,
                    },
                    ensure_ascii=False,
                )
        yield json.dumps(
            {
                "type": "done",
                "messageId": message_id,
                "conversationId": conversation_id,
                "tokenCount": len(reply_full),
            },
            ensure_ascii=False,
        )
