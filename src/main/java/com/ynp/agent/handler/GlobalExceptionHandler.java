package com.ynp.agent.handler;

import com.ynp.agent.exception.ServiceException;
import com.ynp.agent.vo.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一返回 {error: "..."}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 处理业务异常。
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleService(ServiceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 1. 处理参数绑定异常。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidate(Exception ex) {
        String message = "参数错误";
        if (ex instanceof MethodArgumentNotValidException notValid && notValid.getBindingResult().hasErrors()) {
            message = notValid.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException bind && bind.getBindingResult().hasErrors()) {
            message = bind.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof ConstraintViolationException violation && !violation.getConstraintViolations().isEmpty()) {
            message = violation.getConstraintViolations().iterator().next().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    /**
     * 1. 处理通用异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("服务器内部错误"));
    }
}
