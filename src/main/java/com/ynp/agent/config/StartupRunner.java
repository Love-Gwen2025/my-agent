package com.ynp.agent.config;

import com.ynp.agent.service.InitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化数据。
 */
@Component
public class StartupRunner implements CommandLineRunner {

    private final InitService initService;

    public StartupRunner(InitService initService) {
        this.initService = initService;
    }

    /**
     * 1. 启动后确保账号与默认会话。
     */
    @Override
    public void run(String... args) {
        initService.ensureAccounts();
    }
}
