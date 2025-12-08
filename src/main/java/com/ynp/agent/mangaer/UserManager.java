package com.ynp.agent.mangaer;


import com.ynp.agent.model.domain.User;

import java.util.List;

public interface UserManager {
    /**
     * 校验指定用户是否全部存在。
     */
    boolean allExist(List<Long> memberIds);

    /**
     * 根据用户编码查询用户信息。
     *
     * @param userCode 用户编码
     * @return 用户实体，可能为 null
     */
    User selectByUserCode(String userCode);

    /**
     * 插入用户信息。
     *
     * @param user 用户实体
     * @return 主键ID
     */
    Long insertUser(User user);

    /**
     * 根据主键查询用户。
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    User selectById(Long userId);

    /**
     * 更新整条用户记录。
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int updateUser(User user);

}
