from datetime import datetime

from sqlalchemy import delete, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.conversation import Conversation
from app.models.message import Message


class ConversationService:
    """
    1. 处理会话与消息的增删查。
    """

    def __init__(self, db: AsyncSession):
        self.db = db

    async def ensure_owner(self, conversation_id: int, user_id: int) -> Conversation:
        """
        1. 校验会话存在且属于当前用户。
        """
        # 1. 查询会话并校验归属
        query = await self.db.execute(
            select(Conversation).where(Conversation.id == conversation_id, Conversation.user_id == user_id)
        )
        conversation = query.scalar_one_or_none()
        if conversation is None:
            raise PermissionError("会话不存在或无权限")
        return conversation

    async def create_conversation(self, user_id: int, title: str | None, model_code: str | None) -> int:
        """
        1. 创建机器人会话，标题为空时给默认值。
        """
        # 1. 构造实体并写入
        conversation = Conversation(
            user_id=user_id,
            title=title if title else "与聊天助手的会话",
            model_code=model_code,
        )
        self.db.add(conversation)
        await self.db.commit()
        await self.db.refresh(conversation)
        return conversation.id

    async def list_conversations(self, user_id: int) -> list[dict]:
        """
        1. 查询当前用户会话列表，按更新时间降序。
        """
        # 1. 查询列表并转换视图
        query = await self.db.execute(
            select(Conversation).where(Conversation.user_id == user_id).order_by(Conversation.update_time.desc())
        )
        items = query.scalars().all()
        return [item.to_vo() for item in items]

    async def get_conversation(self, conversation_id: int, user_id: int) -> dict:
        """
        1. 获取会话详情，校验归属。
        """
        # 1. 复用归属校验
        conversation = await self.ensure_owner(conversation_id, user_id)
        return conversation.to_vo()

    async def modify_conversation(self, user_id: int, conversation_id: int, title: str | None) -> None:
        """
        1. 修改会话标题。
        """
        # 1. 校验归属后更新
        await self.ensure_owner(conversation_id, user_id)
        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(title=title, update_time=datetime.now())
        )
        await self.db.commit()

    async def delete_conversation(self, user_id: int, conversation_id: int) -> None:
        """
        1. 删除会话与其消息。
        """
        # 1. 校验归属并删除消息与会话
        await self.ensure_owner(conversation_id, user_id)
        await self.db.execute(delete(Message).where(Message.conversation_id == conversation_id))
        await self.db.execute(delete(Conversation).where(Conversation.id == conversation_id))
        await self.db.commit()

    async def history(self, user_id: int, conversation_id: int) -> list[dict]:
        """
        1. 查询会话消息历史，按创建时间升序。
        """
        # 1. 校验归属并查询消息
        await self.ensure_owner(conversation_id, user_id)
        query = await self.db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.create_time.asc(), Message.id.asc())
        )
        messages = query.scalars().all()
        return [msg.to_vo() for msg in messages]

    async def persist_message(
        self,
        conversation_id: int,
        sender_id: int,
        role: str,
        content: str,
        content_type: str = "TEXT",
        model_code: str | None = None,
        token_count: int = 0,
    ) -> Message:
        """
        1. 写入消息记录。
        """
        # 1. 写入消息并更新会话最近消息
        message = Message(
            conversation_id=conversation_id,
            sender_id=sender_id,
            role=role,
            content=content,
            content_type=content_type,
            model_code=model_code,
            status=1,
            token_count=token_count,
        )
        self.db.add(message)
        await self.db.commit()
        await self.db.refresh(message)
        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(last_message_id=message.id, last_message_at=message.create_time)
        )
        await self.db.commit()
        return message
