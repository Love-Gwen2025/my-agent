package com.ynp.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一错误响应。
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
}
