package com.ynp.agent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一返回结果")
public record Result<T>(
        @Schema(description = "是否成功")
        boolean success,
        @Schema(description = "业务码，0 表示成功")
        String code,
        @Schema(description = "提示消息")
        String message,
        @Schema(description = "数据载体")
        T data
) {

    private static final String SUCCESS_CODE = "0";
    private static final String SUCCESS_MESSAGE = "OK";

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(true, SUCCESS_CODE, message, data);
    }

    public static Result<Void> ok() {
        return new Result<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static Result<Void> error(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    public static <T> Result<T> error(String code, String message, T data) {
        return new Result<>(false, code, message, data);
    }
}
