"""
Checkpoint API 路由

提供 checkpoint 历史和分支查询接口，用于前端实现消息分支导航 UI。
"""

from fastapi import APIRouter, Depends, Response, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import Settings, get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.schema.base import ApiResult
from app.services.checkpoint_service import CheckpointService
from app.services.conversation_service import ConversationService

router = APIRouter(prefix="/checkpoint", tags=["检查点"])


# ========== Schema ==========

class CheckpointVo(BaseModel):
    """Checkpoint 视图"""
    checkpointId: str = Field(..., description="Checkpoint ID")
    parentId: str | None = Field(default=None, description="父 Checkpoint ID")
    messageCount: int = Field(default=0, description="消息数量")


class SiblingCheckpointsVo(BaseModel):
    """兄弟 Checkpoints 视图"""
    current: int = Field(..., description="当前索引 (0-based)")
    total: int = Field(..., description="总数")
    siblings: list[str] = Field(default_factory=list, description="兄弟 Checkpoint ID 列表")


class CheckpointMessageVo(BaseModel):
    """Checkpoint 消息视图"""
    role: str = Field(..., description="消息角色")
    content: str = Field(..., description="消息内容")


# ========== Helper ==========

def create_checkpoint_service(settings: Settings) -> CheckpointService:
    """创建 CheckpointService 实例（无需 ModelService）"""
    return CheckpointService(settings=settings)


# ========== Routes ==========

@router.get("/{conversation_id}/history", response_model=ApiResult[list[CheckpointVo]])
async def get_checkpoint_history(
    conversation_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[list[CheckpointVo]]:
    """
    获取会话的 checkpoint 历史列表
    
    用于展示会话的执行历史和分支结构。
    """
    settings = get_settings()
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 获取 checkpoint 历史
        service = create_checkpoint_service(settings)
        history = await service.get_state_history(int(conversation_id))
        
        return ApiResult.ok([CheckpointVo(**item) for item in history])
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("CP-500", f"获取 checkpoint 历史失败: {ex}")


@router.get("/{conversation_id}/siblings", response_model=ApiResult[SiblingCheckpointsVo])
async def get_sibling_checkpoints(
    conversation_id: str,
    checkpoint_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[SiblingCheckpointsVo]:
    """
    获取指定 checkpoint 的兄弟分支
    
    用于实现前端的 "1/2" 分支导航 UI。
    返回同一 parent 下的所有 checkpoint 列表。
    """
    settings = get_settings()
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 获取兄弟 checkpoints
        service = create_checkpoint_service(settings)
        siblings = await service.get_sibling_checkpoints(int(conversation_id), checkpoint_id)
        
        return ApiResult.ok(SiblingCheckpointsVo(**siblings))
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("CP-500", f"获取兄弟 checkpoints 失败: {ex}")


@router.get("/{conversation_id}/messages", response_model=ApiResult[list[CheckpointMessageVo]])
async def get_checkpoint_messages(
    conversation_id: str,
    checkpoint_id: str,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
) -> ApiResult[list[CheckpointMessageVo]]:
    """
    获取指定 checkpoint 的消息列表
    
    用于切换分支时加载对应的消息内容。
    """
    settings = get_settings()
    conv_service = ConversationService(db)
    
    try:
        # 校验会话归属
        await conv_service.ensure_owner(int(conversation_id), current.id)
        
        # 获取 checkpoint 消息
        service = create_checkpoint_service(settings)
        messages = await service.get_checkpoint_messages(int(conversation_id), checkpoint_id)
        
        return ApiResult.ok([CheckpointMessageVo(**msg) for msg in messages])
    except PermissionError as ex:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("CONV-403", str(ex))
    except Exception as ex:
        response.status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        return ApiResult.error("CP-500", f"获取 checkpoint 消息失败: {ex}")
