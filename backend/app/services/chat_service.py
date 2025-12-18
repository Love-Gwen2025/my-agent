"""
聊天服务 - 集成 RAG 和对话缓存
"""

import asyncio
import json
from collections.abc import AsyncIterator

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.settings import Settings
from app.services.conversation_cache_service import ConversationCacheService
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService

SYSTEM_PROMPT = """你是一个智能助手。请基于对话历史和相关上下文来回答用户的问题。
保持回答简洁、准确、有帮助。如果不确定答案，请诚实说明。"""


class ChatService:
    """
    1. 处理同步与流式对话
    2. 集成 Redis 短期缓存和 pgvector RAG 检索
    """

    def __init__(
        self,
        conversation_service: ConversationService,
        model_service: ModelService | None = None,
        embedding_service: EmbeddingService | None = None,
        cache_service: ConversationCacheService | None = None,
        settings: Settings | None = None,
    ):
        self.conversation_service = conversation_service
        self.model_service = model_service
        self.embedding_service = embedding_service
        self.cache_service = cache_service
        self.settings = settings
        self.rag_enabled = settings.rag_enabled if settings else False
        self.rag_top_k = settings.rag_top_k if settings else 5

    def _build_context_prompt(
        self,
        recent_messages: list[dict],
        relevant_messages: list[dict],
        current_message: str,
    ) -> list:
        """
        构建带上下文的消息列表
        """
        messages = [SystemMessage(content=SYSTEM_PROMPT)]

        # 添加 RAG 检索到的相关历史 (作为系统上下文)
        if relevant_messages:
            context_parts = []
            for msg in relevant_messages:
                role_label = "用户" if msg["role"] == "user" else "助手"
                context_parts.append(f"{role_label}: {msg['content']}")

            context_text = "\n".join(context_parts)
            messages.append(
                SystemMessage(
                    content=f"以下是与当前话题相关的历史对话片段，可作为参考：\n{context_text}"
                )
            )

        # 添加最近的对话历史 (短期记忆)
        for msg in recent_messages:
            if msg["role"] == "user":
                messages.append(HumanMessage(content=msg["content"]))
            else:
                messages.append(AIMessage(content=msg["content"]))

        # 添加当前用户消息
        messages.append(HumanMessage(content=current_message))

        return messages

    async def chat(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None,
        db: AsyncSession | None = None,
    ) -> tuple[str, int]:
        """
        同步对话：集成 RAG 检索和缓存
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 记录用户消息
        user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 获取上下文
        recent_messages = []
        relevant_messages = []

        # 3.1 从 Redis 获取最近消息 (短期记忆)
        if self.cache_service:
            recent_messages = await self.cache_service.get_messages_for_llm(conversation_id)

        # 3.2 从 pgvector 检索相关消息 (长期记忆 - RAG)
        if self.rag_enabled and self.embedding_service and db:
            try:
                relevant_messages = await self.embedding_service.search_similar(
                    db=db,
                    query=content,
                    conversation_id=conversation_id,
                    top_k=self.rag_top_k,
                )
            except Exception as e:
                # RAG 失败不影响主流程
                print(f"RAG search failed: {e}")

        # 4. 构建 prompt 并调用模型
        if self.model_service:
            messages = self._build_context_prompt(recent_messages, relevant_messages, content)
            reply = await self.model_service.chat_with_messages(messages)
        else:
            reply = f"暂未接入模型，回显: {content}"

        # 5. 记录助手回复
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply),
        )

        # 6. 异步更新缓存和 embedding
        if self.cache_service:
            await self.cache_service.add_message(conversation_id, "user", content, user_message.id)
            await self.cache_service.add_message(
                conversation_id, "assistant", reply, assistant_message.id
            )

        if self.embedding_service and db:
            # 异步存储 embedding，不阻塞响应
            asyncio.create_task(
                self._store_embeddings_async(
                    db,
                    user_message.id,
                    assistant_message.id,
                    conversation_id,
                    user_id,
                    content,
                    reply,
                )
            )

        return reply, assistant_message.id

    async def _store_embeddings_async(
        self,
        db: AsyncSession,
        user_msg_id: int,
        assistant_msg_id: int,
        conversation_id: int,
        user_id: int,
        user_content: str,
        assistant_content: str,
    ):
        """异步存储消息 embedding"""
        try:
            if self.embedding_service:
                await self.embedding_service.store_message_embedding(
                    db=db,
                    message_id=user_msg_id,
                    conversation_id=conversation_id,
                    user_id=user_id,
                    role="user",
                    content=user_content,
                )
                await self.embedding_service.store_message_embedding(
                    db=db,
                    message_id=assistant_msg_id,
                    conversation_id=conversation_id,
                    user_id=user_id,
                    role="assistant",
                    content=assistant_content,
                )
        except Exception as e:
            print(f"Failed to store embeddings: {e}")

    async def stream(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None,
        db: AsyncSession | None = None,
    ) -> AsyncIterator[str]:
        """
        流式对话：真正的流式输出，逐 token 从 LLM 推送到客户端。

        流程：
        1. 校验会话归属
        2. 记录用户消息
        3. 获取上下文 (Redis 短期记忆 + RAG 长期记忆)
        4. 构建 prompt 并流式调用模型
        5. 逐 token yield 给客户端
        6. 流结束后：记录助手消息、更新缓存和 embedding
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 记录用户消息 (先持久化用户输入)
        user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 获取上下文
        recent_messages = []
        relevant_messages = []

        # 3.1 从 Redis 获取最近消息 (短期记忆)
        if self.cache_service:
            recent_messages = await self.cache_service.get_messages_for_llm(conversation_id)

        # 3.2 从 pgvector 检索相关消息 (长期记忆 - RAG)
        if self.rag_enabled and self.embedding_service and db:
            try:
                relevant_messages = await self.embedding_service.search_similar(
                    db=db,
                    query=content,
                    conversation_id=conversation_id,
                    top_k=self.rag_top_k,
                )
            except Exception as e:
                # RAG 失败不影响主流程
                print(f"RAG search failed: {e}")

        # 4. 构建 prompt
        messages = self._build_context_prompt(recent_messages, relevant_messages, content)

        # 5. 流式调用模型，逐 token yield
        # 预先生成占位 message_id 用于客户端 (后续会更新)
        # 注意：这里我们先不知道完整 reply，所以用 -1 占位
        placeholder_message_id = -1
        full_reply = []  # 用于累积完整回复

        if self.model_service:
            # 真正的流式输出：从 LLM 获取每个 token
            async for token in self.model_service.stream_with_messages(messages):
                full_reply.append(token)
                yield json.dumps(
                    {
                        "type": "chunk",
                        "content": token,
                        "conversationId": conversation_id,
                        "messageId": placeholder_message_id,
                    },
                    ensure_ascii=False,
                )
        else:
            # 未接入模型时的回退
            fallback = f"暂未接入模型，回显: {content}"
            full_reply.append(fallback)
            yield json.dumps(
                {
                    "type": "chunk",
                    "content": fallback,
                    "conversationId": conversation_id,
                    "messageId": placeholder_message_id,
                },
                ensure_ascii=False,
            )

        # 6. 流结束后：记录助手消息
        reply_text = "".join(full_reply)
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply_text,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply_text),
        )

        # 7. 更新缓存
        if self.cache_service:
            await self.cache_service.add_message(conversation_id, "user", content, user_message.id)
            await self.cache_service.add_message(
                conversation_id, "assistant", reply_text, assistant_message.id
            )

        # 8. 异步存储 embedding (不阻塞响应)
        if self.embedding_service and db:
            asyncio.create_task(
                self._store_embeddings_async(
                    db,
                    user_message.id,
                    assistant_message.id,
                    conversation_id,
                    user_id,
                    content,
                    reply_text,
                )
            )

        # 9. 发送完成信号，包含真实的 message_id
        yield json.dumps(
            {
                "type": "done",
                "messageId": assistant_message.id,
                "conversationId": conversation_id,
                "tokenCount": len(reply_text),
            },
            ensure_ascii=False,
        )
