package com.couple.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@TableName("t_user")
@NoArgsConstructor
@AllArgsConstructor
public class User extends BasePo {

    /**
     * 用户编码
     * */
    private String userCode;

    /**
     * 用户名
     * */
    private String userName;

    /**
     * 用户密码
     * */
    private String userPassword;

    /**
     * 用户性别 0-男 1=女
     * */
    private Integer userSex;

    /**
     * 用户手机号
     * */
    private String userPhone;
    /**
     * 用户地址
     * */
    private String address;
    /**
     * 最大设备数
     * */
    private Integer maxLoginNum;

    /**
     * 用户头像，阿里云存储
     * */
    private String avatar;
}
