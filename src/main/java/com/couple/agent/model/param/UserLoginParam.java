package com.couple.agent.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginParam {
    @NotBlank(message = "登录账号不能为空")
    public String userCode;
    @NotBlank(message = "密码不能为空")
    public String userPassword;
}
