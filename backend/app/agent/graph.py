"""
LangGraph Agent 构建模块 (v2)

新架构：
- RewriteNode: 代词消解
- ChatbotNode: 决定调用工具 or 直接回复
- ToolsNode: 执行工具（RAG/搜索等）

流程:
  START → rewrite → chatbot → [tools → chatbot]* → END

支持：
- checkpoint_id 分支（时间旅行）
- 工具自主调用（模型决定是否调用）
"""

import logging
from typing import Annotated, Literal

from langchain_core.messages import AIMessage, AnyMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from typing_extensions import TypedDict

from app.nodes.rewrite_node import create_rewrite_node

logger = logging.getLogger(__name__)

# ========== 1. 定义 Agent 状态 ==========


class AgentState(TypedDict):
    """
    Agent 的状态定义。
    
    Attributes:
        messages: 对话消息历史（使用 add_messages reducer 自动追加）
    """
    messages: Annotated[list[AnyMessage], add_messages]


# ========== 2. 路由逻辑 ==========


def tools_condition(state: AgentState) -> Literal["tools", "__end__"]:
    """
    条件路由：决定下一步是执行工具还是结束。
    """
    messages = state["messages"]
    last_message = messages[-1]

    if isinstance(last_message, AIMessage) and last_message.tool_calls:
        logger.info(f"🔧 Tool calls detected: {[tc['name'] for tc in last_message.tool_calls]}")
        return "tools"

    logger.debug("No tool calls, ending conversation")
    return "__end__"


# ========== 3. 构建 Agent 图 ==========


def create_agent_graph(
    model: ChatOpenAI,
    tools: list[BaseTool],
    checkpointer=None,
    enable_rewrite: bool = True,
) -> StateGraph:
    """
    创建 LangGraph Agent 工作流 (v2)。

    新流程:
    ```
           ┌─────────┐
           │  START  │
           └────┬────┘
                ▼
           ┌─────────┐
           │ rewrite │ (可选：代词消解)
           └────┬────┘
                ▼
           ┌─────────┐
    ┌─────►│ chatbot │◄────┐
    │      └────┬────┘     │
    │     有 tool_calls?   │
    │        Y     N       │
    │       ▼       ▼      │
    │   ┌───────┐  ┌────┐  │
    └───┤ tools │  │ END│  │
        └───────┘  └────┘
    ```

    Args:
        model: LLM 实例
        tools: 工具列表
        checkpointer: 可选的 checkpointer 用于状态持久化
        enable_rewrite: 是否启用代词消解节点

    Returns:
        编译后的 CompiledStateGraph
    """
    # 绑定工具到模型
    if tools:
        logger.info(f"🔧 Binding {len(tools)} tools to model: {[t.name for t in tools]}")
        model_with_tools = model.bind_tools(tools)
    else:
        logger.warning("⚠️ No tools provided to agent")
        model_with_tools = model

    # 定义 chatbot 节点
    async def chatbot(state: AgentState) -> dict:
        """Chatbot 节点：调用 LLM 获取回复或工具调用决策。"""
        logger.info(f"🤖 Chatbot receiving {len(state['messages'])} messages")
        response = await model_with_tools.ainvoke(state["messages"])
        logger.info(f"🤖 Chatbot response: has_tool_calls={bool(response.tool_calls)}, content_len={len(response.content) if response.content else 0}")
        if response.tool_calls:
            logger.info(f"🔧 Tool calls: {[tc['name'] for tc in response.tool_calls]}")
        return {"messages": [response]}

    # 创建 tools 节点
    tool_node = ToolNode(tools) if tools else None

    # 构建状态图
    workflow = StateGraph(AgentState)

    # 添加节点
    if enable_rewrite:
        rewrite_node = create_rewrite_node(model)
        workflow.add_node("rewrite", rewrite_node)
        workflow.add_node("chatbot", chatbot)
        if tool_node:
            workflow.add_node("tools", tool_node)

        # 设置入口点
        workflow.set_entry_point("rewrite")

        # rewrite -> chatbot
        workflow.add_edge("rewrite", "chatbot")
    else:
        workflow.add_node("chatbot", chatbot)
        if tool_node:
            workflow.add_node("tools", tool_node)

        # 设置入口点
        workflow.set_entry_point("chatbot")

    # 添加条件边
    if tool_node:
        workflow.add_conditional_edges(
            "chatbot",
            tools_condition,
            {
                "tools": "tools",
                "__end__": END,
            },
        )
        # tools -> chatbot
        workflow.add_edge("tools", "chatbot")
    else:
        # 没有工具，直接结束
        workflow.add_edge("chatbot", END)

    # 编译并返回
    return workflow.compile(checkpointer=checkpointer)


# ========== 4. 便捷工厂函数 ==========


def create_default_agent(
    model: ChatOpenAI,
    checkpointer=None,
    enable_rewrite: bool = True,
) -> StateGraph:
    """
    使用默认工具集创建 Agent。
    
    包含：
    - 时间/计算器工具
    - RAG 检索工具
    - Tavily 搜索工具（如果配置了 API Key）
    """
    from app.core.settings import get_settings
    from app.tools import AVAILABLE_TOOLS
    from app.tools.rag_tool import rag_search
    from app.tools.tavily_tool import web_search

    settings = get_settings()

    # 基础工具
    all_tools = list(AVAILABLE_TOOLS)

    # 添加 RAG 工具
    if settings.rag_enabled:
        all_tools.append(rag_search)

    # 添加 Tavily 搜索工具
    if settings.tavily_enabled and settings.tavily_api_key:
        all_tools.append(web_search)

    return create_agent_graph(
        model=model,
        tools=all_tools,
        checkpointer=checkpointer,
        enable_rewrite=enable_rewrite,
    )
