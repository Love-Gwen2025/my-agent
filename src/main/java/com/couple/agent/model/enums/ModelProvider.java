package com.couple.agent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 模型提供商枚举
 *
 * <p>定义支持的 AI 模型提供商类型</p>
 *
 * @author ynp
 */
@Getter
@AllArgsConstructor
public enum ModelProvider {

    /**
     * OpenAI 官方 API
     */
    OPENAI("openai", "OpenAI"),

    /**
     * Azure OpenAI 服务
     */
    AZURE_OPENAI("azure_openai", "Azure OpenAI"),

    /**
     * Anthropic Claude 模型
     */
    ANTHROPIC("anthropic", "Claude"),

    /**
     * Google Gemini 模型
     */
    GOOGLE_GEMINI("google_gemini", "Gemini");

    /**
     * 提供商编码
     */
    private final String code;

    /**
     * 提供商显示名称
     */
    private final String displayName;

    /**
     * 根据编码获取提供商枚举
     *
     * @param code 提供商编码
     * @return 对应的枚举值，如果不存在返回 null
     */
    public static ModelProvider fromCode(String code) {
        for (ModelProvider provider : values()) {
            if (provider.getCode().equals(code)) {
                return provider;
            }
        }
        return null;
    }
}
