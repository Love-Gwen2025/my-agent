package com.couple.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import com.couple.agent.model.bo.UserCreateBo;
import com.couple.agent.model.bo.UserUpdateBo;
import com.couple.agent.model.domain.CurrentSession;
import com.couple.agent.model.domain.User;
import com.couple.agent.model.param.UserLoginParam;
import com.couple.agent.service.BaseService;
import com.couple.agent.service.UserService;
import com.couple.agent.utils.SessionUtil;
import com.couple.agent.utils.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserServiceImpl extends BaseService implements UserService {
    /**
     * 用户注册：负责完成参数校验、默认值补全及入库。
     */
    @Override
    public Long createUser(UserCreateBo user) {
        if (Objects.isNull(user)) {
            throw new BizException(BizErrorCode.USER_BAD_REQUEST, "用户注册参数不能为空");
        }
        if (!StringUtils.hasText(user.getUserPassword())) {
            throw new BizException(BizErrorCode.USER_BAD_REQUEST, "密码不能为空");
        }
        User entity = userConverter.createBo2Entity(user);
        // 若调用方未显式指定头像，则尝试落默认头像
        if (!StringUtils.hasText(entity.getAvatar())) {
            String avatarPath = ossProperties.getAvatar();
            if (StringUtils.hasText(avatarPath)) {
                String normalizedPath = avatarPath.startsWith("/") ? avatarPath : "/" + avatarPath;
                String defaultAvatar = ossProperties.resolvePublicDomain() + normalizedPath;
                if (osssUtil.exists(defaultAvatar)) {
                    entity.setAvatar(defaultAvatar);
                }
            }
        }
        return userManager.insertUser(entity);
    }

    /**
     * 用户资料更新：合并变更字段并执行持久化。
     */
    @Override
    public User updateUser(UserUpdateBo user) {
        if (Objects.isNull(user) || Objects.isNull(user.getId())) {
            throw new BizException(BizErrorCode.USER_BAD_REQUEST, "用户 ID 不能为空");
        }
        User dbUser = userManager.selectById(user.getId());
        if (Objects.isNull(dbUser)) {
            throw new BizException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        userConverter.updateEntityFromBo(user, dbUser);
        int affected = userManager.updateUser(dbUser);
        if (affected <= 0) {
            throw new BizException(BizErrorCode.USER_NOT_FOUND, "用户不存在或更新失败");
        }
        return dbUser;
    }


    /**
     * 用户登录：验证凭证、生成 JWT、写入会话并控制最大设备数
     * 如果账号密码对了，登录是一定要登陆上的，除非被禁
     */
    @Override
    public String login(UserLoginParam inParam) {
        // 依据用户编码查询账号
        User user = userManager.selectByUserCode(inParam.getUserCode());
        // 账号不存在时直接抛错
        if (Objects.isNull(user)) {
            throw new BizException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        //校验是否被禁止登录
        if (Objects.isNull(user.getMaxLoginNum()) || user.getMaxLoginNum() <= 0) {
            throw new BizException(BizErrorCode.USER_FORBIDDEN, "该用户被禁止登录");
        }
        // 校验密码是否匹配
        if (!StringUtil.equal(inParam.getUserPassword(), user.getUserPassword())) {
            throw new BizException(BizErrorCode.USER_UNAUTHORIZED, "密码错误");
        }
        // 登录成功，生成令牌，并将令牌等登录态存放redis（按 token 唯一键支持多端共存）
        String jwt = jwtUtil.generateToken(user);
        CurrentSession session = CurrentSession.of(user, jwt);
        // 插入登录会话，调整redis中的会话数与数据库匹配
        enforceDeviceLimit(user, session);
        return jwt;
    }


    @Override
    public User getUserDetail(Long id) {
        return userManager.selectById(id);
    }

    /**
     * 用户上传图片
     */
    @Override
    public String uploadImage(MultipartFile file) {
        // 校验文件是否为空，避免出现空文件上传的情况
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_BAD_REQUEST, "上传文件不能为空");
        }
        // 获取当前登录会话，头像上传必须建立在登录态之上
        CurrentSession session = SessionUtil.get();
        if (Objects.isNull(session) || Objects.isNull(session.getId())) {
            throw new BizException(BizErrorCode.AUTH_UNAUTHORIZED, "用户未登录或会话已过期");
        }
        // 获取原始文件名并截取后缀，作为构造对象路径的依据
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.lastIndexOf('.') >= 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        // 将用户 ID + 随机串拼接成 OSS 对象路径，保证同一用户多次上传不会互相覆盖
        String objectKey = String.format("avatar/%s/%s%s", session.getId(), UUID.randomUUID(), extension);
        try (InputStream inputStream = file.getInputStream()) {
            // 将文件流上传至 OSS，返回可直接访问的 URL
            String imageUrl = osssUtil.uploadSimpleObject(objectKey, inputStream, file.getSize(), file.getContentType());
            // 写入数据库，保证用户基础信息与 OSS 中的资源保持一致
            User user = userManager.selectById(session.getId());
            user.setAvatar(imageUrl);
            int affected = userManager.updateUser(user);
            if (affected <= 0) {
                throw new BizException(BizErrorCode.USER_NOT_FOUND, "用户不存在，无法更新头像");
            }
            return imageUrl;
        } catch (IOException ex) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_SERVER_ERROR, "读取上传文件失败", ex);
        }
    }


    /**
     * 写入会话并根据最大设备数淘汰旧终端
     */
    private void enforceDeviceLimit(User user, CurrentSession session) {
        String userId = String.valueOf(user.getId());
        String indexKey = CurrentSession.indexKey(userId);
        String sessionKey = CurrentSession.sessionKey(userId, session.getToken());
        long ttlSeconds = Duration.ofMinutes(jwtProperties.getExpirationMinutes()).getSeconds();
        if (ttlSeconds <= 0) {
            ttlSeconds = 60;
        }
        long loginTime = session.getLoginTime() != null ? session.getLoginTime() : System.currentTimeMillis();
        session.setLoginTime(loginTime);
        String sessionJson;
        try {
            sessionJson = objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化会话信息失败", e);
        }
        redisTemplate.execute(
                loginLimitScript,
                List.of(indexKey, sessionKey),
                String.valueOf(user.getMaxLoginNum()),
                String.valueOf(loginTime),
                sessionJson,
                String.valueOf(ttlSeconds),
                "UNLINK"
        );
    }

}
