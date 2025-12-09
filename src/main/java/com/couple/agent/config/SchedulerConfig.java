package com.couple.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务调度配置。
 * 定义三个独立的线程池，分别用于不同类型的异步/定时任务，便于统一管理线程资源并避免互相阻塞。
 */
@Configuration
public class SchedulerConfig {

    /**
     * 构建线程池的通用模板，减少重复代码。
     *
     * @param prefix 线程名前缀，方便日志排查
     * @param core   核心线程数
     * @param max    最大线程数
     * @param queue  队列容量
     * @return 配置完成的线程池
     */
    private ThreadPoolTaskExecutor buildExecutor(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix + "-");
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 线程池一：taskOneExecutor
     * 建议用于执行实时性较高的业务任务。
     */
    @Bean(name = "taskOneExecutor")
    public ThreadPoolTaskExecutor taskOneExecutor() {
        return buildExecutor("task-one", 4, 8, 200);
    }

    /**
     * 线程池二：taskTwoExecutor
     * 建议用于执行耗时较长的批处理或数据同步任务。
     */
    @Bean(name = "taskTwoExecutor")
    public ThreadPoolTaskExecutor taskTwoExecutor() {
        return buildExecutor("task-two", 6, 12, 500);
    }

    /**
     * 线程池三：taskThreeExecutor
     * 建议用于执行低优先级的后台任务，例如日志清理、统计等。
     */
    @Bean(name = "taskThreeExecutor")
    public ThreadPoolTaskExecutor taskThreeExecutor() {
        return buildExecutor("task-three", 2, 4, 100);
    }
}
