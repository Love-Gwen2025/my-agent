"""
阿里云 OSS 对象存储工具

提供文件上传、下载、删除等操作的封装。
使用单例模式管理 OSS 客户端连接。
"""

from functools import lru_cache
from pathlib import Path
from typing import BinaryIO

import alibabacloud_oss_v2 as oss

from app.core.settings import get_settings


class OSSClient:
    """阿里云 OSS 客户端封装"""

    def __init__(self):
        settings = get_settings()

        # 从配置文件读取凭证
        credentials_provider = oss.credentials.StaticCredentialsProvider(
            access_key_id=settings.oss_access_key_id,
            access_key_secret=settings.oss_access_key_secret,
        )

        # 配置客户端
        cfg = oss.config.load_default()
        cfg.credentials_provider = credentials_provider
        cfg.region = settings.oss_region

        # endpoint 用于上传（必须是 OSS 原生地址）
        if settings.oss_endpoint:
            cfg.endpoint = settings.oss_endpoint

        self._client = oss.Client(cfg)
        self._bucket = settings.oss_bucket
        self._prefix = settings.oss_object_prefix or ""
        # custom_domain 仅用于生成访问 URL（CDN/自定义域名只读）
        self._custom_domain = settings.oss_custom_domain

    def _build_key(self, key: str) -> str:
        """构建完整的对象键名（添加前缀）"""
        if self._prefix:
            return f"{self._prefix.rstrip('/')}/{key.lstrip('/')}"
        return key

    def upload_file(self, file_path: str | Path, key: str) -> dict:
        """
        上传本地文件到 OSS

        Args:
            file_path: 本地文件路径
            key: OSS 对象键名（不含前缀，前缀会自动添加）

        Returns:
            dict: 包含上传结果信息
        """
        full_key = self._build_key(key)

        result = self._client.put_object_from_file(
            oss.PutObjectRequest(
                bucket=self._bucket,
                key=full_key,
            ),
            str(file_path),
        )

        return {
            "success": result.status_code == 200,
            "key": full_key,
            "url": self.get_object_url(key),
            "etag": result.etag,
            "request_id": result.request_id,
        }

    def upload_bytes(
        self, data: bytes | BinaryIO, key: str, content_type: str | None = None
    ) -> dict:
        """
        上传字节数据到 OSS

        Args:
            data: 字节数据或文件对象
            key: OSS 对象键名
            content_type: MIME 类型（可选）

        Returns:
            dict: 包含上传结果信息
        """
        full_key = self._build_key(key)

        request = oss.PutObjectRequest(
            bucket=self._bucket,
            key=full_key,
            content_type=content_type,
            body=data,  # body 应该在 PutObjectRequest 内部
        )

        result = self._client.put_object(request)

        return {
            "success": result.status_code == 200,
            "key": full_key,
            "url": self.get_object_url(key),
            "etag": result.etag,
            "request_id": result.request_id,
        }

    def delete_object(self, key: str) -> bool:
        """
        删除 OSS 对象

        Args:
            key: OSS 对象键名

        Returns:
            bool: 是否删除成功
        """
        full_key = self._build_key(key)

        result = self._client.delete_object(
            oss.DeleteObjectRequest(
                bucket=self._bucket,
                key=full_key,
            )
        )

        return result.status_code in (200, 204)

    def get_object_url(self, key: str) -> str:
        """
        获取对象的访问 URL

        Args:
            key: OSS 对象键名

        Returns:
            str: 对象访问 URL
        """
        full_key = self._build_key(key)

        if self._custom_domain:
            return f"https://{self._custom_domain}/{full_key}"

        settings = get_settings()
        return f"https://{self._bucket}.oss-{settings.oss_region}.aliyuncs.com/{full_key}"

    def object_exists(self, key: str) -> bool:
        """
        检查对象是否存在

        Args:
            key: OSS 对象键名

        Returns:
            bool: 对象是否存在
        """
        full_key = self._build_key(key)

        try:
            result = self._client.head_object(
                oss.HeadObjectRequest(
                    bucket=self._bucket,
                    key=full_key,
                )
            )
            return result.status_code == 200
        except Exception:
            return False


@lru_cache
def get_oss_client() -> OSSClient:
    """获取 OSS 客户端单例"""
    return OSSClient()
