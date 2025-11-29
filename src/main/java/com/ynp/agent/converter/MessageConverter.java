package com.ynp.agent.converter;

import com.ynp.agent.domain.entity.MessageEntity;
import com.ynp.agent.vo.HistoryMessageVO;
import org.mapstruct.Mapper;

/**
 * 消息转换器。
 */
@Mapper(componentModel = "spring")
public interface MessageConverter {

    /**
     * 1. Entity 转历史 VO（附件后续手工设置）。
     */
    HistoryMessageVO toHistoryVO(MessageEntity entity);
}
