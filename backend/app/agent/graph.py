"""
LangGraph Agent 构建模块 (v3 - 统一图架构)

新架构支持两种模式：
1. 普通对话模式 (mode="chat"):
   START → rewrite → chatbot → [tools → chatbot]* → END

2. 深度搜索模式 (mode="deep_search"):
   START → planning → [search → planning]* → summary → END

通过 state["mode"] 在入口处路由到不同的分支。

支持：
- checkpoint_id 分支（时间旅行）
- 工具自主调用（模型决定是否调用）
- DeepSearch 多轮搜索规划
"""

from typing import Annotated, Literal

from langchain_core.messages import AIMessage, AnyMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from loguru import logger
from typing_extensions import TypedDict

from app.nodes.chatbot_node import create_chatbot_node
from app.nodes.planning_node import create_planning_node
from app.nodes.rewrite_node import create_rewrite_node
from app.nodes.search_node import create_search_node
from app.nodes.summary_node import create_summary_node

# ========== 1. 定义 Agent 状态 ==========


class AgentState(TypedDict):
    """
    Agent 的状态定义 (v3 - 统一状态)。

    Attributes:
        messages: 对话消息历史（使用 add_messages reducer 自动追加）
        mode: 对话模式 ("chat" | "deep_search")
        question: 用户原始问题（DeepSearch 用）
        search_queries: 待搜索的关键词列表（DeepSearch 用）
        references: 累积的参考资料 {query: [results]}（DeepSearch 用）
        planning_rounds: 当前规划轮次（DeepSearch 用）
    """

    messages: Annotated[list[AnyMessage], add_messages]
    # DeepSearch 专用字段
    mode: str
    question: str
    search_queries: list[str]
    references: dict[str, list[str]]
    planning_rounds: int


# ========== 2. 路由逻辑 ==========


def mode_router(state: AgentState) -> Literal["rewrite", "planning"]:
    """
    入口路由：根据 mode 决定进入普通对话还是深度搜索。
    """
    mode = state.get("mode", "chat")
    if mode == "deep_search":
        logger.info("🔬 Entering DeepSearch mode")
        return "planning"
    else:
        logger.info("💬 Entering Chat mode")
        return "rewrite"


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


def planning_router(state: AgentState) -> Literal["search", "summary"]:
    """
    DeepSearch 规划路由：决定是继续搜索还是进入总结。
    """
    from app.core.settings import get_settings

    settings = get_settings()
    max_rounds = settings.deep_search_max_rounds

    planning_rounds = state.get("planning_rounds", 0)
    search_queries = state.get("search_queries", [])

    # 超过最大轮次，强制进入总结
    if planning_rounds >= max_rounds:
        logger.warning(f"⚠️ Max planning rounds ({max_rounds}) reached, forcing summary")
        return "summary"

    # 有搜索词，继续搜索
    if search_queries:
        logger.info(f"🔍 Search queries: {search_queries}")
        return "search"

    # 无搜索词，进入总结
    logger.info("✅ No more queries, proceeding to summary")
    return "summary"


# ========== 3. 构建统一 Agent 图 ==========


def create_agent_graph(
    model: ChatOpenAI,
    tools: list[BaseTool],
    checkpointer=None,
    enable_rewrite: bool = True,
) -> StateGraph:
    """
    创建 LangGraph Agent 工作流 (v3 - 统一图)。

    统一图架构:
    ┌───────────────────────────────────────────────────────┐
    │                        START                          │
    │                          │                            │
    │                       router                          │
    │                    ↙         ↘                        │
    │   ┌─────────────────┐   ┌─────────────────┐          │
    │   │  💬 Chat Mode   │   │  🔬 DeepSearch  │          │
    │   │                 │   │                 │          │
    │   │  rewrite        │   │  planning ◄──┐  │          │
    │   │     ↓           │   │     ↓        │  │          │
    │   │  chatbot ◄──┐   │   │  search? ────┘  │          │
    │   │     ↓       │   │   │     ↓           │          │
    │   │  tools? ────┘   │   │  summary        │          │
    │   │     ↓           │   │     ↓           │          │
    │   │    END          │   │    END          │          │
    │   └─────────────────┘   └─────────────────┘          │
    └───────────────────────────────────────────────────────┘

    Args:
        model: LLM 实例
        tools: 工具列表
        checkpointer: 可选的 checkpointer 用于状态持久化
        enable_rewrite: 是否启用代词消解节点

    Returns:
        编译后的 CompiledStateGraph
    """
    from app.core.settings import get_settings

    settings = get_settings()

    # 绑定工具到模型
    if tools:
        logger.info(f"🔧 Binding {len(tools)} tools to model: {[t.name for t in tools]}")
        model_with_tools = model.bind_tools(tools)
    else:
        logger.warning("⚠️ No tools provided to agent")
        model_with_tools = model

    # 创建节点
    tool_node = ToolNode(tools) if tools else None
    rewrite_node = create_rewrite_node(model) if enable_rewrite else None
    chatbot_node = create_chatbot_node(model_with_tools)
    planning_node = create_planning_node(model, settings)
    search_node = create_search_node(settings)
    summary_node = create_summary_node(model)

    # 构建状态图
    workflow = StateGraph(AgentState)

    # ===== 添加所有节点 =====
    # 普通对话分支
    if rewrite_node:
        workflow.add_node("rewrite", rewrite_node)
    workflow.add_node("chatbot", chatbot_node)
    if tool_node:
        workflow.add_node("tools", tool_node)

    # DeepSearch 分支
    workflow.add_node("planning", planning_node)
    workflow.add_node("search", search_node)
    workflow.add_node("summary", summary_node)

    # ===== 入口路由 =====
    workflow.set_entry_point("router")
    workflow.add_node("router", lambda state: state)  # 透传节点
    workflow.add_conditional_edges(
        "router",
        mode_router,
        {
            "rewrite": "rewrite" if rewrite_node else "chatbot",
            "planning": "planning",
        },
    )

    # ===== 普通对话分支边 =====
    if rewrite_node:
        workflow.add_edge("rewrite", "chatbot")

    if tool_node:
        workflow.add_conditional_edges(
            "chatbot",
            tools_condition,
            {
                "tools": "tools",
                "__end__": END,
            },
        )
        workflow.add_edge("tools", "chatbot")
    else:
        workflow.add_edge("chatbot", END)

    # ===== DeepSearch 分支边 =====
    workflow.add_conditional_edges(
        "planning",
        planning_router,
        {
            "search": "search",
            "summary": "summary",
        },
    )
    workflow.add_edge("search", "planning")  # 循环回到 planning
    workflow.add_edge("summary", END)

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
    all_tools.append(rag_search)

    # 添加 Tavily 搜索工具
    if settings.tavily_api_key:
        all_tools.append(web_search)

    return create_agent_graph(
        model=model,
        tools=all_tools,
        checkpointer=checkpointer,
        enable_rewrite=enable_rewrite,
    )
