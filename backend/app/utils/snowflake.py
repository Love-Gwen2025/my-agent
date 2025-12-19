"""
雪花 ID 生成器

基于 Twitter Snowflake 算法的分布式唯一 ID 生成器。

结构（64 位）：
- 1 位：符号位（始终为 0）
- 41 位：时间戳（毫秒级，可用约 69 年）
- 10 位：机器 ID（支持 1024 个节点）
- 12 位：序列号（同一毫秒内可生成 4096 个 ID）

特点：
1. 趋势递增：按时间排序
2. 分布式安全：无需数据库协调
3. 高性能：本地生成，无网络开销
"""

import os
import threading
import time


class SnowflakeGenerator:
    """
    雪花 ID 生成器

    线程安全的分布式唯一 ID 生成器。

    Usage:
        from app.utils.snowflake import snowflake

        # 生成单个 ID
        id = snowflake.generate()

        # 批量生成
        ids = [snowflake.generate() for _ in range(10)]
    """

    # 起始时间戳（2024-01-01 00:00:00 UTC）
    EPOCH = 1704067200000

    # 各部分的位数
    MACHINE_ID_BITS = 10  # 机器 ID 位数
    SEQUENCE_BITS = 12    # 序列号位数

    # 最大值
    MAX_MACHINE_ID = (1 << MACHINE_ID_BITS) - 1  # 1023
    MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1      # 4095

    # 位移量
    MACHINE_ID_SHIFT = SEQUENCE_BITS
    TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS

    def __init__(self, machine_id: int | None = None):
        """
        初始化雪花 ID 生成器

        Args:
            machine_id: 机器 ID（0-1023），默认从环境变量 SNOWFLAKE_MACHINE_ID 读取，
                       如果未设置则使用 1
        """
        if machine_id is None:
            machine_id = int(os.getenv("SNOWFLAKE_MACHINE_ID", "1"))

        if not 0 <= machine_id <= self.MAX_MACHINE_ID:
            raise ValueError(f"Machine ID must be between 0 and {self.MAX_MACHINE_ID}")

        self.machine_id = machine_id
        self.sequence = 0
        self.last_timestamp = -1
        self._lock = threading.Lock()

    def _current_millis(self) -> int:
        """获取当前时间戳（毫秒）"""
        return int(time.time() * 1000)

    def _wait_next_millis(self, last_timestamp: int) -> int:
        """等待下一毫秒"""
        timestamp = self._current_millis()
        while timestamp <= last_timestamp:
            timestamp = self._current_millis()
        return timestamp

    def generate(self) -> int:
        """
        生成唯一的雪花 ID

        Returns:
            64 位整数 ID

        Raises:
            RuntimeError: 如果系统时钟回拨
        """
        with self._lock:
            timestamp = self._current_millis()

            # 时钟回拨检测
            if timestamp < self.last_timestamp:
                raise RuntimeError(
                    f"Clock moved backwards. Refusing to generate ID for "
                    f"{self.last_timestamp - timestamp} milliseconds"
                )

            # 同一毫秒内，序列号递增
            if timestamp == self.last_timestamp:
                self.sequence = (self.sequence + 1) & self.MAX_SEQUENCE
                # 序列号溢出，等待下一毫秒
                if self.sequence == 0:
                    timestamp = self._wait_next_millis(self.last_timestamp)
            else:
                # 新的毫秒，序列号重置
                self.sequence = 0

            self.last_timestamp = timestamp

            # 组装 ID
            snowflake_id = (
                ((timestamp - self.EPOCH) << self.TIMESTAMP_SHIFT)
                | (self.machine_id << self.MACHINE_ID_SHIFT)
                | self.sequence
            )

            return snowflake_id

    def parse(self, snowflake_id: int) -> dict:
        """
        解析雪花 ID

        Args:
            snowflake_id: 雪花 ID

        Returns:
            包含 timestamp, machine_id, sequence 的字典
        """
        sequence = snowflake_id & self.MAX_SEQUENCE
        machine_id = (snowflake_id >> self.MACHINE_ID_SHIFT) & self.MAX_MACHINE_ID
        timestamp = (snowflake_id >> self.TIMESTAMP_SHIFT) + self.EPOCH

        return {
            "timestamp": timestamp,
            "machine_id": machine_id,
            "sequence": sequence,
            "datetime": time.strftime(
                "%Y-%m-%d %H:%M:%S", time.localtime(timestamp / 1000)
            ),
        }


# 全局单例
snowflake = SnowflakeGenerator()


def generate_id() -> int:
    """
    生成唯一的雪花 ID（便捷函数）

    Returns:
        64 位整数 ID
    """
    return snowflake.generate()
