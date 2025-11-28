package com.ynp.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置。
 */
@Configuration
public class CommonConfig {

    /**
     * 1. 配置 BCrypt 加密器，使用较高强度保证密码安全。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
