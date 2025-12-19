from datetime import datetime

from sqlalchemy import BigInteger, DateTime, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.utils.snowflake import generate_id


class Base(DeclarativeBase):
    """
    统一基类：提供主键与时间字段，保持与 PostgreSQL 表结构的一致性。

    主键使用雪花 ID 算法生成，具有以下特点：
    1. 分布式安全：无需数据库协调，各节点独立生成
    2. 趋势递增：按时间排序，适合索引
    3. 不暴露信息：难以猜测和遍历
    """

    # 使用雪花 ID 作为主键，在 Python 层生成
    id: Mapped[int] = mapped_column(
        BigInteger,
        primary_key=True,
        default=generate_id,  # 每次插入时调用 generate_id() 生成
        autoincrement=False,  # 禁用数据库自增
    )
    create_time: Mapped[datetime] = mapped_column(
        DateTime(timezone=False),
        server_default=func.now(),
        nullable=False,
    )
    update_time: Mapped[datetime] = mapped_column(
        DateTime(timezone=False),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )
    version: Mapped[int] = mapped_column(default=0)

