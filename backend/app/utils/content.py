"""
内容处理工具函数

提供统一的内容提取和格式化功能，处理不同 AI 模型返回的多种格式。
"""


def extract_text_content(content) -> str:
    """
    从模型响应中提取纯文本内容。

    处理多种格式：
    - 字符串: 直接返回
    - 列表[dict]: Gemini 格式 [{'type': 'text', 'text': '...'}]
    - 列表[str]: 拼接返回
    - None: 返回空字符串

    Args:
        content: 模型返回的原始内容

    Returns:
        str: 提取后的纯文本内容

    Examples:
        >>> extract_text_content("hello")
        'hello'
        >>> extract_text_content([{'type': 'text', 'text': 'world'}])
        'world'
        >>> extract_text_content(None)
        ''
    """
    # 空值处理
    if content is None:
        return ""

    # 字符串直接返回
    if isinstance(content, str):
        return content

    # 列表格式处理（如 Gemini 返回的格式）
    if isinstance(content, list):
        texts = []
        for part in content:
            if isinstance(part, dict):
                # Gemini 格式: {'type': 'text', 'text': '...'}
                if part.get("type") == "text" and "text" in part:
                    texts.append(part["text"])
                # 有时只有 text 字段
                elif "text" in part:
                    texts.append(part["text"])
            elif isinstance(part, str):
                texts.append(part)
            else:
                # 其他类型转为字符串
                texts.append(str(part))
        return "".join(texts)

    # 其他类型兜底处理
    return str(content)
