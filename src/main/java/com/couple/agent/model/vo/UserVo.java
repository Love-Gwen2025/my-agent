package com.couple.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户返回视图")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVo {
    /**
     * 用户编码
     * */
    private String userCode;

    /**
     * 用户名
     * */
    private String userName;

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
}
