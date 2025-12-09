package com.couple.agent.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * OSS 基础配置项。
 *
 * 该配置直接对应 Nacos 或本地 YAML 中的 {@code ali.oss} 前缀，
 * 用于集中维护阿里云 OSS 的关键连接参数，确保运行时所有工具类读取同一份配置。
 */
@Data
@Validated
@RefreshScope
@ConfigurationProperties(prefix = "ali.oss")
public class OssProperties {

    /**
     * 默认访问协议，若未指定协议则统一按 HTTPS 拼接域名，避免出现明文传输。
     */
    private static final String DEFAULT_SCHEME = "https://";

    /**
     * OSS 存储桶名称。
     * 一般建议通过环境变量下发真实值，此处仅做占位校验。
     */
    @NotBlank(message = "OSS 存储桶名称不能为空")
    private String bucket;

    /**
     * OSS 所在地域 ID，例如 cn-hangzhou。
     * 地域信息可用于在未显式指定 endpoint 时拼接默认域名。
     */
    @NotBlank(message = "OSS 地域配置不能为空")
    private String region = "cn-hangzhou";

    /**
     * OSS 访问域名（不包含存储桶前缀），例如 oss-cn-hangzhou.aliyuncs.com。
     * 支持配置完整 URL 或纯域名，工具类会自动补全协议与去除多余斜杠。
     */
    private String endpoint;

    /**
     * 自定义的对外访问域名（CDN 或绑定域名），优先级高于默认 endpoint。
     * 可为空，留空时会自动构造 {@code https://<bucket>.<endpoint>}。
     */
    private String customDomain;

    /**
     * 统一的对象前缀，用于将业务文件归类到固定目录下。
     * 例如设置为 {@code uploads} 时，对象会被存储在 uploads/xxx 路径下。
     */
    private String objectPrefix;

    private String avatar;

    /**
     * 解析可用的 OSS Endpoint。
     * <p>
     * 当未填写 endpoint 时，根据地域信息拼接为 {@code https://oss-<region>.aliyuncs.com}。
     * 若已填写但未带协议，则自动补全为 HTTPS，并移除末尾的 "/"。
     *
     * @return 规范化后的 Endpoint，包含协议前缀
     */
    public String resolveEndpoint() {
        String resolvedEndpoint = endpoint;
        if (!StringUtils.hasText(resolvedEndpoint)) {
            resolvedEndpoint = DEFAULT_SCHEME + "oss-" + region + ".aliyuncs.com";
        }
        resolvedEndpoint = normalizeUrl(resolvedEndpoint);
        return resolvedEndpoint;
    }

    /**
     * 获取对外访问域名。
     * <p>
     * 若配置了 customDomain，则优先返回；否则自动拼接默认域名。
     *
     * @return 可直接用于拼接访问 URL 的域名（含协议）
     */
    public String resolvePublicDomain() {
        if (StringUtils.hasText(customDomain)) {
            return normalizeUrl(customDomain);
        }
        String endpointUrl = resolveEndpoint();
        URI endpointUri = toUri(endpointUrl);
        String authority = endpointUri.getAuthority();
        return DEFAULT_SCHEME + bucket + "." + authority;
    }

    /**
     * 解析统一的对象前缀，移除首尾多余的分隔符，避免重复出现 "//"。
     *
     * @return 处理后的对象前缀，若未配置则返回空字符串
     */
    public String resolveObjectPrefix() {
        if (!StringUtils.hasText(objectPrefix)) {
            return "";
        }
        String trimmed = objectPrefix.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 将传入的地址统一补全协议并去除末尾多余的 "/"。
     *
     * @param origin 原始地址，允许为纯域名
     * @return 规范化后的地址
     */
    private String normalizeUrl(String origin) {
        String candidate = origin.trim();
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            candidate = DEFAULT_SCHEME + candidate;
        }
        if (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    /**
     * 将字符串安全转换为 URI 对象。
     *
     * @param value 需要解析的字符串
     * @return URI 实例；若转换失败则抛出非法参数异常
     */
    private URI toUri(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("OSS endpoint 配置不合法：" + value, ex);
        }
    }
}
