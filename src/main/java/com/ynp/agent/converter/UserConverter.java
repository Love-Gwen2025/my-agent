package com.ynp.agent.converter;

import com.ynp.agent.config.AppProperties;
import com.ynp.agent.domain.entity.UserEntity;
import com.ynp.agent.vo.AccountVO;
import com.ynp.agent.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 用户相关转换器。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 1. Entity 转 VO。
     */
    UserVO toVO(UserEntity entity);

    /**
     * 1. 配置账号转 VO。
     */
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    AccountVO fromAccount(AppProperties.UserAccount account);
}
