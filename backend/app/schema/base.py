from typing import Generic, TypeVar

from pydantic import Field
from pydantic.generics import GenericModel

T = TypeVar("T")


class ApiResult(GenericModel, Generic[T]):
    """
    1. 统一的返回包装，保持与 Java 版 Result 结构一致，方便前端无感替换。
    """

    success: bool = Field(default=True, description="是否成功")
    code: str = Field(default="0", description="业务码，0 表示成功")
    message: str = Field(default="OK", description="提示信息")
    data: T | None = Field(default=None, description="返回数据")

    @classmethod
    def ok(cls, data: T | None = None, message: str = "OK") -> "ApiResult[T]":
        """
        1. 构造成功结果。
        """
        return cls(success=True, code="0", message=message, data=data)

    @classmethod
    def error(cls, code: str, message: str, data: T | None = None) -> "ApiResult[T]":
        """
        1. 构造失败结果。
        """
        return cls(success=False, code=code, message=message, data=data)
