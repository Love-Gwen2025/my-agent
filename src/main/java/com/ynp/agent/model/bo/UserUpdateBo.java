package com.ynp.agent.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>用户更新业务入参对象。</p>
 * <p>包含更新场景可变更的字段，未填写的字段在 MapStruct 更新时将被自动忽略。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateBo {

    /**
     * 用户主键，更新操作的定位依据。
     */
    private Long id;

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
     * 头像地址，可选更新。
     */
    private String avatar;
}
