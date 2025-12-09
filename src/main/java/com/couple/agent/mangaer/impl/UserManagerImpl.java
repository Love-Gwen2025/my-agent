package com.couple.agent.mangaer.impl;

import com.github.yulichang.wrapper.MPJLambdaWrapper;

import com.couple.agent.mangaer.BaseManager;
import com.couple.agent.mangaer.UserManager;
import com.couple.agent.model.domain.User;
import org.springframework.stereotype.Service;

@Service
public class UserManagerImpl extends BaseManager implements UserManager {
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
