package com.ynp.agent.exception;

import lombok.Getter;

/**
 * <p>自定义业务异常。</p>
 * <p>通过承载 {@link ErrorCode} 及可选的覆盖提示信息，帮助调用方快速定位问题并输出结构化响应。</p>
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 错误码实例，包含唯一编码与默认提示语。
     */
    private final ErrorCode errorCode;

    /**
     * 实际返回给前端的提示信息，默认取错误码的默认提示，可在构造函数中覆盖。
     */
    private final String message;

    /**
     * 使用错误码默认提示语构造业务异常。
     *
     * @param errorCode 业务错误码
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.message = errorCode.getDefaultMessage();
    }

    /**
     * 使用自定义提示语构造业务异常。
     *
     * @param errorCode       业务错误码
     * @param overrideMessage 自定义提示语
     */
    public BizException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage);
        this.errorCode = errorCode;
        this.message = overrideMessage;
    }

    /**
     * 使用自定义提示语和根因异常构造业务异常。
     *
     * @param errorCode       业务错误码
     * @param overrideMessage 自定义提示语
     * @param cause           原始异常
     */
    public BizException(ErrorCode errorCode, String overrideMessage, Throwable cause) {
        super(overrideMessage, cause);
        this.errorCode = errorCode;
        this.message = overrideMessage;
    }

    /**
     * 使用默认提示语及根因异常构造业务异常。
     *
     * @param errorCode 业务错误码
     * @param cause     原始异常
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.message = errorCode.getDefaultMessage();
    }

    /**
     * @return 业务错误码字符串，便于旧有调用取值。
     */
    public String getCode() {
        return errorCode.getCode();
    }

    /**
     * @return 建议的 HTTP 状态码，方便上层直接设置响应状态。
     */
    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
