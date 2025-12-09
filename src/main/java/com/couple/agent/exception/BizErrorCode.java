package com.couple.agent.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * <p>业务错误码枚举。</p>
 * <p>将系统内所有可预见的业务异常统一收敛在此，后续如需扩展新模块，请继续在对应分组内补充枚举常量。</p>
 */
@AllArgsConstructor
@Getter
public enum BizErrorCode implements ErrorCode {

    // ========================== OSS 模块 ==========================
    /**
     * OSS 对象元数据校验失败。
     */
    OSS_HEAD_ERROR("OSS-HEAD-500", "校验 OSS 对象是否存在失败", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    /**
     * OSS 上传请求参数不合法。
     */
    OSS_UPLOAD_BAD_REQUEST("OSS-UPLOAD-400", "上传请求参数不合法", HttpStatus.BAD_REQUEST.value()),
    /**
     * OSS 上传服务端异常。
     */
    OSS_UPLOAD_SERVER_ERROR("OSS-UPLOAD-500", "上传 OSS 对象失败", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    /**
     * OSS 下载请求参数不合法。
     */
    OSS_DOWNLOAD_BAD_REQUEST("OSS-DOWNLOAD-400", "无法解析 OSS 资源地址", HttpStatus.BAD_REQUEST.value()),
    /**
     * OSS 对象不存在。
     */
    OSS_DOWNLOAD_NOT_FOUND("OSS-DOWNLOAD-404", "请求的 OSS 对象不存在", HttpStatus.NOT_FOUND.value()),
    /**
     * OSS 下载服务端异常。
     */
    OSS_DOWNLOAD_SERVER_ERROR("OSS-DOWNLOAD-500", "下载 OSS 对象失败", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    /**
     * OSS URL 格式错误。
     */
    OSS_URL_BAD_REQUEST("OSS-URL-400", "OSS 资源地址非法", HttpStatus.BAD_REQUEST.value()),

    // ========================== 用户模块 ==========================
    /**
     * 用户不存在。
     */
    USER_NOT_FOUND("USER-404", "用户不存在", HttpStatus.NOT_FOUND.value()),
    /**
     * 用户参数不合法。
     */
    USER_BAD_REQUEST("USER-400", "用户参数不合法", HttpStatus.BAD_REQUEST.value()),
    /**
     * 用户被禁止登录。
     */
    USER_FORBIDDEN("USER-403", "该用户被禁止登录", HttpStatus.FORBIDDEN.value()),
    /**
     * 用户认证失败。
     */
    USER_UNAUTHORIZED("USER-401", "密码错误", HttpStatus.UNAUTHORIZED.value()),

    // ========================== 消息模块 ==========================
    /**
     * 用户未登录或会话过期。
     */
    AUTH_UNAUTHORIZED("AUTH-401", "用户未登录或会话已过期", HttpStatus.UNAUTHORIZED.value()),
    /**
     * 会话不存在。
     */
    MESSAGE_CONVERSATION_NOT_FOUND("MSG-404", "会话不存在或已被删除", HttpStatus.NOT_FOUND.value()),
    /**
     * 当前用户不在会话内。
     */
    MESSAGE_FORBIDDEN("MSG-403", "您不在该会话中，无法发送消息", HttpStatus.FORBIDDEN.value()),

    // ========================== 会话模块 ==========================
    /**
     * 会话成员为空。
     */
    CONVERSATION_MEMBER_EMPTY("CONV-400", "会话成员不能为空", HttpStatus.BAD_REQUEST.value()),
    /**
     * 会话类型为空。
     */
    CONVERSATION_TYPE_EMPTY("CONV-401", "会话类型不能为空", HttpStatus.BAD_REQUEST.value()),
    /**
     * 会话成员包含不存在的用户。
     */
    CONVERSATION_MEMBER_NOT_FOUND("CONV-404", "存在不存在的用户", HttpStatus.NOT_FOUND.value());

    /**
     * 错误码唯一标识。
     */
    private final String code;
    /**
     * 默认错误提示。
     */
    private final String defaultMessage;
    /**
     * 建议的 HTTP 状态码。
     */
    private final int httpStatus;
}
