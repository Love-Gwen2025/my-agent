"""
Agent 服务层 - 封装 LangGraph Agent 的业务逻辑

本模块提供：
1. AgentService: 封装 Agent 图的调用，集成消息持久化
2. 支持同步和流式两种调用模式
"""

import json
from collections.abc import AsyncIterator

from langchain_core.messages import AIMessage, HumanMessage

from app.agent import create_default_agent
from app.core.settings import Settings
from app.services.conversation_service import ConversationService
from app.services.model_service import ModelService


class AgentService:
    """
    Agent 服务 - 封装 LangGraph Agent 的业务调用

    职责：
    1. 管理 Agent 图的生命周期
    2. 集成会话服务（消息持久化）
    3. 提供同步和流式调用接口
    """

    def __init__(
        self,
        model_service: ModelService,
        conversation_service: ConversationService,
        settings: Settings,
    ):
        self.model_service = model_service
        self.conversation_service = conversation_service
        self.settings = settings

        # 创建 Agent 图 (使用默认工具集)
        self.agent = create_default_agent(model_service.get_model())

    async def chat(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None = None,
    ) -> tuple[str, int]:
        """
        同步 Agent 对话。

        流程：
        1. 校验会话归属
        2. 持久化用户消息
        3. 调用 Agent 图获取回复
        4. 持久化助手消息
        5. 返回回复内容和消息 ID

        Args:
            user_id: 用户 ID
            conversation_id: 会话 ID
            content: 用户输入内容
            model_code: 模型代码（可选）

        Returns:
            (reply_text, message_id) 元组
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 持久化用户消息
        _user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 调用 Agent 图
        # 将用户输入封装为 LangChain 消息格式
        initial_state = {"messages": [HumanMessage(content=content)]}

        # 执行 Agent 图 (可能会循环多次: chatbot -> tools -> chatbot -> ...)
        result = await self.agent.ainvoke(initial_state)

        # 4. 提取最终回复
        # 最后一条消息应该是 AIMessage (不含 tool_calls)
        final_messages = result["messages"]
        reply_text = ""

        # 从后往前找最后一条有内容的 AIMessage
        for msg in reversed(final_messages):
            if isinstance(msg, AIMessage) and msg.content:
                reply_text = str(msg.content)
                break

        if not reply_text:
            reply_text = "Agent 未能生成回复"

        # 5. 持久化助手消息
        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,  # -1 表示 AI
            role="assistant",
            content=reply_text,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply_text),
        )

        return reply_text, assistant_message.id

    async def stream(
        self,
        user_id: int,
        conversation_id: int,
        content: str,
        model_code: str | None = None,
    ) -> AsyncIterator[str]:
        """
        流式 Agent 对话。

        使用 LangGraph 的 astream_events 获取实时事件流。

        注意：
        - 流式模式下，工具调用是"批量"出现的，不是逐 token
        - 我们只流式输出最终的文本回复
        - 工具执行过程作为事件发送给客户端
        """
        # 1. 校验会话归属
        await self.conversation_service.ensure_owner(conversation_id, user_id)

        # 2. 持久化用户消息
        _user_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=user_id,
            role="user",
            content=content,
            content_type="TEXT",
            model_code=model_code,
        )

        # 3. 调用 Agent 图 (流式模式)
        initial_state = {"messages": [HumanMessage(content=content)]}

        full_reply = []
        placeholder_message_id = -1

        # 使用 astream_events 获取详细事件流
        async for event in self.agent.astream_events(initial_state, version="v2"):
            kind = event.get("event")

            # 处理 LLM 流式输出事件
            if kind == "on_chat_model_stream":
                chunk = event.get("data", {}).get("chunk")
                if chunk and hasattr(chunk, "content") and chunk.content:
                    token = str(chunk.content)
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

            # 处理工具调用事件 (可选：通知前端正在使用工具)
            elif kind == "on_tool_start":
                tool_name = event.get("name", "unknown")
                yield json.dumps(
                    {
                        "type": "tool_start",
                        "tool": tool_name,
                        "conversationId": conversation_id,
                    },
                    ensure_ascii=False,
                )

            elif kind == "on_tool_end":
                tool_name = event.get("name", "unknown")
                yield json.dumps(
                    {
                        "type": "tool_end",
                        "tool": tool_name,
                        "conversationId": conversation_id,
                    },
                    ensure_ascii=False,
                )

        # 4. 持久化助手消息
        reply_text = "".join(full_reply)
        if not reply_text:
            reply_text = "Agent 未能生成回复"

        assistant_message = await self.conversation_service.persist_message(
            conversation_id=conversation_id,
            sender_id=-1,
            role="assistant",
            content=reply_text,
            content_type="TEXT",
            model_code=model_code,
            token_count=len(reply_text),
        )

        # 5. 发送完成信号
        yield json.dumps(
            {
                "type": "done",
                "messageId": assistant_message.id,
                "conversationId": conversation_id,
                "tokenCount": len(reply_text),
            },
            ensure_ascii=False,
        )
