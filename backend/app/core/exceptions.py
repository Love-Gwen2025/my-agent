"""
自定义异常类层次结构，用于统一业务异常处理。
"""
from typing import Any


class AppException(Exception):
    """
    应用异常基类，所有业务异常均继承此类。
    """

    def __init__(
        self,
        code: str,
        message: str,
        status_code: int = 400,
        data: Any | None = None,
    ):
        self.code = code
        self.message = message
        self.status_code = status_code
        self.data = data
        super().__init__(message)


class NotFoundError(AppException):
    """资源不存在异常"""

    def __init__(self, message: str = "资源不存在", code: str = "NOT_FOUND"):
        super().__init__(code=code, message=message, status_code=404)


class UnauthorizedError(AppException):
    """未授权异常"""

    def __init__(self, message: str = "未登录或登录已过期", code: str = "UNAUTHORIZED"):
        super().__init__(code=code, message=message, status_code=401)


class ForbiddenError(AppException):
    """无权限异常"""

    def __init__(self, message: str = "无权访问该资源", code: str = "FORBIDDEN"):
        super().__init__(code=code, message=message, status_code=403)


class ValidationError(AppException):
    """参数校验异常"""

    def __init__(self, message: str = "参数校验失败", code: str = "VALIDATION_ERROR"):
        super().__init__(code=code, message=message, status_code=422)


class BusinessError(AppException):
    """业务逻辑异常"""

    def __init__(self, message: str, code: str = "BUSINESS_ERROR"):
        super().__init__(code=code, message=message, status_code=400)
