package com.ynp.agent.mangaer.impl;

import com.github.yulichang.wrapper.MPJLambdaWrapper;

import com.ynp.agent.mangaer.BaseManager;
import com.ynp.agent.mangaer.UserManager;
import com.ynp.agent.model.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagerImpl extends BaseManager implements UserManager {
    @Override
    public boolean allExist(List<Long> memberIds) {
        return userMapper.selectCount(new MPJLambdaWrapper<User>()
                .in(User::getId, memberIds)) == memberIds.size();
    }

    @Override
    public User selectByUserCode(String userCode) {
        return userMapper.selectOne(new MPJLambdaWrapper<User>()
                .eq(User::getUserCode, userCode));
    }

    @Override
    public Long insertUser(User user) {
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public User selectById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public int updateUser(User user) {
        return userMapper.updateById(user);
    }
}
