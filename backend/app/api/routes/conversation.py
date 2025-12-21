from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.schema.conversation import (
    ConversationParam,
    ConversationVo,
    MessageSendParam,
    MessageVo,
)
from app.services.conversation_service import ConversationService

router = APIRouter(prefix="/conversation", tags=["会话"])


@router.post("/create/assistant", response_model=ApiResult[ConversationVo])
async def create_assistant_conversation(
    payload: ConversationParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[ConversationVo]:
    """
    1. 创建机器人会话，标题缺省时使用默认文案。
    2. 返回完整会话对象，便于前端直接使用。
    """
    service = ConversationService(db)
    # 1. 写入新会话并获取完整信息
    conv_id = await service.create_conversation(current.id, payload.title, payload.modelCode)
    conversation = await service.get_conversation(conv_id, current.id)
    return ApiResult.ok(ConversationVo(**conversation))


@router.get("/list", response_model=ApiResult[list[ConversationVo]])
async def list_conversations(
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[list[ConversationVo]]:
    """
    1. 查询当前用户会话列表。
    """
    service = ConversationService(db)
    # 1. 返回列表视图
    items = await service.list_conversations(current.id)
    return ApiResult.ok([ConversationVo(**item) for item in items])


@router.get("/history", response_model=ApiResult[list[MessageVo]])
async def history(
    conversationId: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[list[MessageVo]]:
    """
    查询会话历史消息（从数据库读取）
    
    始终从 t_message 表读取，确保返回的 id 是雪花 ID（可转为 int）。
    这样前端可以正确使用 message.id 进行分支导航和重新生成。
    """
    import logging
    
    logger = logging.getLogger(__name__)
    conv_service = ConversationService(db)
    try:
        # 1. 校验会话归属
        await conv_service.ensure_owner(int(conversationId), current.id)
        
        # 2. 始终从数据库获取消息（返回雪花 ID）
        items = await conv_service.history(current.id, int(conversationId))
        logger.info(f"[history] DB items count: {len(items)}, conversationId={conversationId}")
        
        return ApiResult.ok([MessageVo(**item) for item in items])
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        logger.error(f"[history] exception: {ex}, conversationId={conversationId}")
        # 出错时返回空列表
        return ApiResult.ok([])


@router.get("/{conversation_id}", response_model=ApiResult[ConversationVo])
async def get_conversation_detail(
    conversation_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[ConversationVo]:
    """
    1. 获取会话详情，校验归属。
    """
    service = ConversationService(db)
    try:
        vo = await service.get_conversation(int(conversation_id), current.id)
        return ApiResult.ok(ConversationVo(**vo))
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.post("/send", response_model=ApiResult[MessageVo])
async def send_message(
    payload: MessageSendParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[MessageVo]:
    """
    1. 同步消息发送并落库。
    """
    service = ConversationService(db)
    try:
        # 1. 校验归属并写入消息
        await service.ensure_owner(int(payload.conversationId), current.id)
        message = await service.persist_message(
            conversation_id=int(payload.conversationId),
            sender_id=current.id,
            role="user",
            content=payload.content,
            content_type=payload.contentType or "TEXT",
        )
        return ApiResult.ok(MessageVo(**message.to_vo()))
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


# /history 路由已移到 /{conversation_id} 之前定义


@router.patch("/modify", response_model=ApiResult[None])
async def modify_conversation(
    payload: ConversationParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[None]:
    """
    1. 修改会话标题。
    """
    service = ConversationService(db)
    try:
        # 1. 执行更新
        await service.modify_conversation(current.id, int(payload.id) if payload.id else None, payload.title)
        return ApiResult.ok()
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))


@router.delete("/{conversation_id}", response_model=ApiResult[None])
async def delete_conversation(
    conversation_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[None]:
    """
    1. 删除会话及其消息。
    """
    service = ConversationService(db)
    try:
        # 1. 删除会话及消息
        await service.delete_conversation(current.id, int(conversation_id))
        return ApiResult.ok()
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
