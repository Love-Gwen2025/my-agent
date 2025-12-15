from fastapi import APIRouter, Depends, Response, status
from fastapi.responses import JSONResponse, StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.schema.conversation import StreamChatParam
from app.services.chat_service import ChatService
from app.services.conversation_service import ConversationService
from app.services.model_service import ModelService

router = APIRouter(prefix="/chat", tags=["聊天"])


@router.post("/stream")
async def stream_chat(
    payload: StreamChatParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    1. 流式对话：校验会话后逐 token 输出。
    """
    conv_service = ConversationService(db)
    settings = get_settings()
    model_service = ModelService(settings) if settings.ai_deepseek_api_key else None
    chat_service = ChatService(conv_service, model_service)

    try:
        await conv_service.ensure_owner(payload.conversationId, current.id)
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return JSONResponse(
            status_code=status.HTTP_403_FORBIDDEN,
            content=ApiResult.error("CONV-403", str(ex)).model_dump(),
        )

    async def event_generator():
        # 1. 按顺序推送分片
        async for chunk in chat_service.stream(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
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
    1. 同步对话：校验会话归属并回显回复。
    """
    conv_service = ConversationService(db)
    settings = get_settings()
    model_service = ModelService(settings) if settings.ai_deepseek_api_key else None
    chat_service = ChatService(conv_service, model_service)
    try:
        # 1. 执行聊天并返回最终回复
        reply, _ = await chat_service.chat(
            user_id=current.id,
            conversation_id=payload.conversationId,
            content=payload.content,
            model_code=payload.modelCode,
        )
        return ApiResult.ok(reply)
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.get("/health", response_model=ApiResult[str])
async def health() -> ApiResult[str]:
    """
    1. 健康检查接口：便于在迁移过程中验证 Python 服务存活。
    """
    return ApiResult.ok("Chat service is healthy")
