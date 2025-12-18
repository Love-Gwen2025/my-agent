"""
LangGraph Agent 构建模块

本模块实现了基于 LangGraph 的 ReAct Agent 架构：
- AgentState: 定义 Agent 的状态（消息历史等）
- chatbot: 调用 LLM 获取下一步动作（回复或工具调用）
- tools: 执行 LLM 请求的工具调用
- 条件边: 决定下一步是工具执行还是结束

设计原则：
1. 状态即一切：所有状态变化都通过 AgentState 传递
2. 节点无状态：每个节点函数是纯函数，只依赖传入的 state
3. 边控制流：通过条件边决定控制流走向

参考：https://langchain-ai.github.io/langgraph/concepts/agentic_concepts/#react-agent
"""

from typing import Annotated, Literal

from langchain_core.messages import AIMessage, AnyMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from typing_extensions import TypedDict

# ========== 1. 定义 Agent 状态 ==========


class AgentState(TypedDict):
    """
    Agent 的状态定义。

    在 LangGraph 中，状态是图的核心。每一步操作都会更新状态，
    并将新状态传递给下一步。

    Attributes:
        messages: 对话消息历史（使用 add_messages reducer 自动追加）

    关于 add_messages reducer：
        - 使用 Annotated[list, add_messages] 语法
        - 当节点返回 {"messages": [new_msg]} 时，会自动追加到列表
        - 而不是覆盖整个列表，这是 LangGraph 的核心设计
    """

    messages: Annotated[list[AnyMessage], add_messages]


# ========== 2. 路由逻辑 ==========


def tools_condition(state: AgentState) -> Literal["tools", "__end__"]:
    """
    条件路由：决定下一步是执行工具还是结束。

    检查最后一条 AI 消息是否包含 tool_calls：
    - 如果有 tool_calls -> 路由到 "tools" 节点执行工具
    - 如果没有 -> 路由到 END 结束对话

    Args:
        state: 当前 Agent 状态

    Returns:
        "tools" 或 "__end__"
    """
    # 获取最后一条消息
    messages = state["messages"]
    last_message = messages[-1]

    # 检查是否是 AI 消息且包含 tool_calls
    if isinstance(last_message, AIMessage) and last_message.tool_calls:
        return "tools"

    # 没有工具调用，结束对话
    return "__end__"


# ========== 3. 构建 Agent 图 ==========


def create_agent_graph(
    model: ChatOpenAI,
    tools: list[BaseTool],
) -> StateGraph:
    r"""
    创建 LangGraph Agent 工作流。

    这个函数构建一个 ReAct 风格的 Agent 图：

    ```
           ┌─────────┐
           │  START  │
           └────┬────┘
                ▼
           ┌─────────┐
    ┌─────►│ chatbot │◄────┐
    │      └────┬────┘     │
    │           │          │
    │      有 tool_calls?  │
    │        /     \       │
    │       是      否     │
    │       ▼       ▼      │
    │   ┌───────┐  ┌────┐  │
    └───┤ tools │  │ END│  │
        └───────┘  └────┘
    ```

    Args:
        model: 已绑定工具的 LLM 实例 (model.bind_tools(tools))
        tools: 工具列表

    Returns:
        编译后的 CompiledStateGraph，可直接调用 ainvoke/astream
    """

    # 3.1 定义 chatbot 节点
    async def chatbot(state: AgentState) -> dict:
        """
        Chatbot 节点：调用 LLM 获取回复或工具调用决策。

        这个节点不执行工具，只是让 LLM 决定下一步做什么。
        如果 LLM 需要调用工具，会返回包含 tool_calls 的 AIMessage。
        """
        # 将当前消息历史发送给 LLM
        response = await model.ainvoke(state["messages"])
        # 返回新消息，add_messages reducer 会自动追加
        return {"messages": [response]}

    # 3.2 创建 tools 节点
    # ToolNode 是 LangGraph 预构建的工具执行节点
    # 它会自动：
    #   1. 从最后一条 AIMessage 提取 tool_calls
    #   2. 并行执行所有工具调用
    #   3. 将结果包装成 ToolMessage 返回
    tool_node = ToolNode(tools)

    # 3.3 构建状态图
    workflow = StateGraph(AgentState)

    # 3.4 添加节点
    workflow.add_node("chatbot", chatbot)
    workflow.add_node("tools", tool_node)

    # 3.5 设置入口点
    workflow.set_entry_point("chatbot")

    # 3.6 添加边
    # chatbot -> 条件判断 -> (tools 或 END)
    workflow.add_conditional_edges(
        "chatbot",  # 源节点
        tools_condition,  # 条件函数
        {
            "tools": "tools",  # 如果返回 "tools"，跳转到 tools 节点
            "__end__": END,  # 如果返回 "__end__"，结束
        },
    )

    # tools -> chatbot (执行完工具后回到 chatbot 继续对话)
    workflow.add_edge("tools", "chatbot")

    # 3.7 编译并返回
    return workflow.compile()


# ========== 4. 便捷工厂函数 ==========


def create_default_agent(
    model: ChatOpenAI,
) -> StateGraph:
    """
    使用默认工具集创建 Agent。

    这是一个便利函数，自动加载 app.tools 中定义的所有工具。
    """
    from app.tools import AVAILABLE_TOOLS

    # 绑定工具到模型
    model_with_tools = model.bind_tools(AVAILABLE_TOOLS)

    # 创建并返回图
    return create_agent_graph(model_with_tools, AVAILABLE_TOOLS)
