"""
Embedding 服务 - 支持本地模型和远程 API

支持两种模式：
1. 本地模型 (bge-small-zh-v1.5) - 免费、隐私安全、无网络延迟
2. 远程 API (OpenAI/DeepSeek) - 效果好，需要 API Key
"""

from typing import Any

from loguru import logger
from sqlalchemy import text
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
        self.dimension = settings.ai_embedding_dimension
        self._model = None
        self._embeddings = None

        # 根据配置选择模型类型
        self.use_local = settings.ai_embedding_provider == "local"

    def warmup(self) -> None:
        """预加载模型（应用启动时调用）"""
        if self.use_local:
            self._get_local_model()

    def _get_local_model(self):
        """
        延迟加载本地 Embedding 模型 (使用 fastembed，比 sentence-transformers 更轻量)
        fastembed 使用 ONNX Runtime，无需 PyTorch，镜像大小从 11GB 降至 ~500MB
        """
        if self._model is None:
            from fastembed import TextEmbedding

            model_name = self.settings.ai_embedding_model
            logger.info(f"Loading local embedding model (fastembed): {model_name}")
            # fastembed 会自动下载并缓存模型到 ~/.cache/fastembed
            self._model = TextEmbedding(model_name=model_name)
            logger.info("Embedding model loaded successfully")

        return self._model

    def _get_remote_embeddings(self):
        """
        获取远程 Embedding 客户端
        """
        if self._embeddings is None:
            from langchain_openai import OpenAIEmbeddings

            api_key = self.settings.ai_embedding_api_key or self.settings.ai_openai_api_key
            base_url = self.settings.ai_embedding_base_url or self.settings.ai_openai_base_url

            self._embeddings = OpenAIEmbeddings(
                model=self.settings.ai_embedding_model,
                api_key=api_key,
                base_url=base_url,
            )

        return self._embeddings

    async def embed_text(self, text: str) -> list[float]:
        """
        生成文本的 embedding 向量
        """
        if self.use_local:
            model = self._get_local_model()
            # fastembed.embed() 返回生成器，需要转换为 list 取第一个结果
            vectors = list(model.embed([text]))
            return vectors[0].tolist()
        else:
            embeddings = self._get_remote_embeddings()
            return await embeddings.aembed_query(text)

    async def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """
        批量生成 embedding
        """
        if self.use_local:
            model = self._get_local_model()
            # fastembed.embed() 返回生成器，批量转换为列表
            vectors = list(model.embed(texts))
            return [v.tolist() for v in vectors]
        else:
            embeddings = self._get_remote_embeddings()
            return await embeddings.aembed_documents(texts)

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
        similarity_threshold: float = 0.0,
    ) -> list[dict[str, Any]]:
        """
        语义检索相关消息

        Args:
            db: 数据库会话
            query: 查询文本
            conversation_id: 可选的会话 ID 过滤
            top_k: 返回最相似的 K 条结果
            similarity_threshold: 相似度阈值，低于此值的结果将被过滤

        返回: [{"content": str, "role": str, "similarity": float}, ...]
        """
        # 生成查询向量
        query_vector = await self.embed_text(query)

        # 构建查询 - 使用余弦相似度
        # 使用 JSON 格式传递向量，避免 SQL 注入
        import json

        query_vec_json = json.dumps(query_vector)

        if conversation_id:
            sql = text("""
                SELECT
                    content,
                    role,
                    1 - (embedding <=> CAST(:query_vec AS vector)) as similarity
                FROM t_message_embedding
                WHERE conversation_id = :conv_id
                ORDER BY embedding <=> CAST(:query_vec AS vector)
                LIMIT :limit
            """)
            params = {
                "query_vec": query_vec_json,
                "conv_id": conversation_id,
                "limit": top_k,
            }
        else:
            sql = text("""
                SELECT
                    content,
                    role,
                    1 - (embedding <=> CAST(:query_vec AS vector)) as similarity
                FROM t_message_embedding
                ORDER BY embedding <=> CAST(:query_vec AS vector)
                LIMIT :limit
            """)
            params = {
                "query_vec": query_vec_json,
                "limit": top_k,
            }

        result = await db.execute(sql, params)
        rows = result.fetchall()

        # 过滤低于阈值的结果
        return [
            {"content": row.content, "role": row.role, "similarity": float(row.similarity)}
            for row in rows
            if float(row.similarity) >= similarity_threshold
        ]
