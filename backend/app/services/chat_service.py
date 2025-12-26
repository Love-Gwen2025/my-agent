"""
聊天服务 v2 - 使用 LangGraph 原生 checkpoint 分支

架构：
1. 只传 thread_id + checkpoint_id，LangGraph 自动管理历史
2. RAG 和搜索作为工具，模型自主决定调用
3. 每轮结束持久化到数据库（用于展示和审计）
"""

import asyncio
import json
from collections.abc import AsyncIterator

from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession

from app.agent.graph import create_default_agent
from app.core.checkpointer import create_checkpointer
from app.core.constants import AI_SENDER_ID, MAX_TITLE_LENGTH
from app.core.settings import Settings
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService
from app.utils.content import extract_text_content

# 系统提示词
SYSTEM_PROMPT = """你是一个智能助手。你可以使用以下工具来帮助回答问题：
- rag_search: 搜索历史对话中的相关内容
- web_search: 在互联网上搜索实时信息
- get_current_time: 获取当前时间
- simple_calculator: 进行数学计算

请根据用户的问题决定是否需要使用工具。保持回答简洁、准确、有帮助。"""


class ChatService:
    """
    聊天服务 v2 - 使用 LangGraph 原生状态管理

    特性：
    1. checkpoint_id 分支：支持从历史任意点分叉
    2. 代词消解：RewriteNode 自动处理
    3. 工具自主调用：模型决定是否调用 RAG/搜索
    4. 流式输出：逐 token 推送
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

    async def _create_title(self, msg: str) -> str:
        """
        根据消息内容自动生成会话标题

        使用构造时注入的 model_service，避免每次创建新实例
        """
        prompt = f"""
根据传入的消息,生成一个5-10字左右的标题,内容力求准确,简明,扼要。
只输出标题本身，不要加引号或其他内容。
消息: {msg}
标题:"""
        # 使用已注入的 model_service，如果没有则返回截断的消息作为标题
        if self.model_service:
            response = await self.model_service.chat(prompt)
            return response.strip()[:MAX_TITLE_LENGTH]
        return msg[:MAX_TITLE_LENGTH]

    # _ensure_string 已移除，使用 app.utils.content.extract_text_content 替代

    def _has_model(self) -> bool:
        """检查是否已配置模型服务"""
        return self.model_service is not None

    def _build_langgraph_config(
        self,
        conversation_id: int,
        db: AsyncSession | None,
        checkpoint_id: str | None = None,
    ) -> dict:
        """
        构建 LangGraph 配置

        Args:
            conversation_id: 会话 ID
            db: 数据库会话（用于 RAG）
            checkpoint_id: 可选的 checkpoint ID（用于 regenerate 回退）

        Returns:
            LangGraph 配置字典
        """
        config = {
            "configurable": {
                "thread_id": str(conversation_id),
                "embedding_service": self.embedding_service,
                "db_session": db,
                "conversation_id": conversation_id,
            }
        }
        if checkpoint_id:
            config["configurable"]["checkpoint_id"] = checkpoint_id
        return config

    def _format_sse_event(self, event_type: str, conversation_id: int, **kwargs) -> str:
        """
        格式化 SSE 事件为 JSON

        Args:
            event_type: 事件类型 (chunk/tool_start/tool_end/done)
            conversation_id: 会话 ID
            **kwargs: 额外的事件数据

        Returns:
            JSON 格式的字符串
        """
        return json.dumps(
            {"type": event_type, "conversationId": str(conversation_id), **kwargs},
            ensure_ascii=False,
        )

    async def _prepare_stream_context(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None,
        regenerate: bool,
        parent_message_id: int | None,
    ) -> tuple:
        """
        准备流式对话的上下文

        Returns:
            (conversation, generated_title, user_message, parent_checkpoint_id)
        """
        # 1. 校验会话归属
        conversation = await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 首次消息时生成标题
        generated_title = None
        if not conversation.current_message_id:
            generated_title = await self._create_title(content)
            await self.conversation_service.modify_conversation(
                user_id, conversation_id, generated_title
            )

        # 3. 持久化用户消息（regenerate 模式跳过）
        user_message = None
        if not regenerate:
            user_message = await self.conversation_service.persist_message(
                conversation_id=conversation_id,
                sender_id=user_id,
                role="user",
                content=content,
                content_type="TEXT",
                model_code=model_code,
                parent_id=parent_message_id,
            )

        # 4. 处理 regenerate 回退
        parent_checkpoint_id = None
        if regenerate and parent_message_id:
            parent_msg = await self.conversation_service.get_message_by_id(parent_message_id)
            if parent_msg and parent_msg.checkpoint_id:
                parent_checkpoint_id = parent_msg.checkpoint_id
                logger.info(f"[stream] Rollback to checkpoint: {parent_checkpoint_id}")

        return conversation, generated_title, user_message, parent_checkpoint_id

    async def stream(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None = None,
        regenerate: bool = False,
        parent_message_id: int | None = None,
        db: AsyncSession | None = None,
    ) -> AsyncIterator[str]:
        """
        流式对话 - 使用 LangGraph 原生状态管理

        流程：
        1. 校验会话归属
        1.1 如果是首次发送消息，自动生成标题
        2. 持久化用户消息（regenerate 模式下跳过）
        3. 设置 RAG 上下文
        4. 调用 LangGraph（自动加载历史、执行工具）
        5. 流式输出
        6. 持久化助手回复

        Args:
            user_id: 用户 ID
            conversation_id: 会话 ID
            content: 用户消息内容
            model_code: 模型编码（可选）
            regenerate: 重新生成模式，跳过用户消息持久化
            parent_message_id: 父消息 ID，用于构建消息树
            db: 数据库会话（用于 RAG）

        Yields:
            JSON 格式的 SSE 数据
        """
        # 1. 准备上下文（校验归属、生成标题、持久化用户消息、处理 regenerate）
        _, generated_title, user_message, parent_checkpoint_id = await self._prepare_stream_context(
            user_id, conversation_id, content, model_code, regenerate, parent_message_id
        )

        full_reply = []
        placeholder_message_id = -1

        # 2. 构建 LangGraph config
        config = self._build_langgraph_config(conversation_id, db, parent_checkpoint_id)

        logger.info(
            f"[stream] regenerate={regenerate}, parent_checkpoint_id={parent_checkpoint_id}"
        )

        if self._has_model():
            async with create_checkpointer(self.settings) as checkpointer:
                model = self.model_service.get_model()

                # 创建带工具的 Agent
                graph = create_default_agent(
                    model=model,
                    checkpointer=checkpointer,
                    enable_rewrite=True,
                )

                # 构建输入消息
                # 给 SystemMessage 固定 ID，防止 LangGraph 重复追加
                if regenerate and parent_checkpoint_id:
                    # 重新生成时，不添加新消息，直接从父 checkpoint 继续执行
                    # 这样新生成的 checkpoint 会成为原 checkpoint 的兄弟
                    input_messages = []
                else:
                    input_messages = [
                        SystemMessage(content=SYSTEM_PROMPT, id="sys_instruction"),
                        HumanMessage(content=content),
                    ]

                # 使用 astream_events 获得 token 级流式输出
                async for event in graph.astream_events(
                    {"messages": input_messages}, config=config, version="v2"
                ):
                    kind = event.get("event", "")

                    # LLM 生成的 token
                    if kind == "on_chat_model_stream":
                        chunk = event.get("data", {}).get("chunk")
                        if chunk and hasattr(chunk, "content") and chunk.content:
                            # 使用统一工具函数处理 Gemini 格式
                            token = extract_text_content(chunk.content)
                            if token:  # 只处理非空 token
                                full_reply.append(token)
                                yield self._format_sse_event(
                                    "chunk",
                                    conversation_id,
                                    content=token,
                                    messageId=placeholder_message_id,
                                )

                    elif kind == "on_tool_start":
                        tool_name = event.get("name", "unknown")
                        logger.info(f"Tool started: {tool_name}")
                        yield self._format_sse_event("tool_start", conversation_id, tool=tool_name)

                    elif kind == "on_tool_end":
                        tool_name = event.get("name", "unknown")
                        logger.info(f"Tool ended: {tool_name}")
                        yield self._format_sse_event("tool_end", conversation_id, tool=tool_name)
        else:
            # 未接入模型时的回退
            fallback = f"暂未接入模型，回显: {content}"
            full_reply.append(fallback)
            yield self._format_sse_event(
                "chunk", conversation_id, content=fallback, messageId=placeholder_message_id
            )

        # 5. 持久化助手消息
        reply_text = "".join(full_reply) if full_reply else ""

        # 获取最新 checkpoint ID 用于关联
        latest_checkpoint_id, _ = await self._get_latest_checkpoint_id(
            conversation_id=conversation_id,
        )

        # AI 消息的 parent_id 是用户消息的 ID
        ai_parent_id = user_message.id if user_message else parent_message_id

        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=AI_SENDER_ID,
            role="assistant",
            content=reply_text,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply_text),
            parent_id=ai_parent_id,
            checkpoint_id=latest_checkpoint_id,
        )

        # 6. 异步存储 embedding（使用独立 session，避免请求结束后 session 已关闭）
        if self.embedding_service:
            if user_message:
                # 正常模式：存储用户消息和 AI 回复
                task = asyncio.create_task(
                    self._store_embeddings_async(
                        user_message.id,
                        assistant_message.id,
                        conversation_id,
                        user_id,
                        content,
                        reply_text,
                    )
                )
                task.add_done_callback(self._handle_task_exception)
            else:
                # regenerate 模式：只存储 AI 回复（用户消息已存过）
                task = asyncio.create_task(
                    self._store_ai_embedding_async(
                        assistant_message.id,
                        conversation_id,
                        user_id,
                        reply_text,
                    )
                )
                task.add_done_callback(self._handle_task_exception)

        # 7. 发送完成信号
        # 获取用户消息 ID（regenerate 时使用 parent_message_id）
        user_message_id = user_message.id if user_message else parent_message_id

        yield json.dumps(
            {
                "type": "done",
                "messageId": str(assistant_message.id),
                "conversationId": str(conversation_id),
                "tokenCount": len(reply_text),
                "parentId": str(ai_parent_id) if ai_parent_id else None,
                "userMessageId": str(user_message_id) if user_message_id else None,
                "title": generated_title,  # 新生成的标题（如果有）
            },
            ensure_ascii=False,
        )

    async def _store_embeddings_async(
        self,
        user_message_id: int,
        assistant_message_id: int,
        conversation_id: int,
        user_id: int,
        user_content: str,
        assistant_content: str,
    ) -> None:
        """异步存储消息的 embedding（使用独立 session）"""
        from app.core.db import SessionLocal

        # 确保内容是字符串
        user_content = extract_text_content(user_content)
        assistant_content = extract_text_content(assistant_content)

        async with SessionLocal() as db:
            try:
                await self.embedding_service.store_message_embedding(
                    db=db,
                    message_id=user_message_id,
                    conversation_id=conversation_id,
                    user_id=user_id,
                    role="user",
                    content=user_content,
                )
                await self.embedding_service.store_message_embedding(
                    db=db,
                    message_id=assistant_message_id,
                    conversation_id=conversation_id,
                    user_id=user_id,
                    role="assistant",
                    content=assistant_content,
                )
                logger.info(
                    f"Stored embeddings for messages {user_message_id}, {assistant_message_id}"
                )
            except Exception as e:
                logger.error(f"Failed to store embeddings: {e}")

    async def _store_ai_embedding_async(
        self,
        assistant_message_id: int,
        conversation_id: int,
        user_id: int,
        assistant_content: str,
    ) -> None:
        """异步存储 AI 回复的 embedding（用于 regenerate 模式，使用独立 session）"""
        from app.core.db import SessionLocal

        # 确保内容是字符串
        assistant_content = extract_text_content(assistant_content)

        async with SessionLocal() as db:
            try:
                await self.embedding_service.store_message_embedding(
                    db=db,
                    message_id=assistant_message_id,
                    conversation_id=conversation_id,
                    user_id=user_id,
                    role="assistant",
                    content=assistant_content,
                )
                logger.info(f"Stored AI embedding for message {assistant_message_id}")
            except Exception as e:
                logger.error(f"Failed to store AI embedding: {e}")

    def _handle_task_exception(self, task: asyncio.Task) -> None:
        """处理异步任务异常"""
        try:
            task.result()
        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.error(f"Background task failed: {e}")

    async def _get_latest_checkpoint_id(
        self,
        conversation_id: int,
    ) -> str | None:
        """
        获取最新 checkpointId，用于在 SSE done 事件中返回。
        """
        try:
            # 复用仅包含 thread_id 的配置，确保读取最新状态
            config = {"configurable": {"thread_id": str(conversation_id)}}
            async with create_checkpointer(self.settings) as checkpointer:
                # 使用 alist 获取最新 checkpoint，以便获取 parent_config
                async for checkpoint_tuple in checkpointer.alist(config, limit=1):
                    # CheckpointTuple 是对象，使用属性访问
                    checkpoint = checkpoint_tuple.checkpoint or {}
                    parent_config = checkpoint_tuple.parent_config or {}

                    checkpoint_id = checkpoint.get("id")
                    parent_id = None
                    if parent_config:
                        configurable = parent_config.get("configurable", {}) or {}
                        parent_id = configurable.get("checkpoint_id")

                    return checkpoint_id, parent_id
                return None, None
        except Exception as exc:
            logger.error(f"Failed to fetch latest checkpoint info: {exc}")
            return None, None
