package com.ynp.agent.controller;


import com.ynp.agent.model.dto.Result;
import com.ynp.agent.model.param.UserLoginParam;
import com.ynp.agent.model.param.UserParam;
import com.ynp.agent.model.vo.UserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@Tag(name = "用户登录模块", description = "用户注册、登录、校验接口")
public class UserController extends BaseController{

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Long> register(@Validated @RequestBody UserParam inParam){
        return Result.ok(userService.createUser(userConverter.param2CreateBo(inParam)));
    }
    @PostMapping("/update")
    @Operation(summary = "用户修改")
    public Result<UserVo> update(@Validated @RequestBody UserParam inParam) {
        return Result.ok(userConverter.entity2Vo(userService.updateUser(userConverter.param2UpdateBo(inParam))));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<String> login(@Valid @RequestBody UserLoginParam inParam) {
        return  Result.ok(userService.login(inParam));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户注销")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.ok();
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "用户详情")
    public Result<UserVo> getUserDetail(@PathVariable(value = "id")Long id){
        return Result.ok(userConverter.entity2Vo(userService.getUserDetail(id)));
    }
    @PostMapping("/upload/image")
    @Operation(summary = "头像上传")
    public Result<String> uploadImage( @RequestParam(value = "file") MultipartFile file){
        return Result.ok(userService.uploadImage(file));
    }
}
