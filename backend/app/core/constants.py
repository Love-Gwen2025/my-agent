"""
业务常量定义

集中管理业务中使用的魔法数字和字符串常量。
"""

# ==================== 系统标识 ====================

# AI 助手的发送者 ID（-1 表示系统/AI）
AI_SENDER_ID = -1


# ==================== 消息状态 ====================


class MessageStatus:
    """消息状态枚举"""

    PENDING = 0  # 待发送
    SENT = 1  # 已发送
    FAILED = 2  # 发送失败
    DELETED = 3  # 已删除


# ==================== 默认值 ====================

# 默认会话标题
DEFAULT_CONVERSATION_TITLE = "与聊天助手的对话"

# 标题最大长度
MAX_TITLE_LENGTH = 20


# ==================== 角色标识 ====================


class Role:
    """角色标识常量"""

    USER = "user"
    ASSISTANT = "assistant"
    SYSTEM = "system"
