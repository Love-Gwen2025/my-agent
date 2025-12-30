"""
API Key 加密工具

使用 Fernet 对称加密保护用户 API Key。
密钥从 jwt_secret 通过 PBKDF2 派生，无需额外配置。
"""

import base64
import hashlib

from cryptography.fernet import Fernet

from app.core.settings import get_settings


def _derive_key(secret: str) -> bytes:
    """
    从 jwt_secret 派生 Fernet 密钥

    使用 SHA-256 哈希将任意长度的 secret 转换为 32 字节密钥，
    然后 base64 编码为 Fernet 要求的格式。

    Args:
        secret: JWT 密钥字符串

    Returns:
        32 字节的 base64 编码密钥
    """
    # 使用 SHA-256 生成 32 字节哈希
    key_bytes = hashlib.sha256(secret.encode()).digest()
    # Fernet 要求 base64 编码的 32 字节密钥
    return base64.urlsafe_b64encode(key_bytes)


def get_cipher() -> Fernet:
    """
    获取加密器实例

    Returns:
        Fernet 加密器
    """
    settings = get_settings()
    key = _derive_key(settings.jwt_secret)
    return Fernet(key)


def encrypt_api_key(api_key: str) -> str:
    """
    加密 API Key

    Args:
        api_key: 明文 API Key

    Returns:
        加密后的字符串（base64 编码）
    """
    cipher = get_cipher()
    encrypted = cipher.encrypt(api_key.encode())
    return encrypted.decode()


def decrypt_api_key(encrypted: str) -> str:
    """
    解密 API Key

    Args:
        encrypted: 加密后的 API Key 字符串

    Returns:
        明文 API Key
    """
    cipher = get_cipher()
    decrypted = cipher.decrypt(encrypted.encode())
    return decrypted.decode()
