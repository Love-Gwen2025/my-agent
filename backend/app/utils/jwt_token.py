from datetime import UTC, datetime, timedelta
from typing import Any

from jose import JWTError, jwt

from app.core.settings import Settings


def create_access_token(user_id: int, user_name: str, settings: Settings) -> tuple[str, datetime]:
    """
    1. 生成 JWT，包含过期时间与基础身份信息。
    """
    now = datetime.now(UTC)
    expire = now + timedelta(minutes=settings.jwt_expire_minutes)
    payload = {
        "sub": str(user_id),
        "userId": str(user_id),
        "userName": user_name,
        "iat": int(now.timestamp()),
        "exp": int(expire.timestamp()),
        "iss": settings.jwt_issuer,
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")
    return token, expire


def decode_token(token: str, settings: Settings) -> dict[str, Any] | None:
    """
    1. 解析 JWT，异常返回 None，避免抛出 500。
    """
    try:
        return jwt.decode(
            token, settings.jwt_secret, algorithms=["HS256"], options={"verify_aud": False}
        )
    except JWTError:
        return None
