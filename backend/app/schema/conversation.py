"""
Pydantic Schema 定义

注意: 所有雪花 ID 使用 str 类型，避免 JavaScript 大整数精度丢失
"""

from pydantic import BaseModel, Field


class ConversationParam(BaseModel):
    """
    1. 对应 Java 版 ConversationParam，用于会话创建或修改。
    """

    id: str | None = Field(default=None, description="会话 ID，可为空")
    title: str | None = Field(default=None, description="会话标题")
    modelCode: str | None = Field(default=None, description="默认模型编码")


class MessageSendParam(BaseModel):
    """
    1. 对应 Java 版 MessageSendParam，用于非流式消息发送。
    """

    conversationId: str = Field(..., description="会话 ID")
    content: str = Field(..., description="消息内容")
    contentType: str | None = Field(default="TEXT", description="消息类型")
    replyTo: str | None = Field(default=None, description="引用消息 ID")


class StreamChatParam(BaseModel):
    """
    1. 对应 Java 版 StreamChatParam，用于流式对话。
    """

    conversationId: str = Field(..., description="会话 ID")
    content: str = Field(..., description="用户消息内容")
    modelCode: str | None = Field(default=None, description="模型编码")
    systemPrompt: str | None = Field(default=None, description="系统提示词")
    checkpointId: str | None = Field(default=None, description="检查点 ID，用于分支/时间旅行")


class MessageVo(BaseModel):
    """
    1. 对应 Java 版 MessageVo/HistoryMessageVo 的核心字段，用于统一返回消息。
    """

    id: str = Field(..., description="消息 ID")
    conversationId: str = Field(..., description="会话 ID")
    senderId: str = Field(..., description="发送者 ID")
    role: str = Field(..., description="角色 user/assistant")
    content: str = Field(..., description="消息内容")
    contentType: str = Field(default="TEXT", description="消息类型")
    modelCode: str | None = Field(default=None, description="模型编码")
    tokenCount: int | None = Field(default=None, description="Token 数")
    createTime: str | None = Field(default=None, description="创建时间 ISO8601")


class ConversationVo(BaseModel):
    """
    1. 对应 Java 版 ConversationVo，描述会话概要。
    """

    id: str = Field(..., description="会话 ID")
    title: str | None = Field(default=None, description="会话标题")
    userId: str = Field(..., description="拥有者 ID")
    modelCode: str | None = Field(default=None, description="默认模型编码")
    lastMessageId: str | None = Field(default=None, description="最后一条消息 ID")
    lastMessageAt: str | None = Field(default=None, description="最后消息时间 ISO8601")
    avatar: str | None = Field(default=None, description="会话头像")


class StreamChatEvent(BaseModel):
    """
    1. 对应 Java 版 StreamChatEvent，用于 SSE 返回。
    """

    type: str = Field(..., description="事件类型：chunk/done/error")
    content: str | None = Field(default=None, description="内容片段")
    messageId: str | None = Field(default=None, description="消息 ID")
    conversationId: str | None = Field(default=None, description="会话 ID")
    error: str | None = Field(default=None, description="错误信息")
    tokenCount: int | None = Field(default=None, description="Token 使用量")
