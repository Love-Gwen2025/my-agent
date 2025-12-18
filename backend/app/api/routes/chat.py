"""
聊天相关 API 路由 - 集成 RAG 和缓存
"""

from fastapi import APIRouter, Depends, Response, status
from fastapi.responses import JSONResponse, StreamingResponse
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.redis import get_redis
from app.core.settings import Settings, get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.schema.conversation import StreamChatParam
from app.services.chat_service import ChatService
from app.services.conversation_cache_service import ConversationCacheService
from app.services.conversation_service import ConversationService
from app.services.embedding_service import EmbeddingService
from app.services.model_service import ModelService

router = APIRouter(prefix="/chat", tags=["聊天"])


def create_chat_service(
    db: AsyncSession,
    redis: Redis,
    settings: Settings,
) -> ChatService:
    """
    创建 ChatService 实例，包含所有依赖
    """
    conv_service = ConversationService(db)
    model_service = ModelService(settings) if settings.ai_deepseek_api_key else None

    # 可选服务 - 根据配置启用
    embedding_service = None
    cache_service = None

    if settings.ai_openai_api_key or settings.ai_embedding_api_key:
        embedding_service = EmbeddingService(settings)

    if redis:
        cache_service = ConversationCacheService(redis, settings)

    return ChatService(
        conversation_service=conv_service,
        model_service=model_service,
        embedding_service=embedding_service,
        cache_service=cache_service,
        settings=settings,
    )


@router.post("/stream")
async def stream_chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    redis: Redis = Depends(get_redis),
    current: CurrentUser = Depends(get_current_user),
):
    """
    1. 流式对话：集成 RAG 和缓存，逐 token 输出。
    """
    settings = get_settings()
    chat_service = create_chat_service(db, redis, settings)
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
        async for chunk in chat_service.stream(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
            db=db,
        ):
            yield f"data: {chunk}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@router.post("/send", response_model=ApiResult[str])
async def chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    redis: Redis = Depends(get_redis),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[str]:
    """
    1. 同步对话：集成 RAG 和缓存，返回完整回复。
    """
    settings = get_settings()
    chat_service = create_chat_service(db, redis, settings)

    try:
        reply, _ = await chat_service.chat(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
            db=db,
        )
        return ApiResult.ok(reply)
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.get("/health", response_model=ApiResult[str])
async def health() -> ApiResult[str]:
    """
    1. 健康检查接口。
    """
    return ApiResult.ok("Chat service is healthy")
