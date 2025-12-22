"""
密码哈希工具 - 直接使用 bcrypt 库
"""

import bcrypt


def hash_password(raw_password: str) -> str:
    """
    1. 使用 bcrypt 生成密码哈希。
    """
    password_bytes = raw_password.encode("utf-8")
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password_bytes, salt)
    return hashed.decode("utf-8")


def verify_password(raw_password: str, stored_password: str) -> bool:
    """
    1. 使用 bcrypt 校验密码是否匹配。
    """
    password_bytes = raw_password.encode("utf-8")
    stored_bytes = stored_password.encode("utf-8")
    return bcrypt.checkpw(password_bytes, stored_bytes)
