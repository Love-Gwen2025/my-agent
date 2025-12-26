"""
数据转换器 - MapStruct 风格

统一 Model → VO 的转换逻辑，确保类型安全和一致性。
模型层的 to_vo() 方法保留用于简单场景，复杂场景使用转换器。
"""

from app.models.conversation import Conversation
from app.models.message import Message
from app.schema.conversation import ConversationVo, MessageVo


class ConversationConverter:
    """会话数据转换器"""

    @staticmethod
    def to_vo(model: Conversation) -> ConversationVo:
        """
        Conversation Model → ConversationVo

        注意: BigInt ID 转为字符串，避免 JavaScript 精度丢失
        """
        return ConversationVo(
            id=str(model.id),
            title=model.title,
            userId=str(model.user_id),
            modelCode=model.model_code,
            lastMessageId=str(model.last_message_id) if model.last_message_id else None,
            lastMessageAt=model.last_message_at.isoformat() if model.last_message_at else None,
            avatar=model.avatar,
        )

    @staticmethod
    def to_vo_list(models: list[Conversation]) -> list[ConversationVo]:
        """批量转换"""
        return [ConversationConverter.to_vo(m) for m in models]

    @staticmethod
    def from_dict(data: dict) -> ConversationVo:
        """
        字典 → ConversationVo

        用于 Service 返回 dict 的场景
        """
        return ConversationVo(**data)


class MessageConverter:
    """消息数据转换器"""

    @staticmethod
    def to_vo(model: Message) -> MessageVo:
        """
        Message Model → MessageVo
        """
        return MessageVo(
            id=str(model.id),
            conversationId=str(model.conversation_id),
            senderId=str(model.sender_id),
            role=model.role,
            content=model.content or "",
            contentType=model.content_type or "TEXT",
            modelCode=model.model_code,
            tokenCount=model.token_count,
            createTime=model.create_time.isoformat() if model.create_time else None,
            parentId=str(model.parent_id) if model.parent_id else None,
            checkpointId=model.checkpoint_id,
        )

    @staticmethod
    def to_vo_list(models: list[Message]) -> list[MessageVo]:
        """批量转换"""
        return [MessageConverter.to_vo(m) for m in models]

    @staticmethod
    def from_dict(data: dict) -> MessageVo:
        """
        字典 → MessageVo

        用于 Service 返回 dict 的场景
        """
        return MessageVo(**data)
