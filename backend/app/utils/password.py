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


def verify_and_migrate(raw_password: str, stored_password: str) -> tuple[bool, str | None]:
    """
    1. 校验密码，兼容旧明文：
       - 若存储值以 $2 开头则按 bcrypt 校验；
       - 否则按明文比对，匹配则返回新的 bcrypt 哈希用于回写。
    """
    # 明文比对（不以 $2 开头）
    if not stored_password.startswith("$2"):
        if raw_password == stored_password:
            new_hash = hash_password(raw_password)
            return True, new_hash
        return False, None
    
    # bcrypt 校验
    try:
        password_bytes = raw_password.encode("utf-8")
        stored_bytes = stored_password.encode("utf-8")
        is_valid = bcrypt.checkpw(password_bytes, stored_bytes)
        return is_valid, None
    except Exception:
        # 如果哈希格式不兼容，尝试明文比对
        if raw_password == stored_password:
            new_hash = hash_password(raw_password)
            return True, new_hash
        return False, None
