from datetime import datetime

from sqlalchemy import JSON, BigInteger, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Message(Base):
    """
    1. 消息实体，对应 t_message 表。
    """

    __tablename__ = "t_message"

    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    sender_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    role: Mapped[str] = mapped_column(String(20), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    content_type: Mapped[str] = mapped_column(String(50), default="TEXT")
    token_count: Mapped[int] = mapped_column(Integer, default=0)
    model_code: Mapped[str | None] = mapped_column(String(50))
    status: Mapped[int] = mapped_column(Integer, default=1)
    ext: Mapped[dict | None] = mapped_column(JSON)

    def to_vo(self) -> dict:
        """
        1. 转为接口需要的消息视图。
        注意: BigInt ID 转为字符串，避免 JavaScript 精度丢失
        """
        return {
            "id": str(self.id),
            "conversationId": str(self.conversation_id),
            "senderId": str(self.sender_id),
            "role": self.role,
            "content": self.content,
            "contentType": self.content_type,
            "modelCode": self.model_code,
            "tokenCount": self.token_count,
            "createTime": self.create_time.isoformat()
            if isinstance(self.create_time, datetime)
            else None,
        }

