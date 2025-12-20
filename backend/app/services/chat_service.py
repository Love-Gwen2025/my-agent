"""
聊天服务 - 使用 LangGraph 自动状态管理

本服务集成 LangGraph RedisSaver 实现对话记忆自动持久化：
1. 每次对话通过 thread_id 自动加载历史状态
2. 对话结束后自动保存到 Redis
3. 保留 RAG 检索能力（pgvector）
"""

import asyncio
import json
import logging
from collections.abc import AsyncIterator

from langchain_core.messages import AIMessage, AnyMessage, HumanMessage, SystemMessage
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
        self.rag_similarity_threshold = settings.rag_similarity_threshold if settings else 0.6
        self.max_history_messages = settings.max_history_messages if settings else 20
        self.max_history_tokens = settings.max_history_tokens if settings else 4000

        # Logger for async task errors
        self._logger = logging.getLogger(__name__)

    def _has_model(self) -> bool:
        """
        检查是否已配置模型服务
        """
        return self.model_service is not None

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

    async def _load_history_from_db(
        self, 
        user_id: int, 
        conversation_id: int,
        limit: int | None = None,
    ) -> list[AnyMessage]:
        """
        从数据库加载历史消息，作为 Redis 过期时的 fallback。
        
        Args:
            user_id: 用户 ID
            conversation_id: 会话 ID
            limit: 加载消息数量限制，默认使用 max_history_messages 配置
            
        Returns:
            LangChain 消息列表 (HumanMessage/AIMessage)
        """
        limit = limit or self.max_history_messages
        
        try:
            # 从数据库加载历史消息
            history = await self.conversation_service.history(user_id, conversation_id)
            
            # 取最近 N 条（排除最新的用户消息，因为会单独添加）
            recent_history = history[-(limit + 1):-1] if len(history) > 1 else []
            
            # 转换为 LangChain 消息格式
            messages = []
            for msg in recent_history:
                if msg["role"] == "user":
                    messages.append(HumanMessage(content=msg["content"]))
                elif msg["role"] == "assistant":
                    messages.append(AIMessage(content=msg["content"]))
            
            return messages
        except Exception as e:
            self._logger.warning(f"Failed to load history from DB: {e}")
            return []

    async def _build_messages_with_history(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        rag_context: str | None = None,
    ) -> list[AnyMessage]:
        """
        构建包含历史消息的完整消息列表。
        
        结构：
        1. SystemMessage: 系统提示
        2. 历史消息（从数据库加载，作为 Redis 过期的 fallback）
        3. SystemMessage: RAG 上下文（如果有）
        4. HumanMessage: 当前用户消息
        """
        messages = [SystemMessage(content=SYSTEM_PROMPT)]
        
        # 加载数据库历史作为 fallback（LangGraph 会自动合并 Redis 中的状态）
        db_history = await self._load_history_from_db(user_id, conversation_id)
        if db_history:
            messages.extend(db_history)
        
        # RAG 上下文
        if rag_context:
            messages.append(
                SystemMessage(content=f"以下是与当前话题相关的历史对话片段，可作为参考：\n{rag_context}")
            )
        
        # 当前用户消息
        messages.append(HumanMessage(content=content))
        
        return messages


    def _trim_messages(
        self,
        messages: list[AnyMessage],
        max_messages: int | None = None,
        max_tokens: int | None = None,
    ) -> list[AnyMessage]:
        """
        裁剪消息历史，避免超出 LLM 上下文窗口限制。

        策略：
        1. 始终保留 SystemMessage
        2. 保留最近的 N 条消息
        3. 估算 token 数 (字符数/2) 并裁剪

        Args:
            messages: 消息列表
            max_messages: 最大消息数（不包括 SystemMessage）
            max_tokens: 最大 token 数估算值

        Returns:
            裁剪后的消息列表
        """
        max_messages = max_messages or self.max_history_messages
        max_tokens = max_tokens or self.max_history_tokens

        # 分离 SystemMessage 和其他消息
        system_messages = [m for m in messages if isinstance(m, SystemMessage)]
        other_messages = [m for m in messages if not isinstance(m, SystemMessage)]

        # 按消息数量裁剪
        if len(other_messages) > max_messages:
            other_messages = other_messages[-max_messages:]

        # 按 token 数估算裁剪 (中文字符数 / 2 ≈ token 数)
        def estimate_tokens(msg: AnyMessage) -> int:
            content = getattr(msg, "content", "")
            if isinstance(content, str):
                return len(content) // 2
            return 0

        total_tokens = sum(estimate_tokens(m) for m in other_messages)
        while total_tokens > max_tokens and len(other_messages) > 1:
            removed = other_messages.pop(0)
            total_tokens -= estimate_tokens(removed)

        return system_messages + other_messages

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
                    similarity_threshold=self.rag_similarity_threshold,
                )
                rag_context = self._build_rag_context(relevant_messages)
            except Exception as e:
                self._logger.warning(f"RAG search failed: {e}")

        # 4. 调用 LangGraph
        if self._has_model():
            # 构建消息列表（包含数据库历史作为 fallback）
            messages = await self._build_messages_with_history(
                user_id=user_id,
                conversation_id=conversation_id,
                content=content,
                rag_context=rag_context,
            )


            # 需要在 checkpointer 上下文中运行
            async with create_redis_checkpointer(self.settings) as checkpointer:
                # 创建带 checkpointer 的 graph
                model = self.model_service.get_model()
                temp_graph = create_agent_graph(
                    model=model,
                    tools=[],
                    checkpointer=checkpointer,
                )
                # 调用 graph (thread_id 用于自动 checkpoint)
                config = {"configurable": {"thread_id": str(conversation_id)}}
                
                # 裁剪消息历史，避免超出上下文窗口
                trimmed_messages = self._trim_messages(messages)
                result = await temp_graph.ainvoke({"messages": trimmed_messages}, config=config)

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

        # 6. 异步存储 embedding（不阻塞，带异常处理）
        if self.embedding_service and db:
            task = asyncio.create_task(
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
            task.add_done_callback(self._handle_task_exception)

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
            self._logger.error(f"Failed to store embeddings: {e}")

    def _handle_task_exception(self, task: asyncio.Task) -> None:
        """
        异步任务异常处理回调。

        用于捕获 fire-and-forget 任务的异常，避免静默失败。
        """
        try:
            exc = task.exception()
            if exc:
                self._logger.error(f"Background task failed: {exc}", exc_info=exc)
        except asyncio.CancelledError:
            pass  # 任务被取消时忽略

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
                    similarity_threshold=self.rag_similarity_threshold,
                )
                rag_context = self._build_rag_context(relevant_messages)
            except Exception as e:
                self._logger.warning(f"RAG search failed: {e}")

        # 4. 构建消息（包含数据库历史作为 fallback）
        messages = await self._build_messages_with_history(
            user_id=user_id,
            conversation_id=conversation_id,
            content=content,
            rag_context=rag_context,
        )

        full_reply = []
        placeholder_message_id = -1
        config = {"configurable": {"thread_id": str(conversation_id)}}


        if self._has_model():
            # 需要在 checkpointer 上下文中运行
            async with create_redis_checkpointer(self.settings) as checkpointer:
                # 重新创建带 checkpointer 的 graph
                model = self.model_service.get_model()
                temp_graph = create_agent_graph(
                    model=model,
                    tools=[],
                    checkpointer=checkpointer,
                )
                # 裁剪消息历史
                trimmed_messages = self._trim_messages(messages)
                
                # 使用 astream_events 获得真正的 token 级流式输出
                # 同时保留 LangGraph 的完整图结构和工具路由能力
                async for event in temp_graph.astream_events(
                    {"messages": trimmed_messages}, 
                    config=config, 
                    version="v2"
                ):
                    kind = event.get("event", "")
                    
                    # LLM 生成的 token
                    if kind == "on_chat_model_stream":
                        chunk = event.get("data", {}).get("chunk")
                        if chunk and hasattr(chunk, "content") and chunk.content:
                            token = str(chunk.content)
                            full_reply.append(token)
                            yield json.dumps(
                                {
                                    "type": "chunk",
                                    "content": token,
                                    "conversationId": str(conversation_id),
                                    "messageId": placeholder_message_id,
                                },
                                ensure_ascii=False,
                            )
                    
                    # 工具开始调用（可选：向前端发送工具状态）
                    elif kind == "on_tool_start":
                        tool_name = event.get("name", "unknown")
                        self._logger.info(f"Tool started: {tool_name}")
                        # 可选：通知前端工具正在执行
                        # yield json.dumps({"type": "tool_start", "tool": tool_name})
                    
                    # 工具调用结束（可选：向前端发送工具结果）
                    elif kind == "on_tool_end":
                        tool_name = event.get("name", "unknown")
                        self._logger.info(f"Tool ended: {tool_name}")
                        # 可选：通知前端工具执行完成
                        # yield json.dumps({"type": "tool_end", "tool": tool_name})
        else:
            # 未接入模型时的回退
            fallback = f"暂未接入模型，回显: {content}"
            full_reply.append(fallback)
            yield json.dumps(
                {
                    "type": "chunk",
                    "content": fallback,
                    "conversationId": str(conversation_id),
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

        # 6. 异步存储 embedding（带异常处理）
        if self.embedding_service and db:
            task = asyncio.create_task(
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
            task.add_done_callback(self._handle_task_exception)

        # 7. 发送完成信号
        yield json.dumps(
            {
                "type": "done",
                "messageId": str(assistant_message.id),
                "conversationId": str(conversation_id),
                "tokenCount": len(reply_text),
            },
            ensure_ascii=False,
        )
