"""
聊天服务 - 使用 LangGraph 自动状态管理

本服务集成 LangGraph RedisSaver 实现对话记忆自动持久化：
1. 每次对话通过 thread_id 自动加载历史状态
2. 对话结束后自动保存到 Redis
3. 保留 RAG 检索能力（pgvector）
"""

import asyncio
import json
from collections.abc import AsyncIterator

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from sqlalchemy.ext.asyncio import AsyncSession

from app.agent.graph import AgentState, create_agent_graph
from app.core.checkpointer import create_redis_checkpointer
from app.core.settings import Settings
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService

SYSTEM_PROMPT = """你是一个智能助手。请基于对话历史和相关上下文来回答用户的问题。
保持回答简洁、准确、有帮助。如果不确定答案，请诚实说明。"""


class ChatService:
    """
    聊天服务 - 使用 LangGraph 自动状态管理

    特性：
    1. 自动对话记忆：通过 RedisSaver 自动保存/恢复对话状态
    2. RAG 检索：使用 pgvector 进行语义相关消息检索
    3. 流式输出：支持逐 token 推送到客户端
    """

    def __init__(
        self,
        conversation_service: ConversationService,
        model_service: ModelService | None = None,
        embedding_service: EmbeddingService | None = None,
        settings: Settings | None = None,
    ):
        self.conversation_service = conversation_service
        self.model_service = model_service
        self.embedding_service = embedding_service
        self.settings = settings
        self.rag_enabled = settings.rag_enabled if settings else False
        self.rag_top_k = settings.rag_top_k if settings else 5

        # 初始化 LangGraph 组件
        self._graph = None
        self._checkpointer = None

    async def _get_graph(self):
        """
        延迟初始化 LangGraph 图（带 Redis checkpointer）
        """
        if self._graph is None and self.model_service:
            # 创建 checkpointer
            self._checkpointer = await create_redis_checkpointer(self.settings)
            # 创建图
            model = self.model_service.get_model()
            self._graph = create_agent_graph(
                model=model,
                tools=[],  # 暂无工具
                checkpointer=self._checkpointer,
            )
        return self._graph

    def _build_rag_context(self, relevant_messages: list[dict]) -> str | None:
        """
        构建 RAG 检索上下文
        """
        if not relevant_messages:
            return None

        context_parts = []
        for msg in relevant_messages:
            role_label = "用户" if msg["role"] == "user" else "助手"
            context_parts.append(f"{role_label}: {msg['content']}")

        return "\n".join(context_parts)

    async def chat(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None,
        db: AsyncSession | None = None,
    ) -> tuple[str, int]:
        """
        同步对话：使用 LangGraph 自动管理对话历史

        流程：
        1. 校验会话归属
        2. 持久化用户消息
        3. 可选：RAG 检索相关上下文
        4. 调用 LangGraph graph（自动加载/保存状态）
        5. 持久化助手消息
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 持久化用户消息到数据库
        user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 可选：RAG 检索相关上下文
        rag_context = None
        if self.rag_enabled and self.embedding_service and db:
            try:
                relevant_messages = await self.embedding_service.search_similar(
                    db=db,
                    query=content,
                    conversation_id=conversation_id,
                    top_k=self.rag_top_k,
                )
                rag_context = self._build_rag_context(relevant_messages)
            except Exception as e:
                print(f"RAG search failed: {e}")

        # 4. 调用 LangGraph
        graph = await self._get_graph()

        if graph:
            # 构建消息列表
            messages = [SystemMessage(content=SYSTEM_PROMPT)]
            if rag_context:
                messages.append(
                    SystemMessage(content=f"以下是与当前话题相关的历史对话片段，可作为参考：\n{rag_context}")
                )
            messages.append(HumanMessage(content=content))

            # 调用 graph (thread_id 用于自动 checkpoint)
            config = {"configurable": {"thread_id": str(conversation_id)}}
            result = await graph.ainvoke({"messages": messages}, config=config)

            # 提取回复
            last_message = result["messages"][-1]
            reply = last_message.content if isinstance(last_message, AIMessage) else str(last_message)
        else:
            reply = f"暂未接入模型，回显: {content}"

        # 5. 持久化助手消息
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply),
        )

        # 6. 异步存储 embedding（不阻塞）
        if self.embedding_service and db:
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
        流式对话：使用 LangGraph 自动管理对话历史，逐 token 输出

        流程：
        1. 校验会话归属
        2. 持久化用户消息
        3. 可选：RAG 检索相关上下文
        4. 流式调用 LangGraph graph
        5. 逐 token yield 给客户端
        6. 流结束后持久化助手消息
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 持久化用户消息
        user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 可选：RAG 检索
        rag_context = None
        if self.rag_enabled and self.embedding_service and db:
            try:
                relevant_messages = await self.embedding_service.search_similar(
                    db=db,
                    query=content,
                    conversation_id=conversation_id,
                    top_k=self.rag_top_k,
                )
                rag_context = self._build_rag_context(relevant_messages)
            except Exception as e:
                print(f"RAG search failed: {e}")

        # 4. 构建消息并流式调用
        messages = [SystemMessage(content=SYSTEM_PROMPT)]
        if rag_context:
            messages.append(
                SystemMessage(content=f"以下是与当前话题相关的历史对话片段，可作为参考：\n{rag_context}")
            )
        messages.append(HumanMessage(content=content))

        full_reply = []
        placeholder_message_id = -1
        config = {"configurable": {"thread_id": str(conversation_id)}}

        graph = await self._get_graph()

        if graph:
            # 使用 astream 实现流式输出
            async for event in graph.astream({"messages": messages}, config=config, stream_mode="values"):
                # 获取最后一条消息
                if "messages" in event and event["messages"]:
                    last_msg = event["messages"][-1]
                    if isinstance(last_msg, AIMessage) and last_msg.content:
                        # 计算增量 (新增的内容)
                        current_content = last_msg.content
                        previous_len = len("".join(full_reply))
                        if len(current_content) > previous_len:
                            delta = current_content[previous_len:]
                            full_reply.clear()
                            full_reply.append(current_content)
                            yield json.dumps(
                                {
                                    "type": "chunk",
                                    "content": delta,
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

        # 5. 流结束后：持久化助手消息
        reply_text = "".join(full_reply) if full_reply else ""
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply_text,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply_text),
        )

        # 6. 异步存储 embedding
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

        # 7. 发送完成信号
        yield json.dumps(
            {
                "type": "done",
                "messageId": assistant_message.id,
                "conversationId": conversation_id,
                "tokenCount": len(reply_text),
            },
            ensure_ascii=False,
        )
