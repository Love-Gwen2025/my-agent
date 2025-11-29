package com.ynp.agent.converter;

import com.ynp.agent.domain.entity.AttachmentEntity;
import com.ynp.agent.vo.AttachmentVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 附件转换器。
 */
@Mapper(componentModel = "spring")
public interface AttachmentConverter {

    /**
     * 1. Entity 转 VO。
     */
    @Mapping(target = "url", ignore = true)
    AttachmentVO toVO(AttachmentEntity entity);
}
