"""
聊天相关 API 路由 - 使用 LangGraph 自动状态管理
"""

from fastapi import APIRouter, Depends, Response, status
from fastapi.responses import JSONResponse, StreamingResponse
from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import Settings, get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.schema.conversation import StreamChatParam
from app.services.chat_service import ChatService
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService

router = APIRouter(prefix="/chat", tags=["聊天"])


def create_chat_service(
    db: AsyncSession,
    settings: Settings,
) -> ChatService:
    """
    创建 ChatService 实例
    """
    conv_service = ConversationService(db)
    model_service = ModelService(settings) if settings.ai_deepseek_api_key else None

    # 可选服务 - 根据配置启用
    # 本地模式不需要 API Key，远程模式需要
    embedding_service = None
    use_local_embedding = settings.ai_embedding_provider == "local"
    has_remote_key = settings.ai_openai_api_key or settings.ai_embedding_api_key
    if use_local_embedding or has_remote_key:
        embedding_service = EmbeddingService(settings)

    return ChatService(
        conversation_service=conv_service,
        model_service=model_service,
        embedding_service=embedding_service,
        settings=settings,
    )


@router.post("/stream")
async def stream_chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    流式对话：使用 LangGraph 自动管理对话历史，逐 token 输出。
    """
    settings = get_settings()
    chat_service = create_chat_service(db, settings)
    conv_service = ConversationService(db)

    try:
        await conv_service.ensure_owner(int(payload.conversationId), current.id)
    except PermissionError as ex:
        logger.warning(f"Permission denied: user={current.id}, conversation={payload.conversationId}")
        response.status_code = status.HTTP_403_FORBIDDEN
        return JSONResponse(
            status_code=status.HTTP_403_FORBIDDEN,
            content=ApiResult.error("CONV-403", str(ex)).model_dump(),
        )

    async def event_generator():
        async for chunk in chat_service.stream(
            user_id=current.id,
            conversation_id=int(payload.conversationId),
            content=payload.content,
            model_code=payload.modelCode,
            regenerate=payload.regenerate,
            parent_message_id=int(payload.parentMessageId) if payload.parentMessageId else None,
            db=db,
        ):
            yield f"data: {chunk}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.post("/send", response_model=ApiResult[str])
async def chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[str]:
    """
    同步对话（已废弃，请使用 /stream）。
    为保持兼容性，此接口收集流式结果后一次返回。
    """
    settings = get_settings()
    chat_service = create_chat_service(db, settings)

    try:
        # 收集流式结果
        full_reply = []
        async for chunk in chat_service.stream(
            user_id=current.id,
            conversation_id=int(payload.conversationId),
            content=payload.content,
            model_code=payload.modelCode,
            regenerate=payload.regenerate,
            parent_message_id=int(payload.parentMessageId) if payload.parentMessageId else None,
            db=db,
        ):
            import json
            data = json.loads(chunk)
            if data.get("type") == "chunk":
                full_reply.append(data.get("content", ""))

        return ApiResult.ok("".join(full_reply))
    except PermissionError as ex:
        logger.warning(f"Permission denied: user={current.id}, conversation={payload.conversationId}")
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.get("/health", response_model=ApiResult[str])
async def health() -> ApiResult[str]:
    """
    健康检查接口。
    """
    return ApiResult.ok("Chat service is healthy")
