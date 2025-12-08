package com.ynp.agent.model.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "存储用户登录状态的对象")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentSession {


    private Long id;
    /**
     * 用户编码
     */
    private String userCode;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户性别 0-男 1=女
     */
    private Integer userSex;

    /**
     * 用户手机号
     */
    private String userPhone;
    /**
     * 用户地址
     */
    private String address;

    private String token;
    /**
     * 登录时间戳（毫秒）
     */
    private Long loginTime;

    /**
     * 根据当前用户与令牌构造会话对象，方便写入 Redis
     *
     * @param user  当前登录用户
     * @param token 本次生成的 JWT
     * @return 包含用户基础信息的会话快照
     */
    public static CurrentSession of(User user, String token) {
        long now = System.currentTimeMillis();
        return CurrentSession.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .userName(user.getUserName())
                .userSex(user.getUserSex())
                .userPhone(user.getUserPhone())
                .address(user.getAddress())
                .token(token)
                .loginTime(now)
                .build();
    }

    /**
     * 根据 token 拼接会话存储使用的键
     * {}为redis中哈希值取值号，括号里为哈希值，sessionKey与indexKey需要保持一致
     * 目的是为了在同一哈希槽中，防止跨槽运算错误
     * @param token JWT 字符串
     * @return Redis 中的会话键
     */
    public static String sessionKey(String userId,String token) {
        return String.format("agent:user:{%s}:session:%s",userId,token);
    }

    /**
     * 根据用户编码拼接索引键，用于维护该用户所有在线 token
     *
     * @param userId 用户编码
     * @return Redis ZSet 索引键
     */
    public static String indexKey(String userId) {
        return String.format("agent:user:{%s}",userId);
    }
}
