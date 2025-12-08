package com.ynp.agent.exception;

/**
 * <p>错误码抽象接口。</p>
 * <p>约束所有错误码都必须提供唯一编码、默认提示语以及建议的 HTTP 状态码，便于统一返回和网关治理。</p>
 */
public interface ErrorCode {

    /**
     * @return 业务唯一错误码，例如 {@code USER-404}。
     */
    String getCode();

    /**
     * @return 默认提示语，作为接口响应和日志输出的兜底信息。
     */
    String getDefaultMessage();

    /**
     * @return 建议的 HTTP 状态码，供控制层设置响应状态或网关自动下发。
     */
    int getHttpStatus();
}
