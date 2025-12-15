from fastapi import APIRouter, Depends, File, Response, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db_session
from app.core.settings import get_settings
from app.dependencies.auth import CurrentUser, get_current_user
from app.core.redis import get_redis
from app.models.user import User
from app.schema.base import ApiResult
from app.schema.user import UserLoginPayload, UserParamPayload, UserVo
from app.services.auth_service import AuthService
from app.utils.session_store import SessionStore

router = APIRouter(prefix="/user", tags=["用户"])


@router.post("/register", response_model=ApiResult[int])
async def register_user(
    payload: UserParamPayload,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    redis=Depends(get_redis),
):
    """
    1. 用户注册：校验重复，写入 bcrypt 哈希。
    """
    settings = get_settings()
    store = SessionStore(redis, settings)
    service = AuthService(db, store, settings)
    try:
        # 1. 执行注册并返回主键
        user_id = await service.register(payload.model_dump())
        return ApiResult.ok(user_id)
    except ValueError as ex:
        response.status_code = status.HTTP_400_BAD_REQUEST
        return ApiResult.error("USER-400", str(ex))


@router.post("/update", response_model=ApiResult[UserVo])
async def update_user(
    payload: UserParamPayload,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    1. 用户信息修改：目前仅更新基本资料，密码修改可复用注册逻辑。
    """
    # 1. 读取用户并更新基础字段
    user: User | None = await db.get(User, current.id)
    if user is None:
        response.status_code = status.HTTP_404_NOT_FOUND
        return ApiResult.error("USER-404", "用户不存在")
    user.user_name = payload.userName or user.user_name
    user.user_phone = payload.userPhone or user.user_phone
    user.address = payload.address or user.address
    user.user_sex = payload.userSex if payload.userSex is not None else user.user_sex
    await db.commit()
    await db.refresh(user)
    vo = UserVo(
        userCode=user.user_code,
        userName=user.user_name,
        userSex=user.user_sex,
        userPhone=user.user_phone,
        address=user.address,
        maxLoginNum=user.max_login_num,
    )
    return ApiResult.ok(vo)


@router.post("/login", response_model=ApiResult[str])
async def login_user(
    payload: UserLoginPayload,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    redis=Depends(get_redis),
):
    """
    1. 用户登录：兼容明文/哈希，生成 JWT 并写入 Redis 会话。
    """
    settings = get_settings()
    store = SessionStore(redis, settings)
    service = AuthService(db, store, settings)
    try:
        token = await service.login(payload.model_dump())
        return ApiResult.ok(token)
    except ValueError as ex:
        response.status_code = status.HTTP_400_BAD_REQUEST
        return ApiResult.error("USER-401", str(ex))


@router.post("/logout", response_model=ApiResult[None])
async def logout_user(
    response: Response,
    current: CurrentUser = Depends(get_current_user),
    redis=Depends(get_redis),
):
    """
    1. 注销：删除 Redis 会话索引。
    """
    settings = get_settings()
    store = SessionStore(redis, settings)
    # 使用 CurrentUser 中存储的 token（从请求头获取）
    await store.remove_session(str(current.id), current.token)
    return ApiResult.ok()


@router.get("/me", response_model=ApiResult[UserVo])
async def get_current_user_info(
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    1. 获取当前登录用户信息，无需传入 user_id。
    """
    user: User | None = await db.get(User, current.id)
    if user is None:
        raise ValueError("用户不存在")
    vo = UserVo(
        userCode=user.user_code,
        userName=user.user_name,
        userSex=user.user_sex,
        userPhone=user.user_phone,
        address=user.address,
        maxLoginNum=user.max_login_num,
    )
    return ApiResult.ok(vo)


@router.get("/detail/{user_id}", response_model=ApiResult[UserVo])
async def get_user_detail(
    user_id: int,
    response: Response,
    db: AsyncSession = Depends(get_db_session),
    current: CurrentUser = Depends(get_current_user),
):
    """
    1. 查询用户详情：仅允许本人查询。
    """
    # 1. 校验用户存在与归属
    user: User | None = await db.get(User, user_id)
    if user is None:
        response.status_code = status.HTTP_404_NOT_FOUND
        return ApiResult.error("USER-404", "用户不存在")
    if current.id != user.id:
        response.status_code = status.HTTP_403_FORBIDDEN
        return ApiResult.error("USER-403", "无权查看其他用户信息")
    vo = UserVo(
        userCode=user.user_code,
        userName=user.user_name,
        userSex=user.user_sex,
        userPhone=user.user_phone,
        address=user.address,
        maxLoginNum=user.max_login_num,
    )
    return ApiResult.ok(vo)


@router.post("/upload/image", response_model=ApiResult[str])
async def upload_image(file: UploadFile = File(...), response: Response = None) -> ApiResult[str]:
    """
    1. 占位实现：上传 OSS/本地存储逻辑尚未迁移。
    """
    response.status_code = status.HTTP_501_NOT_IMPLEMENTED
    return ApiResult.error("NOT_IMPLEMENTED", "上传逻辑待迁移")
