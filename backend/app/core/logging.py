"""
结构化日志配置模块。
开发环境使用彩色控制台输出，生产环境支持 JSON 格式。
"""

import logging
import sys
from typing import Literal

from app.core.settings import get_settings


def setup_logging(
    level: str = "INFO",
    format_type: Literal["console", "json"] = "console",
) -> None:
    """
    配置应用日志。

    Args:
        level: 日志级别 (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        format_type: 输出格式 (console: 彩色控制台, json: JSON 格式)
    """
    settings = get_settings()
    log_level = getattr(logging, level.upper(), logging.INFO)

    # 清除现有 handlers
    root_logger = logging.getLogger()
    root_logger.handlers.clear()

    # 创建 handler
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(log_level)

    if format_type == "json":
        # 生产环境 JSON 格式
        formatter = logging.Formatter(
            '{"timestamp": "%(asctime)s", "level": "%(levelname)s", '
            '"logger": "%(name)s", "message": "%(message)s"}'
        )
    else:
        # 开发环境彩色输出
        formatter = logging.Formatter(
            "\033[90m%(asctime)s\033[0m | %(levelname)-8s | \033[36m%(name)s\033[0m - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )

    handler.setFormatter(formatter)
    root_logger.addHandler(handler)
    root_logger.setLevel(log_level)

    # 降低第三方库日志级别
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("sqlalchemy.engine").setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    """
    获取指定名称的 logger 实例。

    Args:
        name: logger 名称，通常使用 __name__

    Returns:
        logging.Logger 实例
    """
    return logging.getLogger(name)
