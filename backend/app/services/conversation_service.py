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
            select(Conversation).where(
                Conversation.id == conversation_id, Conversation.user_id == user_id
            )
        )
        conversation = query.scalar_one_or_none()
        if conversation is None:
            raise PermissionError("会话不存在或无权限")
        return conversation

    async def create_conversation(
        self, user_id: int, title: str | None, model_code: str | None
    ) -> int:
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
            select(Conversation)
            .where(Conversation.user_id == user_id)
            .order_by(Conversation.update_time.desc())
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

    async def modify_conversation(
        self, user_id: int, conversation_id: int, title: str | None
    ) -> None:
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

    async def history(self, user_id: int, conversation_id: int) -> dict:
        """
        返回完整消息树，让前端处理分支逻辑。

        返回:
            {
                "messages": [...],  # 所有消息
                "currentMessageId": "..."  # 当前选中的消息 ID
            }
        """
        # 1. 校验归属
        conversation = await self.ensure_owner(conversation_id, user_id)
        
        # 2. 查询所有消息
        query = await self.db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.create_time.asc(), Message.id.asc())
        )
        all_messages = query.scalars().all()

        # 3. 返回完整消息列表和当前选中消息ID
        return {
            "messages": [msg.to_vo() for msg in all_messages],
            "currentMessageId": str(conversation.current_message_id) if conversation.current_message_id else None
        }

    async def persist_message(
        self,
        conversation_id: int,
        sender_id: int,
        role: str,
        content: str,
        content_type: str = "TEXT",
        model_code: str | None = None,
        token_count: int = 0,
        parent_id: int | None = None,
        checkpoint_id: str | None = None,
    ) -> Message:
        """
        写入消息记录，支持消息树结构。
        
        Args:
            parent_id: 父消息 ID，用于构建分支
            checkpoint_id: 关联的 LangGraph checkpoint ID
        """
        message = Message(
            conversation_id=conversation_id,
            sender_id=sender_id,
            role=role,
            content=content,
            content_type=content_type,
            model_code=model_code,
            status=1,
            token_count=token_count,
            parent_id=parent_id,
            checkpoint_id=checkpoint_id,
        )
        self.db.add(message)
        await self.db.commit()
        await self.db.refresh(message)
        # 同时更新 last_message_id 和 current_message_id
        # current_message_id 用于分支切换后恢复位置
        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(
                last_message_id=message.id,
                last_message_at=message.create_time,
                current_message_id=message.id
            )
        )
        await self.db.commit()
        return message

    async def get_sibling_messages(self, message_id: int) -> dict:
        """
        获取消息的兄弟分支（基于 SQL parent_id 查询）
        
        这是业界标准做法，极其简单高效。
        
        Returns:
            {
                "current": 0,  # 当前索引
                "total": 2,    # 总数
                "siblings": ["msg_id_1", "msg_id_2"]
            }
        """
        # 1. 查当前消息
        result = await self.db.execute(
            select(Message).where(Message.id == message_id)
        )
        current_msg = result.scalar_one_or_none()
        
        if not current_msg or not current_msg.parent_id:
            return {"current": 0, "total": 1, "siblings": [str(message_id)]}
        
        # 2. 查所有同父消息
        result = await self.db.execute(
            select(Message.id)
            .where(Message.parent_id == current_msg.parent_id)
            .order_by(Message.create_time.asc())
        )
        siblings = [str(row[0]) for row in result.all()]
        
        # 3. 计算当前索引
        try:
            current_index = siblings.index(str(message_id))
        except ValueError:
            current_index = 0
        
        return {
            "current": current_index,
            "total": len(siblings),
            "siblings": siblings,
        }

    async def get_message_by_id(self, message_id: int) -> Message | None:
        """根据 ID 获取消息"""
        result = await self.db.execute(
            select(Message).where(Message.id == message_id)
        )
        return result.scalar_one_or_none()

    async def set_current_message(self, conversation_id: int, message_id: int) -> None:
        """
        保存用户选择的当前分支消息 ID。
        
        当用户通过分支导航器切换分支时调用，
        刷新页面后 history 会从这个消息开始回溯构建链路。
        """
        await self.db.execute(
            update(Conversation)
            .where(Conversation.id == conversation_id)
            .values(current_message_id=message_id, update_time=datetime.now())
        )
        await self.db.commit()

    async def get_current_message_id(self, conversation_id: int) -> int | None:
        """获取会话保存的当前消息 ID"""
        result = await self.db.execute(
            select(Conversation.current_message_id)
            .where(Conversation.id == conversation_id)
        )
        return result.scalar_one_or_none()

