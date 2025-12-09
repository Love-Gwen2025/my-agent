package com.ynp.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 虚拟线程配置类
 *
 * <p>配置 Java 21 虚拟线程执行器，用于异步任务处理</p>
 *
 * @author ynp
 */
@Slf4j
@Configuration
@EnableScheduling
public class VirtualThreadConfig {

    /**
     * 创建向量化任务执行器
     *
     * <p>使用虚拟线程池，每个任务一个虚拟线程，低开销高并发</p>
     *
     * @return 虚拟线程执行器
     */
    @Bean("embeddingTaskExecutor")
    public ExecutorService embeddingTaskExecutor() {
        log.info("创建虚拟线程执行器 (embeddingTaskExecutor)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建通用异步任务执行器
     *
     * @return 虚拟线程执行器
     */
    @Bean("asyncTaskExecutor")
    public ExecutorService asyncTaskExecutor() {
        log.info("创建虚拟线程执行器 (asyncTaskExecutor)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
