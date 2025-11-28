package com.ynp.agent.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 应用自定义配置，集中解析环境变量。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * JWT 加密密钥。
     */
    @NotBlank
    private String sessionSecret;

    /**
     * JWT 过期时间（分钟）。
     */
    private long jwtExpireMinutes;

    /**
     * 历史消息截断条数。
     */
    private int historyLimit;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 助手展示名。
     */
    private String assistantDisplayName;

    /**
     * 上传大小限制（MB）。
     */
    private long uploadSizeLimitMb;

    /**
     * 最多上传文件数。
     */
    private int maxUploadFiles;

    /**
     * 数据目录。
     */
    private String dataDir;

    /**
     * GPT 相关配置。
     */
    private String gptEndpoint;
    private String gptApiKey;
    private String gptModel;
    private double gptTemperature;
    private long gptTimeoutMs;

    /**
     * 账号配置字符串。
     */
    private String userAccounts;

    /**
     * 解析后的账号集合。
     */
    private List<UserAccount> parsedAccounts = new ArrayList<>();

    /**
     * 1. 在 Bean 构建后解析账号配置，确保最小可用集合。
     */
    @PostConstruct
    public void initAccounts() {
        List<UserAccount> accounts = new ArrayList<>();
        if (!StringUtils.hasText(userAccounts)) {
            throw new IllegalStateException("USER_ACCOUNTS 未配置。格式：username:displayName:password，多组用逗号分隔");
        }
        String[] chunks = userAccounts.split(",");
        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            /* 1. 将每组 username:displayName:password 拆分并校验 */
            String[] fields = trimmed.split(":");
            if (fields.length < 3) {
                continue;
            }
            String username = fields[0].trim().toLowerCase();
            String displayName = fields[1].trim();
            String password = fields[2].trim();
            if (!StringUtils.hasText(username) || !StringUtils.hasText(displayName) || !StringUtils.hasText(password)) {
                continue;
            }
            accounts.add(new UserAccount(username, displayName, password));
        }
        this.parsedAccounts = Collections.unmodifiableList(accounts);
    }

    /**
     * 1. 获取账号配置的不可变视图。
     */
    public List<UserAccount> getSafeAccounts() {
        if (Objects.isNull(parsedAccounts)) {
            return Collections.emptyList();
        }
        return parsedAccounts;
    }

    /**
     * 配置中的账号结构。
     */
    @Getter
    public static class UserAccount {

        private final String username;
        private final String displayName;
        private final String password;

        public UserAccount(String username, String displayName, String password) {
            this.username = username;
            this.displayName = displayName;
            this.password = password;
        }
    }
}
