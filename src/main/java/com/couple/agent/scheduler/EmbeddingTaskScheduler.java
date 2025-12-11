package com.couple.agent.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.couple.agent.mapper.EmbeddingTaskMapper;
import com.couple.agent.model.domain.EmbeddingTask;
import com.couple.agent.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 向量化任务调度器
 * 定时扫描待处理的向量化任务，使用虚拟线程异步执行
 */
@Slf4j
@Component
public class EmbeddingTaskScheduler {

    /**
     * 每批处理的任务数量
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY = 3;

    @Autowired
    private EmbeddingTaskMapper embeddingTaskMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    @Qualifier("embeddingTaskExecutor")
    private ExecutorService executor;

    /**
     * 定时处理待执行的向量化任务
     *
     * 每 10 秒扫描一次待处理任务
     */
    @Scheduled(fixedDelay = 10000)
    public void processPendingTasks() {
        // 1. 查询待处理的任务
        List<EmbeddingTask> tasks = fetchPendingTasks(BATCH_SIZE);
        if (tasks.isEmpty()) {
            return;
        }

        log.debug("开始处理 {} 个向量化任务", tasks.size());

        // 2. 使用虚拟线程并发处理任务
        for (EmbeddingTask task : tasks) {
            executor.submit(() -> processTask(task));
        }
    }

    /**
     * 定时处理失败需要重试的任务
     *
     * <p>每 30 秒扫描一次失败任务</p>
     */
    @Scheduled(fixedDelay = 30000)
    public void processRetryTasks() {
        // 查询需要重试的任务（失败且重试次数未超限）
        List<EmbeddingTask> tasks = embeddingTaskMapper.selectList(
                new LambdaQueryWrapper<EmbeddingTask>()
                        .eq(EmbeddingTask::getStatus, EmbeddingTask.STATUS_FAILED)
                        .lt(EmbeddingTask::getRetryCount, MAX_RETRY)
                        .orderByAsc(EmbeddingTask::getCreateTime)
                        .last("LIMIT " + BATCH_SIZE)
        );

        if (tasks.isEmpty()) {
            return;
        }

        log.info("开始重试 {} 个失败的向量化任务", tasks.size());

        for (EmbeddingTask task : tasks) {
            executor.submit(() -> processTask(task));
        }
    }

    /**
     * 获取待处理的任务
     *
     * @param limit 数量限制
     * @return 任务列表
     */
    private List<EmbeddingTask> fetchPendingTasks(int limit) {
        return embeddingTaskMapper.selectList(
                new LambdaQueryWrapper<EmbeddingTask>()
                        .eq(EmbeddingTask::getStatus, EmbeddingTask.STATUS_PENDING)
                        .orderByAsc(EmbeddingTask::getCreateTime)
                        .last("LIMIT " + limit)
        );
    }

    /**
     * 处理单个任务
     *
     * @param task 向量化任务
     */
    private void processTask(EmbeddingTask task) {
        try {
            // 1. 标记任务为处理中
            markProcessing(task.getId());

            // 2. 执行向量化
            embeddingService.storeMessageEmbedding(
                    task.getMessageId(),
                    task.getConversationId(),
                    task.getUserId(),
                    task.getContentText()
            );

            // 3. 标记任务完成
            markCompleted(task.getId());

            log.debug("向量化任务完成，taskId: {}, messageId: {}",
                    task.getId(), task.getMessageId());

        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    /**
     * 标记任务为处理中
     *
     * @param taskId 任务ID
     */
    private void markProcessing(Long taskId) {
        embeddingTaskMapper.update(null,
                new LambdaUpdateWrapper<EmbeddingTask>()
                        .eq(EmbeddingTask::getId, taskId)
                        .set(EmbeddingTask::getStatus, EmbeddingTask.STATUS_PROCESSING)
                        .set(EmbeddingTask::getProcessTime, LocalDateTime.now())
        );
    }

    /**
     * 标记任务完成
     *
     * @param taskId 任务ID
     */
    private void markCompleted(Long taskId) {
        embeddingTaskMapper.update(null,
                new LambdaUpdateWrapper<EmbeddingTask>()
                        .eq(EmbeddingTask::getId, taskId)
                        .set(EmbeddingTask::getStatus, EmbeddingTask.STATUS_COMPLETED)
                        .set(EmbeddingTask::getProcessTime, LocalDateTime.now())
        );
    }

    /**
     * 处理任务失败
     *
     * @param task 任务
     * @param e 异常
     */
    private void handleTaskFailure(EmbeddingTask task, Exception e) {
        int newRetryCount = task.getRetryCount() + 1;

        if (newRetryCount >= MAX_RETRY) {
            // 超过重试次数，标记为失败
            embeddingTaskMapper.update(null,
                    new LambdaUpdateWrapper<EmbeddingTask>()
                            .eq(EmbeddingTask::getId, task.getId())
                            .set(EmbeddingTask::getStatus, EmbeddingTask.STATUS_FAILED)
                            .set(EmbeddingTask::getRetryCount, newRetryCount)
                            .set(EmbeddingTask::getErrorMessage, e.getMessage())
                            .set(EmbeddingTask::getProcessTime, LocalDateTime.now())
            );

            log.error("向量化任务最终失败，taskId: {}, messageId: {}, 错误: {}",
                    task.getId(), task.getMessageId(), e.getMessage());

        } else {
            // 增加重试次数，重置状态为待处理
            embeddingTaskMapper.update(null,
                    new LambdaUpdateWrapper<EmbeddingTask>()
                            .eq(EmbeddingTask::getId, task.getId())
                            .set(EmbeddingTask::getStatus, EmbeddingTask.STATUS_PENDING)
                            .set(EmbeddingTask::getRetryCount, newRetryCount)
                            .set(EmbeddingTask::getErrorMessage, e.getMessage())
            );

            log.warn("向量化任务失败将重试，taskId: {}, 重试次数: {}/{}, 错误: {}",
                    task.getId(), newRetryCount, MAX_RETRY, e.getMessage());
        }
    }
}
