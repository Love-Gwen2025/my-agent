package com.ynp.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口。
 */
@SpringBootApplication
public class AgentApplication {

    /**
     * 1. 启动 Spring Boot 应用，加载全部配置与 Bean。
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
