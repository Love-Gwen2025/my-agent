package com.ynp.agent.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * AI 模型配置实体
 *
 * <p>存储支持的 AI 模型配置信息，包括模型参数、提供商等</p>
 *
 * @author ynp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@TableName("t_ai_model")
public class AiModel extends BasePo {

    /**
     * 模型编码（唯一标识）
     * 如：gpt-4o, claude-3-5-sonnet-20241022
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 模型显示名称
     * 如：GPT-4o, Claude 3.5 Sonnet
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 模型提供商
     * 如：openai, anthropic, google_gemini
     */
    @TableField("provider")
    private String provider;

    /**
     * API 端点地址
     */
    @TableField("api_endpoint")
    private String apiEndpoint;

    /**
     * 是否为默认模型
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 是否支持流式输出
     */
    @TableField("is_streaming")
    private Boolean isStreaming;

    /**
     * 最大 Token 数
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数（控制随机性）
     */
    @TableField("temperature")
    private BigDecimal temperature;

    /**
     * 其他配置（JSON 格式）
     */
    @TableField("config")
    private String config;

    /**
     * 状态：0=禁用，1=启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
