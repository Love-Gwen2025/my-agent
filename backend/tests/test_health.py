"""
健康检查接口测试。
"""
import pytest


@pytest.mark.asyncio
async def test_chat_health(client):
    """测试聊天服务健康检查接口"""
    response = await client.get("/chat/health")
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["code"] == "0"
    assert "healthy" in data["data"].lower()


@pytest.mark.asyncio
async def test_docs_available(client):
    """测试 API 文档可访问"""
    response = await client.get("/docs")
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_openapi_schema(client):
    """测试 OpenAPI schema 可获取"""
    response = await client.get("/openapi.json")
    assert response.status_code == 200
    data = response.json()
    assert "openapi" in data
    assert "paths" in data
