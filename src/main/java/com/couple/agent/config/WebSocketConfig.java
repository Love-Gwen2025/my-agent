package com.couple.agent.config;
// WebSocketConfig.java

import com.fasterxml.jackson.databind.ObjectMapper;
import com.couple.agent.web.support.WebSocketAuthHandshakeInterceptor;
import com.couple.agent.web.support.WebSocketPrincipalHandshakeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;
    private final WebSocketPrincipalHandshakeHandler principalHandshakeHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(principalHandshakeHandler)
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 先放开，后面改白名单
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic"); // 简单内存 Broker
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");      // 点对点
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // 自定义 Jackson 转换器，使用全局 ObjectMapper 确保 Long → String 一致
        MappingJackson2MessageConverter jsonConverter = new MappingJackson2MessageConverter();
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
        jsonConverter.setContentTypeResolver(resolver);
        jsonConverter.setObjectMapper(objectMapper);
        messageConverters.add(jsonConverter);
        // 额外保留字符串转换器，兼容心跳/文本消息
        messageConverters.add(new StringMessageConverter());
        return false; // 继续注册框架默认的转换器
    }
}
