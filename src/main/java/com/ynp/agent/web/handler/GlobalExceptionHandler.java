package com.ynp.agent.web.handler;


import com.ynp.agent.exception.BizException;
import com.ynp.agent.model.dto.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 统一捕获系统中的自定义业务异常、参数校验异常以及未预期的运行时异常，保证接口能返回格式化的 Result 结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：直接透传自定义的错误码与提示信息。
     *
     * @param ex 业务异常
     * @return 标准错误响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex, HttpServletResponse response) {
        response.setStatus(ex.getHttpStatus());
        log.warn("业务异常：code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理 Bean Validation 的参数校验异常。
     * 包括 {@link MethodArgumentNotValidException} 和 {@link BindException}，拼接所有字段错误信息反馈给前端。
     *
     * @param ex 参数绑定或校验异常
     * @return 标准错误响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleMethodArgumentNotValidException(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException invalidException) {
            message = invalidException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining("; "));
        } else if (ex instanceof BindException bindException) {
            message = bindException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining("; "));
        } else {
            message = "请求参数校验失败";
        }
        log.warn("参数校验失败：{}", message, ex);
        return Result.error("VALIDATE_ERROR", message);
    }

    /**
     * 处理所有未捕获的异常，避免堆栈信息直接抛给用户。
     *
     * @param ex 未捕获的异常
     * @return 标准错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleDefaultException(Exception ex) {
        log.error("系统内部异常", ex);
        return Result.error("INTERNAL_ERROR", "系统繁忙，请稍后再试");
    }

    /**
     * 将字段错误格式化为“字段名: 错误信息”形式。
     *
     * @param fieldError 字段错误信息
     * @return 格式化后的字符串
     */
    private String formatFieldError(FieldError fieldError) {
        String field = fieldError.getField();
        String msg = Objects.requireNonNullElse(fieldError.getDefaultMessage(), "参数不合法");
        return field + ": " + msg;
    }
}
