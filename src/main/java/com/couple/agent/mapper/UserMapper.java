package com.couple.agent.mapper;

import com.github.yulichang.base.MPJBaseMapper;

import com.couple.agent.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
}
