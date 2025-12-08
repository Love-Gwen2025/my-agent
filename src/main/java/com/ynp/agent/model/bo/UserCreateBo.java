package com.ynp.agent.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>用户新增业务入参对象。</p>
 * <p>用于在控制层与服务层之间传递注册场景所需的核心字段，便于后续扩展校验或默认值处理。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateBo {

    /**
     * 用户编码。
     */
    private String userCode;

    /**
     * 用户名。
     */
    private String userName;

    /**
     * 登录密码。
     */
    private String userPassword;

    /**
     * 手机号。
     */
    private String userPhone;

    /**
     * 联系地址。
     */
    private String address;

    /**
     * 性别，0-男 1-女。
     */
    private Integer userSex;

    /**
     * 最大在线设备数。
     */
    private Integer maxLoginNum;

    /**
     * 头像地址，可为空。
     */
    private String avatar;
}
