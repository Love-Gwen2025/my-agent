from typing import Annotated

from fastapi import Depends, Header, HTTPException, status

from app.core.redis import get_redis
from app.core.settings import get_settings
from app.utils.jwt_token import decode_token
from app.utils.session_store import SessionStore


class CurrentUser:
    """
    1. 描述当前登录用户的会话视图。
    """

    def __init__(self, payload: dict):
        self.id: int = int(payload.get("id"))
        self.user_code: str = payload.get("userCode", "")
        self.user_name: str = payload.get("userName", "")
        self.user_sex: int | None = payload.get("userSex")
        self.user_phone: str | None = payload.get("userPhone")
        self.address: str | None = payload.get("address")
        self.token: str = payload.get("token", "")


async def get_current_user(
    token: Annotated[str | None, Header()] = None,
    authorization: Annotated[str | None, Header()] = None,
    redis=Depends(get_redis),
):
    """
    1. 解析 `token` 或 `Authorization` 头，校验 JWT 并从 Redis 获取会话，未通过则返回 401。
    """
    raw_token: str | None = None
    if token:
        raw_token = token.strip()
    elif authorization:
        raw = authorization.strip()
        raw_token = raw.split(" ", 1)[1].strip() if raw.lower().startswith("bearer ") else raw
    if not raw_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="未登录")
    # 1. 解析 JWT
    settings = get_settings()
    claims = decode_token(raw_token, settings)
    if claims is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="token 无效或已过期")
    user_id = claims.get("userId") or claims.get("sub")
    # 2. 校验 Redis 会话
    store = SessionStore(redis, settings)
    session = await store.load_session(str(user_id), raw_token)
    if session is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="会话不存在或已过期")
    return CurrentUser(session)
