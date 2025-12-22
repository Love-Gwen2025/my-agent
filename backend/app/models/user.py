from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class User(Base):
    """
    1. 用户实体，对应 t_user 表。
    """

    __tablename__ = "t_user"

    user_code: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    user_name: Mapped[str] = mapped_column(String(100), nullable=False)
    user_password: Mapped[str] = mapped_column(String(255), nullable=False)
    user_sex: Mapped[int | None] = mapped_column(Integer, default=0)
    user_phone: Mapped[str | None] = mapped_column(String(20))
    address: Mapped[str | None] = mapped_column(String(500))
    max_login_num: Mapped[int] = mapped_column(Integer, default=3)
    avatar: Mapped[str | None] = mapped_column(String(500))

    def to_vo(self) -> dict:
        """
        1. 转为接口需要的用户视图。
        """
        return {
            "userCode": self.user_code,
            "userName": self.user_name,
            "userSex": self.user_sex,
            "userPhone": self.user_phone,
            "address": self.address,
            "maxLoginNum": self.max_login_num,
        }

    @property
    def id_str(self) -> str:
        """
        1. 返回字符串形式的主键，便于 Redis 组成键。
        """
        return str(self.id)
