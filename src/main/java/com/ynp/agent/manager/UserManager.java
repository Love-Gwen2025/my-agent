package com.ynp.agent.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ynp.agent.domain.entity.UserEntity;
import com.ynp.agent.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 用户相关数据访问。
 */
@Service
public class UserManager extends BaseManager {

    private final UserMapper userMapper;

    public UserManager(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 1. 根据用户名获取用户。
     */
    public UserEntity findByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        /* 1. 按用户名精确匹配 */
        wrapper.eq(UserEntity::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 1. 根据 ID 获取用户。
     */
    public UserEntity findById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 1. 创建用户。
     */
    public UserEntity createUser(UserEntity user) {
        /* 1. 插入用户记录 */
        userMapper.insert(user);
        return user;
    }

    /**
     * 1. 更新用户密码哈希。
     */
    public void updatePassword(Long userId, String passwordHash) {
        UserEntity entity = userMapper.selectById(userId);
        if (Objects.isNull(entity)) {
            return;
        }
        /* 1. 写入新哈希 */
        entity.setPasswordHash(passwordHash);
        userMapper.updateById(entity);
    }

    /**
     * 1. 更新用户展示名。
     */
    public void updateDisplayName(Long userId, String displayName) {
        UserEntity entity = userMapper.selectById(userId);
        if (Objects.isNull(entity)) {
            return;
        }
        /* 1. 更新展示名 */
        entity.setDisplayName(displayName);
        userMapper.updateById(entity);
    }

    /**
     * 1. 列出所有用户。
     */
    public List<UserEntity> listAll() {
        /* 1. 查询全量用户 */
        return userMapper.selectList(new LambdaQueryWrapper<>());
    }
}
