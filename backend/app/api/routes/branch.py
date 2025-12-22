"""
分支管理 API 路由

提供消息分支查询和导航接口，用于前端实现消息分支导航 UI。
"""

from fastapi import APIRouter, Depends, Response, status
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.services.conversation_service import ConversationService

router = APIRouter(prefix="/branch", tags=["分支管理"])


# ========== Schema ==========

class SiblingMessagesVo(BaseModel):
    """兄弟消息视图"""
    current: int = Field(..., description="当前索引 (0-based)")
    total: int = Field(..., description="总数")
    siblings: list[str] = Field(default_factory=list, description="兄弟消息 ID 列表")


# ========== Routes ==========

@router.get("/{conversation_id}/siblings", response_model=ApiResult[SiblingMessagesVo])
async def get_message_siblings(
    conversation_id: str,
    message_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[SiblingMessagesVo]:
    """
    获取消息的兄弟分支（基于 SQL parent_id 查询）
    
    使用消息表的 parent_id 字段查询同父消息，
    比基于 checkpoint 的查询更准确、更高效。
    
    Args:
        conversation_id: 会话 ID
        message_id: 当前消息 ID
    
    Returns:
        {
            "current": 0,  # 当前消息在兄弟中的索引
            "total": 2,    # 兄弟总数
            "siblings": ["msg_id_1", "msg_id_2"]  # 所有兄弟消息 ID
        }
    """
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 使用 SQL 查询获取兄弟消息
        siblings = await conv_service.get_sibling_messages(int(message_id))
        
        return ApiResult.ok(SiblingMessagesVo(**siblings))
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("BRANCH-500", f"获取兄弟消息失败: {ex}")


@router.get("/{conversation_id}/message/{message_id}", response_model=ApiResult[dict])
async def get_message_by_id(
    conversation_id: str,
    message_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[dict]:
    """
    根据 ID 获取消息详情
    
    用于切换分支时加载指定消息内容。
    """
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 获取消息
        message = await conv_service.get_message_by_id(int(message_id))
        
        if not message:
            response.status_code = status.HTTP_404_NOT_FOUND
            return ApiResult.error("MSG-404", "消息不存在")
        
        return ApiResult.ok(message.to_vo())
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("BRANCH-500", f"获取消息失败: {ex}")


class SetCurrentMessageParam(BaseModel):
    """设置当前消息参数"""
    messageId: str = Field(..., description="当前选中的消息 ID")


@router.put("/{conversation_id}/current", response_model=ApiResult[str])
async def set_current_message(
    conversation_id: str,
    payload: SetCurrentMessageParam,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[str]:
    """
    保存用户选择的当前分支消息 ID
    
    当用户通过分支导航器切换分支时调用此接口，
    刷新页面后会恢复到用户选择的分支。
    """
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 保存当前消息 ID
        await conv_service.set_current_message(
            int(conversation_id), 
            int(payload.messageId)
        )
        
        return ApiResult.ok("保存成功")
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("BRANCH-500", f"保存失败: {ex}")
