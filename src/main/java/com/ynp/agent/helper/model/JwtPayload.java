package com.ynp.agent.helper.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * JWT 解析结果。
 */
@Data
@AllArgsConstructor
public class JwtPayload {

    private Long userId;
    private String username;
    private String displayName;
    private Instant expireAt;
}
