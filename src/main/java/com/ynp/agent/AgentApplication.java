package com.ynp.agent;

import com.ynp.agent.config.JwtProperties;
import com.ynp.agent.config.OssProperties;
import com.ynp.agent.config.SecurityIgnoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 应用入口。
 */
@EnableConfigurationProperties({JwtProperties.class, SecurityIgnoreProperties.class, OssProperties.class})
@SpringBootApplication
public class AgentApplication {

    /**
     * 1. 启动 Spring Boot 应用，加载全部配置与 Bean。
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
