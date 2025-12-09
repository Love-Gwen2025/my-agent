package com.couple.agent.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.exceptions.ServiceException;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.HeadObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.aliyun.sdk.service.oss2.utils.IOUtils;

import com.couple.agent.config.OssProperties;
import com.couple.agent.exception.BizErrorCode;
import com.couple.agent.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 阿里云 OSS 操作工具类。
 * <p>
 * 工具类对常见的校验、上传、下载逻辑做了统一封装，默认使用环境变量中的
 * {@code OSS_ACCESS_KEY_ID}/{@code OSS_ACCESS_KEY_SECRET} 完成签名，
 * 并基于 {@link OssProperties} 读取统一的桶与域名配置，避免各业务模块重复造轮子。
 */
@Component
@RefreshScope
@RequiredArgsConstructor
public class OSSSUtil {

    private final OssProperties ossProperties;

    /**
     * 判断给定 URL 指向的 OSS 资源是否存在。
     * @param resourceUrl 完整的 OSS 访问地址，可以是默认域名或自定义域名
     * @return 资源存在返回 {@code true}，不存在返回 {@code false}
     */
    public boolean exists(String resourceUrl) {
        if (StringUtil.isBlank(resourceUrl)) {
            return false;
        }
        OssObjectLocation location = resolveLocation(resourceUrl);
        if (Objects.isNull(location) || StringUtil.isBlank(location.getObjectKey())) {
            return false;
        }
        HeadObjectRequest request = HeadObjectRequest.newBuilder()
                .bucket(location.getBucketName())
                .key(location.getObjectKey())
                .build();
        try (OSSClient client = createClient()) {
            client.headObject(request);
            return true;
        } catch (ServiceException ex) {
            // 404 代表对象不存在，其他状态码统一向上抛出
            if (ex.statusCode() == 404) {
                return false;
            }
            throw new BizException(BizErrorCode.OSS_HEAD_ERROR, "校验 OSS 对象是否存在失败", ex);
        } catch (Exception ex) {
            throw new BizException(BizErrorCode.OSS_HEAD_ERROR, "校验 OSS 对象是否存在失败", ex);
        }
    }

    /**
     * 简单上传对象，将数据写入当前配置的存储桶并返回可直接访问的 URL。
     *
     * @param objectKey     对象名称或相对路径（无需携带桶名）
     * @param dataStream    待上传的数据流，调用方负责在上传后自行关闭
     * @param contentLength 数据长度，可为空；若传入且小于 {@link Integer#MAX_VALUE} 会写入 HTTP 头部
     * @param contentType   内容类型，可为空
     * @return 上传后的对外访问 URL
     */
    public String uploadSimpleObject(String objectKey,
                                     InputStream dataStream,
                                     Long contentLength,
                                     String contentType) {
        if (StringUtil.isBlank(objectKey)) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_BAD_REQUEST, "上传对象名称不能为空");
        }
        if (Objects.isNull(dataStream)) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_BAD_REQUEST, "上传数据流不能为空");
        }
        String actualKey = buildStoreKey(objectKey);
        PutObjectRequest.Builder builder = PutObjectRequest.newBuilder()
                .bucket(ossProperties.getBucket())
                .key(actualKey)
                .body(BinaryData.fromStream(dataStream, contentLength));
        if (!StringUtil.isBlank(contentType)) {
            builder.contentType(contentType);
        }
        if (!Objects.isNull(contentLength) && contentLength >= 0 && contentLength <= Integer.MAX_VALUE) {
            builder.contentLength(contentLength.intValue());
        }
        try (OSSClient client = createClient()) {
            client.putObject(builder.build());
            return buildPublicUrl(actualKey);
        } catch (ServiceException ex) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_SERVER_ERROR, "上传 OSS 对象失败：" + ex.errorMessage(), ex);
        } catch (Exception ex) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_SERVER_ERROR, "上传 OSS 对象失败", ex);
        }
    }

    /**
     * 根据 URL 下载 OSS 对象，并直接返回文件字节数组。
     * <p>
     * 若需处理大文件，可在业务层对接流式处理逻辑，此处为了通用性默认读取到内存中。
     *
     * @param resourceUrl 完整的 OSS 访问地址
     * @return 对象文件的字节数组
     */
    public byte[] downloadObject(String resourceUrl) {
        OssObjectLocation location = resolveLocation(resourceUrl);
        if (Objects.isNull(location) || StringUtil.isBlank(location.getObjectKey())) {
            throw new BizException(BizErrorCode.OSS_DOWNLOAD_BAD_REQUEST, "无法解析 OSS 资源地址");
        }
        GetObjectRequest request = GetObjectRequest.newBuilder()
                .bucket(location.getBucketName())
                .key(location.getObjectKey())
                .build();
        try (OSSClient client = createClient(); GetObjectResult result = client.getObject(request)) {
            return IOUtils.toByteArray(result.body());
        } catch (ServiceException ex) {
            if (ex.statusCode() == 404) {
                throw new BizException(BizErrorCode.OSS_DOWNLOAD_NOT_FOUND, "请求的 OSS 对象不存在", ex);
            }
            throw new BizException(BizErrorCode.OSS_DOWNLOAD_SERVER_ERROR, "下载 OSS 对象失败：" + ex.errorMessage(), ex);
        } catch (Exception ex) {
            throw new BizException(BizErrorCode.OSS_DOWNLOAD_SERVER_ERROR, "下载 OSS 对象失败", ex);
        }
    }

    /**
     * 创建 OSS 客户端实例，每次调用后均通过 try-with-resources 自动关闭，避免连接泄漏。
     *
     * @return 已配置好凭证、地域与域名的 OSSClient
     */
    private OSSClient createClient() {
        return OSSClient.newBuilder()
                .region(ossProperties.getRegion())
                .endpoint(ossProperties.resolveEndpoint())
                .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                .build();
    }

    /**
     * 根据业务对象名称拼接实际存储路径，自动去除首尾斜杠并挂载统一前缀。
     *
     * @param rawKey 业务侧传入的对象名称
     * @return OSS 中实际使用的对象完整路径
     */
    private String buildStoreKey(String rawKey) {
        String trimmed = rawKey.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (StringUtil.isBlank(trimmed)) {
            throw new BizException(BizErrorCode.OSS_UPLOAD_BAD_REQUEST, "上传对象名称不能为空");
        }
        String prefix = ossProperties.resolveObjectPrefix();
        if (StringUtil.isBlank(prefix)) {
            return trimmed;
        }
        if (trimmed.startsWith(prefix + "/") || StringUtil.equal(trimmed, prefix)) {
            return trimmed;
        }
        return prefix + "/" + trimmed;
    }

    /**
     * 解析 URL 并抽取存储桶与对象路径。
     *
     * @param resourceUrl OSS 访问地址
     * @return 解析结果，包含桶名与对象路径
     */
    private OssObjectLocation resolveLocation(String resourceUrl) {
        try {
            URI uri = new URI(resourceUrl);
            String host = uri.getHost();
            String objectPath = uri.getPath();
            if (StringUtil.isBlank(objectPath)) {
                return null;
            }
            if (objectPath.startsWith("/")) {
                objectPath = objectPath.substring(1);
            }
            if (StringUtil.isBlank(objectPath)) {
                return null;
            }
            String decodedPath = decodePath(objectPath);
            String bucket = extractBucket(host);
            if (StringUtil.isBlank(bucket)) {
                bucket = ossProperties.getBucket();
            }
            return new OssObjectLocation(bucket, decodedPath);
        } catch (URISyntaxException ex) {
            throw new BizException(BizErrorCode.OSS_URL_BAD_REQUEST, "OSS 资源地址格式不正确", ex);
        }
    }

    /**
     * 从域名中推断存储桶名称，兼容默认域名与自定义域名两种形态。
     *
     * @param host URL 中的主机名
     * @return 推断出的存储桶名称，无法识别时返回空字符串
     */
    private String extractBucket(String host) {
        if (StringUtil.isBlank(host)) {
            return "";
        }
        String endpointHost = safeResolveHost(ossProperties.resolveEndpoint());
        if (!StringUtil.isBlank(endpointHost) && host.endsWith(endpointHost)) {
            int idx = host.length() - endpointHost.length();
            if (idx > 1) {
                String bucketCandidate = host.substring(0, idx - 1);
                if (!StringUtil.isBlank(bucketCandidate)) {
                    return bucketCandidate;
                }
            }
            return ossProperties.getBucket();
        }
        String publicHost = safeResolveHost(ossProperties.resolvePublicDomain());
        if (!StringUtil.isBlank(publicHost) && host.equalsIgnoreCase(publicHost)) {
            return ossProperties.getBucket();
        }
        return "";
    }

    /**
     * 安全地解析 URL 所对应的 host，解析失败返回空字符串。
     *
     * @param url 待解析的地址
     * @return host 字符串，若无法解析返回空字符串
     */
    private String safeResolveHost(String url) {
        try {
            URI uri = new URI(url);
            return StringUtils.hasText(uri.getHost()) ? uri.getHost() : "";
        } catch (URISyntaxException ex) {
            return "";
        }
    }

    /**
     * 对 URL 中的对象路径进行解码，保证含中文或特殊字符的 Key 能被正确识别。
     *
     * @param encodedPath URL 中的编码路径
     * @return 解码后的对象 Key
     */
    private String decodePath(String encodedPath) {
        try {
            return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new BizException(BizErrorCode.OSS_URL_BAD_REQUEST, "OSS 资源路径包含非法编码", ex);
        }
    }

    /**
     * 构造可直接访问的对象 URL，自动处理多余的斜杠。
     *
     * @param objectKey 对象在 OSS 中的完整路径
     * @return 对外访问地址
     */
    private String buildPublicUrl(String objectKey) {
        String domain = ossProperties.resolvePublicDomain();
        while (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/" + objectKey;
    }

    /**
     * OSS 资源定位结果，封装桶名与对象路径。
     */
    private static class OssObjectLocation {
        private final String bucketName;
        private final String objectKey;

        OssObjectLocation(String bucketName, String objectKey) {
            this.bucketName = bucketName;
            this.objectKey = objectKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public String getObjectKey() {
            return objectKey;
        }
    }
}
