"""
头像上传接口集成测试

运行方式：
    cd backend
    uv run pytest tests/test_upload.py -v -s

注意：需要先配置好 .env 中的 OSS 相关环境变量
"""

import uuid

import pytest

from app.utils.alioss_util import get_oss_client


class TestAvatarUpload:
    """头像上传集成测试"""

    @pytest.fixture
    def oss(self):
        """获取 OSS 客户端"""
        return get_oss_client()

    @pytest.fixture
    def fake_image_bytes(self):
        """创建一个最小的有效 PNG 图片（1x1 像素透明）"""
        # 最小有效 PNG 文件的字节序列
        return bytes(
            [
                0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,  # PNG 签名
                0x00,
                0x00,
                0x00,
                0x0D,
                0x49,
                0x48,
                0x44,
                0x52,  # IHDR 块
                0x00,
                0x00,
                0x00,
                0x01,
                0x00,
                0x00,
                0x00,
                0x01,  # 1x1 像素
                0x08,
                0x06,
                0x00,
                0x00,
                0x00,
                0x1F,
                0x15,
                0xC4,
                0x89,
                0x00,
                0x00,
                0x00,
                0x0A,
                0x49,
                0x44,
                0x41,  # IDAT 块
                0x54,
                0x78,
                0x9C,
                0x63,
                0x00,
                0x01,
                0x00,
                0x00,
                0x05,
                0x00,
                0x01,
                0x0D,
                0x0A,
                0x2D,
                0xB4,
                0x00,
                0x00,
                0x00,
                0x00,
                0x49,
                0x45,
                0x4E,
                0x44,
                0xAE,  # IEND 块
                0x42,
                0x60,
                0x82,
            ]
        )

    def test_upload_avatar_to_oss(self, oss, fake_image_bytes):
        """测试直接上传头像到 OSS"""
        # 生成唯一 key
        key = f"avatars/test/{uuid.uuid4().hex}.png"

        # 上传
        result = oss.upload_bytes(fake_image_bytes, key, content_type="image/png")

        print(f"上传结果: {result}")
        assert result["success"] is True
        assert result["url"] is not None
        assert "avatars" in result["url"]

        # 清理
        oss.delete_object(key)

    def test_upload_bytes_content_type(self, oss):
        """测试上传时指定 content-type"""
        key = f"avatars/test/{uuid.uuid4().hex}.txt"
        data = b"Hello Avatar Test"

        result = oss.upload_bytes(data, key, content_type="text/plain")

        assert result["success"] is True

        # 清理
        oss.delete_object(key)
