package com.ynp.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ynp.agent.domain.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息表 Mapper。
 */
@Mapper
public interface MessageMapper extends MPJBaseMapper<MessageEntity> {
}
