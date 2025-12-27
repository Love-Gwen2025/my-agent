from pydantic import BaseModel, Field


class UserLoginPayload(BaseModel):
    """
    1. 对应 Java 版 UserLoginParam，用户登录请求体。
    """

    userCode: str = Field(..., description="用户编码/登录账号")
    userPassword: str = Field(..., description="用户密码")


class UserParamPayload(BaseModel):
    """
    1. 对应 Java 版 UserParam，用户注册或修改请求体。
    """

    id: int | None = Field(default=None, description="用户 ID，修改时必填")
    userCode: str = Field(..., description="用户编码")
    userName: str | None = Field(default=None, description="用户昵称（可选，默认使用 userCode）")
    userPassword: str = Field(..., description="用户密码")
    userPhone: str | None = Field(default=None, description="手机号")
    address: str | None = Field(default=None, description="地址")
    userSex: int | None = Field(default=0, description="性别 0/1")


class UserVo(BaseModel):
    """
    1. 对应 Java 版 UserVo，前端所需返回体。
    """

    userCode: str = Field(..., description="用户编码")
    userName: str = Field(..., description="用户昵称")
    userSex: int | None = Field(default=None, description="性别 0/1")
    userPhone: str | None = Field(default=None, description="手机号")
    address: str | None = Field(default=None, description="地址")
    maxLoginNum: int | None = Field(default=None, description="最大登录设备数")
    avatar: str | None = Field(default=None, description="头像 URL")


class UserUpdatePayload(BaseModel):
    """用户信息更新请求体（不含密码）"""

    userName: str | None = Field(default=None, description="用户昵称")
    userPhone: str | None = Field(default=None, description="手机号")
    address: str | None = Field(default=None, description="地址")
    userSex: int | None = Field(default=None, description="性别 0/1")


class ChangePasswordPayload(BaseModel):
    """修改密码请求体"""

    oldPassword: str = Field(..., description="旧密码")
    newPassword: str = Field(..., min_length=6, description="新密码（至少6位）")
