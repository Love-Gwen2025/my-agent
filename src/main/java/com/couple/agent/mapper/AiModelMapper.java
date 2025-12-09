package com.couple.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.couple.agent.model.domain.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型配置 Mapper 接口
 *
 * @author ynp
 */
@Mapper
public interface AiModelMapper extends MPJBaseMapper<AiModel> {
}
