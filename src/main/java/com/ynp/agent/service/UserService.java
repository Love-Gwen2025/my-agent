package com.ynp.agent.service;


import com.ynp.agent.model.bo.UserCreateBo;
import com.ynp.agent.model.bo.UserUpdateBo;
import com.ynp.agent.model.domain.User;
import com.ynp.agent.model.param.UserLoginParam;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

public interface UserService  {

    /**
     * 注册新用户。
     *
     * @param user 用户注册业务对象
     * @return 新用户 ID
     */
    Long createUser(UserCreateBo user);

    /**
     * 更新用户信息。
     *
     * @param user 用户更新业务对象
     * @return 更新后的用户实体
     */
    User updateUser(UserUpdateBo user);

    /**
     * 执行登录并返回令牌。
     *
     * @param inParam 登录参数
     * @return JWT 字符串
     */
    String login(@Valid UserLoginParam inParam);

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    User getUserDetail(Long id);

    /**
     * 上传用户头像并返回访问地址。
     *
     * @param file 头像文件
     * @return 头像访问 URL
     */
    String uploadImage(MultipartFile file);
}
