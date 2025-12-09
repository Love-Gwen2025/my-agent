package com.couple.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.couple.agent.model.domain.EmbeddingTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 向量化任务 Mapper 接口
 *
 * @author ynp
 */
@Mapper
public interface EmbeddingTaskMapper extends MPJBaseMapper<EmbeddingTask> {
}
