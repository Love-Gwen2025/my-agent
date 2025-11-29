package com.ynp.agent.exception;

/**
 * 业务异常，携带 HTTP 状态码。
 */
public class ServiceException extends RuntimeException {

    private final int status;

    public ServiceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
