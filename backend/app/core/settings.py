from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    1. 统一承载运行所需的环境变量，便于在 1C2G 场景下集中控制连接池与进程参数。
    2. 默认读取项目根目录下的 .env（位于 py-app/.env），避免干扰现有 Java 配置。
    """

    app_env: str = Field(default="dev", description="当前运行环境标识")
    app_name: str = Field(default="couple-agent-py", description="应用名称")
    app_host: str = Field(default="0.0.0.0", description="监听地址")
    app_port: int = Field(default=8080, description="监听端口")
    app_log_level: str = Field(default="info", description="日志级别")

    db_host: str = Field(default="localhost", description="PostgreSQL 主机地址")
    db_port: int = Field(default=5432, description="PostgreSQL 端口")
    db_name: str = Field(default="couple_agent", description="数据库名称")
    db_user: str = Field(default="postgres", description="数据库用户名")
    db_password: str = Field(default="postgres", description="数据库密码")
    db_pool_size: int = Field(default=5, description="数据库连接池大小")
    db_max_overflow: int = Field(default=5, description="数据库超出池后的最大连接数")

    redis_host: str = Field(default="localhost", description="Redis 主机地址")
    redis_port: int = Field(default=6379, description="Redis 端口")
    redis_password: str | None = Field(default=None, description="Redis 密码")
    redis_db: int = Field(default=0, description="Redis 库序号")

    jwt_secret: str = Field(default="change_me", description="JWT 密钥")
    jwt_expire_minutes: int = Field(default=60, description="JWT 过期分钟数")
    jwt_issuer: str = Field(default="couple-agent", description="JWT 颁发者")

    ai_openai_api_key: str | None = Field(default=None, description="OpenAI API Key")
    ai_openai_base_url: str = Field(default="https://api.openai.com/v1", description="OpenAI 基础地址")
    ai_openai_deployment_name: str | None = Field(default=None, description="OpenAI 模型或部署名称")
    ai_openai_temperature: float = Field(default=0.7, description="OpenAI 温度参数")
    ai_openai_timeout: int = Field(default=30, description="OpenAI 请求超时时间，秒")

    ai_deepseek_api_key: str | None = Field(default=None, description="DeepSeek API Key")
    ai_deepseek_base_url: str = Field(default="https://api.deepseek.com", description="DeepSeek 基础地址")
    ai_deepseek_model_name: str = Field(default="deepseek-chat", description="DeepSeek 模型名称")
    ai_deepseek_temperature: float = Field(default=0.7, description="DeepSeek 温度参数")
    ai_deepseek_timeout: int = Field(default=30, description="DeepSeek 请求超时时间，秒")

    oss_bucket: str | None = Field(default=None, description="OSS 存储桶名称")
    oss_region: str = Field(default="cn-hangzhou", description="OSS 地域")
    oss_endpoint: str | None = Field(default=None, description="OSS 访问域名")
    oss_custom_domain: str | None = Field(default=None, description="OSS 自定义对外域名")
    oss_object_prefix: str | None = Field(default=None, description="OSS 对象前缀")
    oss_avatar: str | None = Field(default=None, description="OSS 默认头像路径")

    enable_sse_streaming: bool = Field(default=True, description="是否启用 SSE 流式输出")
    enable_websocket: bool = Field(default=True, description="是否启用 WebSocket")

    # Embedding 配置
    ai_embedding_provider: str = Field(default="openai", description="Embedding 提供商: openai / deepseek")
    ai_embedding_model: str = Field(default="text-embedding-3-small", description="Embedding 模型名称")
    ai_embedding_dimension: int = Field(default=1536, description="Embedding 向量维度")
    ai_embedding_api_key: str | None = Field(default=None, description="Embedding API Key (默认复用 OpenAI key)")
    ai_embedding_base_url: str | None = Field(default=None, description="Embedding API Base URL")

    # 对话上下文配置
    conversation_cache_ttl: int = Field(default=3600, description="Redis 对话缓存 TTL (秒)")
    conversation_cache_max_messages: int = Field(default=20, description="缓存最近消息数量")
    rag_enabled: bool = Field(default=True, description="是否启用 RAG 检索")
    rag_top_k: int = Field(default=5, description="RAG 检索返回数量")

    model_config = SettingsConfigDict(
        env_file=str(Path(__file__).resolve().parents[3] / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    """
    1. 单例模式获取配置实例，使用 lru_cache 缓存避免重复创建。
    """
    return Settings()
