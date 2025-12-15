"""
Embedding 服务 - 生成向量并管理存储
"""
import asyncio
from typing import Any

from langchain_openai import OpenAIEmbeddings
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.settings import Settings
from app.models.message_embedding import MessageEmbedding


class EmbeddingService:
    """
    1. 生成文本 embedding
    2. 存储到 pgvector
    3. 语义检索相关消息
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        # 使用 OpenAI embedding (兼容 DeepSeek)
        api_key = settings.ai_embedding_api_key or settings.ai_openai_api_key
        base_url = settings.ai_embedding_base_url or settings.ai_openai_base_url
        
        self.embeddings = OpenAIEmbeddings(
            model=settings.ai_embedding_model,
            api_key=api_key,
            base_url=base_url,
        )
        self.dimension = settings.ai_embedding_dimension

    async def embed_text(self, text: str) -> list[float]:
        """
        生成文本的 embedding 向量
        """
        return await self.embeddings.aembed_query(text)

    async def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """
        批量生成 embedding
        """
        return await self.embeddings.aembed_documents(texts)

    async def store_message_embedding(
        self,
        db: AsyncSession,
        message_id: int,
        conversation_id: int,
        user_id: int,
        role: str,
        content: str,
    ) -> MessageEmbedding:
        """
        为消息生成 embedding 并存储
        """
        # 生成向量
        vector = await self.embed_text(content)
        
        # 存储
        embedding = MessageEmbedding(
            message_id=message_id,
            conversation_id=conversation_id,
            user_id=user_id,
            role=role,
            content=content,
            embedding=vector,
        )
        db.add(embedding)
        await db.commit()
        await db.refresh(embedding)
        return embedding

    async def search_similar(
        self,
        db: AsyncSession,
        query: str,
        conversation_id: int | None = None,
        top_k: int = 5,
    ) -> list[dict[str, Any]]:
        """
        语义检索相关消息
        
        返回: [{"content": str, "role": str, "similarity": float}, ...]
        """
        # 生成查询向量
        query_vector = await self.embed_text(query)
        
        # 构建查询 - 使用余弦相似度
        if conversation_id:
            sql = text("""
                SELECT 
                    content,
                    role,
                    1 - (embedding <=> :query_vec::vector) as similarity
                FROM t_message_embedding
                WHERE conversation_id = :conv_id
                ORDER BY embedding <=> :query_vec::vector
                LIMIT :limit
            """)
            params = {
                "query_vec": str(query_vector),
                "conv_id": conversation_id,
                "limit": top_k,
            }
        else:
            sql = text("""
                SELECT 
                    content,
                    role,
                    1 - (embedding <=> :query_vec::vector) as similarity
                FROM t_message_embedding
                ORDER BY embedding <=> :query_vec::vector
                LIMIT :limit
            """)
            params = {
                "query_vec": str(query_vector),
                "limit": top_k,
            }
        
        result = await db.execute(sql, params)
        rows = result.fetchall()
        
        return [
            {"content": row.content, "role": row.role, "similarity": float(row.similarity)}
            for row in rows
        ]
