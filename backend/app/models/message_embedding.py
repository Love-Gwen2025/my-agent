"""
消息向量存储模型
"""
from pgvector.sqlalchemy import Vector
from sqlalchemy import BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class MessageEmbedding(Base):
    """
    消息 Embedding 存储表，用于 RAG 语义检索
    """

    __tablename__ = "t_message_embedding"

    message_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    role: Mapped[str] = mapped_column(Text, nullable=False)  # user / assistant
    content: Mapped[str] = mapped_column(Text, nullable=False)
    # pgvector 向量列 - 维度可配置，默认 1536 (OpenAI text-embedding-3-small)
    embedding = mapped_column(Vector(1536), nullable=True)
