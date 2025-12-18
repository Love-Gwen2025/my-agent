"""
工具定义模块 - 包含所有可供 Agent 调用的工具

这个模块定义了 Agent 可以使用的各种工具 (Tools)。
在 LangChain 中，工具是使用 @tool 装饰器定义的函数，
Agent 可以决定何时以及如何调用这些工具来完成任务。
"""

from datetime import datetime, timedelta, timezone

from langchain_core.tools import tool

# ========== 示例工具：日期时间工具 ==========


@tool
def get_current_time(timezone_offset: int = 8) -> str:
    """
    获取当前的日期和时间。

    Args:
        timezone_offset: 时区偏移量（默认为 8，即北京时间 UTC+8）

    Returns:
        格式化的当前日期时间字符串，例如 "2024年12月17日 星期二 15:30:45"
    """
    # 计算指定时区的时间
    tz = timezone(timedelta(hours=timezone_offset))
    now = datetime.now(tz)

    # 中文星期映射
    weekdays = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    weekday = weekdays[now.weekday()]

    # 格式化输出
    return f"{now.year}年{now.month}月{now.day}日 {weekday} {now.strftime('%H:%M:%S')}"


@tool
def calculate_date_difference(date1: str, date2: str) -> str:
    """
    计算两个日期之间相差的天数。可用于计算纪念日、倒计时等。

    Args:
        date1: 第一个日期，格式为 YYYY-MM-DD
        date2: 第二个日期，格式为 YYYY-MM-DD

    Returns:
        两个日期之间相差的天数描述
    """
    try:
        d1 = datetime.strptime(date1, "%Y-%m-%d")
        d2 = datetime.strptime(date2, "%Y-%m-%d")
        diff = abs((d2 - d1).days)

        if d2 > d1:
            return f"从 {date1} 到 {date2} 共有 {diff} 天"
        else:
            return f"从 {date2} 到 {date1} 共有 {diff} 天"
    except ValueError as e:
        return f"日期格式错误，请使用 YYYY-MM-DD 格式。错误详情: {e}"


@tool
def simple_calculator(expression: str) -> str:
    """
    执行简单的数学计算。支持加减乘除和基本运算。

    Args:
        expression: 要计算的数学表达式，例如 "2 + 3 * 4" 或 "100 / 5"

    Returns:
        计算结果或错误信息
    """
    try:
        # 安全地评估数学表达式（仅允许数字和基本运算符）
        allowed_chars = set("0123456789+-*/.() ")
        if not all(c in allowed_chars for c in expression):
            return "错误：表达式包含不允许的字符。仅支持数字和 +, -, *, /, (, ), . 运算符"

        # 使用 eval 进行计算（已验证安全）
        result = eval(expression)  # noqa: S307
        return f"{expression} = {result}"
    except ZeroDivisionError:
        return "错误：除数不能为零"
    except Exception as e:
        return f"计算错误: {e}"


# ========== 工具列表：统一导出所有可用工具 ==========

# 这个列表会被 ModelService 使用，用于绑定到 LLM
AVAILABLE_TOOLS = [
    get_current_time,
    calculate_date_difference,
    simple_calculator,
]
