"""
Checkpoint 查询服务

提供 LangGraph checkpoint 历史查询功能，用于实现消息分支导航。
所有方法直接使用 checkpointer 读取数据，无需实例化 Agent。
"""

import logging
from typing import Any

from app.core.checkpointer import create_checkpointer
from app.core.settings import Settings

logger = logging.getLogger(__name__)


class CheckpointService:
    """
    LangGraph Checkpoint 查询服务
    
    直接使用 checkpointer 读取数据，无需实例化 Agent，性能更高。
    """
    
    def __init__(self, settings: Settings):
        self.settings = settings
    
    async def get_state_history(
        self, 
        conversation_id: int, 
        limit: int = 50
    ) -> list[dict[str, Any]]:
        """
        获取会话的 checkpoint 历史列表（直接读取 checkpointer）
        
        Args:
            conversation_id: 会话 ID
            limit: 返回数量限制
            
        Returns:
            checkpoint 列表
        """
        config = {"configurable": {"thread_id": str(conversation_id)}}
        
        async with create_checkpointer(self.settings) as checkpointer:
            history = []
            # 直接使用 checkpointer.alist 遍历历史
            async for checkpoint_tuple in checkpointer.alist(config, limit=limit):
                checkpoint = checkpoint_tuple.get("checkpoint", {})
                parent_checkpoint = checkpoint_tuple.get("parent_checkpoint", {})
                
                # 获取消息数量
                channel_values = checkpoint.get("channel_values", {})
                messages = channel_values.get("messages", [])
                
                history.append({
                    "checkpointId": checkpoint.get("id"),
                    "parentId": parent_checkpoint.get("id") if parent_checkpoint else None,
                    "messageCount": len(messages),
                })
                
            return history
    
    async def get_sibling_checkpoints(
        self, 
        conversation_id: int, 
        checkpoint_id: str
    ) -> dict[str, Any]:
        """
        获取指定 checkpoint 的兄弟分支（同一 parent 下的所有分支）
        
        用于实现 "1/2" 这样的分支导航 UI
        
        Args:
            conversation_id: 会话 ID
            checkpoint_id: 当前 checkpoint ID
            
        Returns:
            {
                "current": 0,  # 当前索引 (0-based)
                "total": 2,    # 总数
                "siblings": ["cp_id_1", "cp_id_2"]  # 所有兄弟 ID
            }
        """
        history = await self.get_state_history(conversation_id)
        
        # 找到当前 checkpoint
        current = next(
            (h for h in history if h["checkpointId"] == checkpoint_id), 
            None
        )
        
        if not current:
            return {
                "current": 0,
                "total": 1,
                "siblings": [checkpoint_id],
            }
        
        parent_id = current["parentId"]
        
        # 找到所有具有相同 parent 的 checkpoints
        siblings = [
            h["checkpointId"] 
            for h in history 
            if h["parentId"] == parent_id
        ]
        
        if checkpoint_id not in siblings:
            siblings.append(checkpoint_id)
        
        try:
            current_index = siblings.index(checkpoint_id)
        except ValueError:
            current_index = 0
        
        return {
            "current": current_index,
            "total": len(siblings),
            "siblings": siblings,
        }
    
    async def get_checkpoint_messages(
        self, 
        conversation_id: int, 
        checkpoint_id: str
    ) -> list[dict[str, Any]]:
        """
        获取指定 checkpoint 的消息列表（直接读取 checkpointer）
        
        Args:
            conversation_id: 会话 ID
            checkpoint_id: checkpoint ID
            
        Returns:
            消息列表
        """
        config = {
            "configurable": {
                "thread_id": str(conversation_id),
                "checkpoint_id": checkpoint_id,
            }
        }
        
        async with create_checkpointer(self.settings) as checkpointer:
            result = await checkpointer.aget(config)
            
            if not result:
                return []
            
            # 从 checkpoint 提取消息
            channel_values = result.get("channel_values", {})
            messages = channel_values.get("messages", [])
            
            # 复用 _transform_messages 方法
            return self._transform_messages(messages, str(conversation_id))

    async def get_latest_messages(
        self, 
        conversation_id: int
    ) -> list[dict[str, Any]]:
        """
        直接从 Checkpointer 读取状态，无需构建 Graph 实例，性能更高。
        
        Args:
            conversation_id: 会话 ID
            
        Returns:
            消息列表，格式与前端 Message 类型兼容
        """
        config = {"configurable": {"thread_id": str(conversation_id)}}
        
        async with create_checkpointer(self.settings) as checkpointer:
            # 直接调用 checkpointer.aget 获取最新 checkpoint
            checkpoint_tuple = await checkpointer.aget(config)
            
            if not checkpoint_tuple:
                return []
            
            # aget 返回的是 dict: {'channel_values': {'messages': [...]}, ...}
            messages = checkpoint_tuple.get("channel_values", {}).get("messages", [])
            
            return self._transform_messages(messages, str(conversation_id))
    
    def _transform_messages(
        self, 
        messages: list[Any], 
        conversation_id: str
    ) -> list[dict[str, Any]]:
        """
        独立的消息转换逻辑，遵循单一职责原则
        """
        result = []
        
        for idx, msg in enumerate(messages):
            # 1. 基础属性提取（兼容 dict 或 Object）
            msg_type = getattr(msg, "type", None) or (msg.get("type") if isinstance(msg, dict) else None)
            content = getattr(msg, "content", "") or (msg.get("content", "") if isinstance(msg, dict) else "")
            
            # 2. 角色映射
            if msg_type == "human":
                role = "user"
                sender_id = "0"
            elif msg_type == "ai":
                role = "assistant"
                sender_id = "-1"
            elif msg_type == "system":
                # 不显示系统提示词
                continue
            elif msg_type == "tool":
                # 不显示工具调用结果
                continue
            else:
                role = "unknown"
                sender_id = "unknown"
            
            # 3. 处理工具调用请求（AI 消息可能只有 tool_calls 没有 content）
            if role == "assistant" and not content:
                tool_calls = getattr(msg, "tool_calls", [])
                if tool_calls:
                    # 有工具调用但无内容的中间消息，跳过
                    continue
                else:
                    # 既没内容也没工具调用的空消息，跳过
                    continue
            
            # 4. 构造 DTO，使用稳定的消息 ID
            msg_id = getattr(msg, "id", None) or f"{conversation_id}_{idx}"
            
            # 尝试获取时间戳
            additional_kwargs = getattr(msg, "additional_kwargs", {}) or {}
            create_time = additional_kwargs.get("created_at")
            
            result.append({
                "id": str(msg_id),
                "conversationId": conversation_id,
                "senderId": sender_id,
                "role": role,
                "content": content,
                "contentType": "TEXT",
                "createTime": create_time,
            })
        
        return result
