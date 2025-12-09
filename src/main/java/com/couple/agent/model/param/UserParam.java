package com.couple.agent.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户注册/修改参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserParam {
    private Long id;

    private String userCode;

    private String userName;

    private String userPassword;

    private String userPhone;

    private String address;

    private Integer userSex;

}
