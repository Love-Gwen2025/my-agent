package com.couple.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置
 * 注册自定义反序列化模块，允许前端以字符串形式提交长整型 ID，后端仍然按照 Long 处理
 */
@Configuration
public class JacksonConfig {

    /**
     * 定制 ObjectMapper，使其在解析 Long 类型字段时兼容字符串输入
     * 这样即便前端出于精度考虑传递字符串 ID，控制层依旧可以直接使用 Long 类型参数
     * 同时，序列化的时候也要把Long转为String
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringAndCoercion() {
        return builder -> {
            // 1.Long 序列化为字符串（包含 long 基本类型）
            SimpleModule m = new SimpleModule()
                    .addSerializer(Long.class, ToStringSerializer.instance)
                    .addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.modulesToInstall(m);

            // 2.允许字符串反序列化为整数（请求体中 "123" → Long）
            builder.postConfigurer((ObjectMapper mapper) -> {
                mapper.coercionConfigFor(LogicalType.Integer)
                        .setCoercion(CoercionInputShape.String, CoercionAction.TryConvert);
                // 可选：也允许小整数放大到 Long（一般默认即可）
                // mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
            });

            // 可选：全局不写科学计数法
            builder.featuresToDisable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        };
    }
}

