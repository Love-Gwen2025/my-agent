package com.ynp.agent.converter;

import com.ynp.agent.domain.entity.ConversationEntity;
import com.ynp.agent.vo.ConversationVO;
import org.mapstruct.Mapper;

/**
 * 会话转换器。
 */
@Mapper(componentModel = "spring")
public interface ConversationConverter {

    /**
     * 1. Entity 转 VO。
     */
    ConversationVO toVO(ConversationEntity entity);
}
