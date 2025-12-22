from datetime import datetime

from sqlalchemy import JSON, BigInteger, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Conversation(Base):
    """
    1. 会话实体，对应 t_conversation 表。
    """

    __tablename__ = "t_conversation"

    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[str | None] = mapped_column(String(255))
    model_code: Mapped[str | None] = mapped_column(String(50))
    last_message_id: Mapped[int | None] = mapped_column(BigInteger)
    last_message_at: Mapped[datetime | None] = mapped_column()
    ext: Mapped[dict | None] = mapped_column(JSON)
    avatar: Mapped[str | None] = mapped_column(String(512))
    current_message_id: Mapped[int | None] = mapped_column(BigInteger)

    def to_vo(self) -> dict:
        """
        1. 转为接口会话视图。
        注意: BigInt ID 转为字符串，避免 JavaScript 精度丢失
        """
        return {
            "id": str(self.id),
            "title": self.title,
            "userId": str(self.user_id),
            "modelCode": self.model_code,
            "lastMessageId": str(self.last_message_id) if self.last_message_id else None,
            "lastMessageAt": self.last_message_at.isoformat() if self.last_message_at else None,
            "avatar": self.avatar,
        }

