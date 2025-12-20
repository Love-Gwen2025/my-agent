"""
聊天服务 v2 - 使用 LangGraph 原生 checkpoint 分支

架构：
1. 只传 thread_id + checkpoint_id，LangGraph 自动管理历史
2. RAG 和搜索作为工具，模型自主决定调用
3. 每轮结束持久化到数据库（用于展示和审计）
"""

import asyncio
import json
import logging
from collections.abc import AsyncIterator

from langchain_core.messages import HumanMessage, SystemMessage
from sqlalchemy.ext.asyncio import AsyncSession

from app.agent.graph import create_default_agent
from app.core.checkpointer import create_redis_checkpointer
from app.core.settings import Settings
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService
from app.tools.rag_tool import set_rag_context

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
        self._logger = logging.getLogger(__name__)

    def _has_model(self) -> bool:
        """检查是否已配置模型服务"""
        return self.model_service is not None

    async def stream(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None = None,
        checkpoint_id: str | None = None,
        db: AsyncSession | None = None,
    ) -> AsyncIterator[str]:
        """
        流式对话 - 使用 LangGraph 原生状态管理

        流程：
        1. 校验会话归属
        2. 持久化用户消息
        3. 设置 RAG 上下文
        4. 调用 LangGraph（自动加载历史、执行工具）
        5. 流式输出
        6. 持久化助手回复

        Args:
            user_id: 用户 ID
            conversation_id: 会话 ID
            content: 用户消息内容
            model_code: 模型编码（可选）
            checkpoint_id: 检查点 ID（可选，用于分支/时间旅行）
            db: 数据库会话（用于 RAG）

        Yields:
            JSON 格式的 SSE 数据
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

        # 3. 设置 RAG 上下文（供 rag_tool 使用）
        set_rag_context(
            embedding_service=self.embedding_service,
            db_session=db,
            conversation_id=conversation_id,
        )

        full_reply = []
        placeholder_message_id = -1

        # 4. 构建 LangGraph config
        config = {
            "configurable": {
                "thread_id": str(conversation_id),
            }
        }
        # 如果指定了 checkpoint_id，从该点分叉
        if checkpoint_id:
            config["configurable"]["checkpoint_id"] = checkpoint_id

        if self._has_model():
            async with create_redis_checkpointer(self.settings) as checkpointer:
                model = self.model_service.get_model()
                
                # 创建带工具的 Agent
                graph = create_default_agent(
                    model=model,
                    checkpointer=checkpointer,
                    enable_rewrite=True,
                )

                # 构建输入消息
                input_messages = [
                    SystemMessage(content=SYSTEM_PROMPT),
                    HumanMessage(content=content),
                ]

                # 使用 astream_events 获得 token 级流式输出
                async for event in graph.astream_events(
                    {"messages": input_messages},
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

                    # 工具开始调用
                    elif kind == "on_tool_start":
                        tool_name = event.get("name", "unknown")
                        self._logger.info(f"Tool started: {tool_name}")
                        yield json.dumps(
                            {
                                "type": "tool_start",
                                "tool": tool_name,
                                "conversationId": str(conversation_id),
                            },
                            ensure_ascii=False,
                        )

                    # 工具调用结束
                    elif kind == "on_tool_end":
                        tool_name = event.get("name", "unknown")
                        self._logger.info(f"Tool ended: {tool_name}")
                        yield json.dumps(
                            {
                                "type": "tool_end",
                                "tool": tool_name,
                                "conversationId": str(conversation_id),
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
                    "conversationId": str(conversation_id),
                    "messageId": placeholder_message_id,
                },
                ensure_ascii=False,
            )

        # 5. 持久化助手消息
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

    async def _store_embeddings_async(
        self,
        db: AsyncSession,
        user_message_id: int,
        assistant_message_id: int,
        conversation_id: int,
        user_id: int,
        user_content: str,
        assistant_content: str,
    ) -> None:
        """异步存储消息的 embedding"""
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
        except Exception as e:
            self._logger.error(f"Failed to store embeddings: {e}")

    def _handle_task_exception(self, task: asyncio.Task) -> None:
        """处理异步任务异常"""
        try:
            task.result()
        except asyncio.CancelledError:
            pass
        except Exception as e:
            self._logger.error(f"Background task failed: {e}")
