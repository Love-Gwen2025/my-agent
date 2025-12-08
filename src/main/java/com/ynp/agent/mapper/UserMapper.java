package com.ynp.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;

import com.ynp.agent.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
}
