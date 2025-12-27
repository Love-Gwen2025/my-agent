"""
Pytest fixtures for testing.
"""

import sys
from pathlib import Path

# 确保 app 模块可被导入
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import pytest


@pytest.fixture
async def client():
    """
    创建异步测试客户端。

    延迟导入 app.main 以避免在不需要时触发应用初始化。
    """
    from httpx import ASGITransport, AsyncClient
    from app.main import app

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
