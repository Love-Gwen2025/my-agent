package com.ynp.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ynp.agent.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper extends MPJBaseMapper<UserEntity> {
}
