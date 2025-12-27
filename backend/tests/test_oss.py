"""
OSS 集成测试 - 真实调用阿里云 OSS

运行方式：
    cd backend
    pytest tests/test_oss.py -v -s

注意：需要先配置好 .env 中的 OSS 相关环境变量
"""

import tempfile
import uuid
from pathlib import Path

import pytest

from app.utils.alioss_util import get_oss_client


class TestOSSIntegration:
    """OSS 集成测试"""

    @pytest.fixture
    def oss(self):
        """获取 OSS 客户端"""
        return get_oss_client()

    @pytest.fixture
    def test_file(self):
        """创建临时测试文件"""
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("Hello OSS Integration Test!")
            temp_path = Path(f.name)

        yield temp_path

        # 测试后清理本地文件
        temp_path.unlink(missing_ok=True)

    def test_upload_file(self, oss, test_file):
        """测试上传文件"""
        # 用 UUID 避免重复
        key = f"test/{uuid.uuid4().hex}.txt"

        result = oss.upload_file(test_file, key)

        print(f"上传结果: {result}")
        assert result["success"] is True
        assert result["url"] is not None

        # 清理：删除上传的文件
        oss.delete_object(key)

    def test_upload_bytes(self, oss):
        """测试上传字节数据"""
        key = f"test/{uuid.uuid4().hex}.txt"
        data = b"Hello from bytes!"

        result = oss.upload_bytes(data, key, content_type="text/plain")

        print(f"上传结果: {result}")
        assert result["success"] is True

        # 清理
        oss.delete_object(key)

    def test_object_exists(self, oss, test_file):
        """测试检查对象是否存在"""
        key = f"test/{uuid.uuid4().hex}.txt"

        # 上传前不存在
        assert oss.object_exists(key) is False

        # 上传
        oss.upload_file(test_file, key)

        # 上传后存在
        assert oss.object_exists(key) is True

        # 删除
        oss.delete_object(key)

        # 删除后不存在
        assert oss.object_exists(key) is False

    def test_get_object_url(self, oss):
        """测试获取 URL"""
        url = oss.get_object_url("avatars/test.jpg")

        print(f"生成的 URL: {url}")
        assert url.startswith("https://")
        assert "avatars/test.jpg" in url or "test.jpg" in url
