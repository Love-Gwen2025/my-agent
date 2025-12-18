"""
Agent API 路由 - 提供 LangGraph Agent 的 HTTP 接口

本模块提供：
- /agent/chat: 同步 Agent 对话
- /agent/stream: 流式 Agent 对话 (SSE)

使用 Agent 接口可以体验工具调用能力，例如：
- "现在几点了？" -> 调用 get_current_time 工具
- "计算 123 * 456" -> 调用 simple_calculator 工具
"""

from fastapi import APIRouter, Depends, Response, status
from fastapi.responses import JSONResponse, StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import Settings, get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.schema.conversation import StreamChatParam
from app.services.agent_service import AgentService
from app.services.conversation_service import ConversationService
from app.services.model_service import ModelService
from app.tools import AVAILABLE_TOOLS

router = APIRouter(prefix="/agent", tags=["Agent (LangGraph)"])


def create_agent_service(
    db: AsyncSession,
    settings: Settings,
) -> AgentService:
    """
    创建 AgentService 实例，包含所有依赖
    """
    conv_service = ConversationService(db)

    # 创建 ModelService，绑定工具
    model_service = ModelService(settings, tools=AVAILABLE_TOOLS)

    return AgentService(
        model_service=model_service,
        conversation_service=conv_service,
        settings=settings,
    )


@router.post("/chat", response_model=ApiResult[str])
async def agent_chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[str]:
    """
    同步 Agent 对话。

    Agent 可以使用工具来回答问题，例如：
    - "现在几点了？" -> 调用时间工具
    - "从2024-01-01到今天过了多少天？" -> 调用日期计算工具
    - "计算 123 * 456 的结果" -> 调用计算器工具
    """
    settings = get_settings()
    agent_service = create_agent_service(db, settings)

    try:
        reply, _ = await agent_service.chat(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
        )
        return ApiResult.ok(reply)
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.post("/stream")
async def agent_stream(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    流式 Agent 对话 (Server-Sent Events)。

    事件类型:
    - chunk: 文本内容片段 {"type": "chunk", "content": "..."}
    - tool_start: 开始执行工具 {"type": "tool_start", "tool": "get_current_time"}
    - tool_end: 工具执行完成 {"type": "tool_end", "tool": "get_current_time"}
    - done: 对话完成 {"type": "done", "messageId": 123}
    """
    settings = get_settings()
    agent_service = create_agent_service(db, settings)
    conv_service = ConversationService(db)

    try:
        await conv_service.ensure_owner(payload.conversationId, current.id)
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return JSONResponse(
            status_code=status.HTTP_403_FORBIDDEN,
            content=ApiResult.error("CONV-403", str(ex)).model_dump(),
        )

    async def event_generator():
        async for chunk in agent_service.stream(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
        ):
            yield f"data: {chunk}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.get("/tools", response_model=ApiResult[list[dict]])
async def list_tools() -> ApiResult[list[dict]]:
    """
    列出所有可用的工具及其描述。

    这个接口可以用于前端展示 Agent 的能力。
    """
    tools_info = []
    for tool in AVAILABLE_TOOLS:
        tools_info.append(
            {
                "name": tool.name,
                "description": tool.description,
            }
        )
    return ApiResult.ok(tools_info)
