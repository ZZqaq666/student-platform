package com.student.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置。
 *
 * 学长学姐答疑需要在发布问题 / 提交追问后异步调用大模型生成回答，
 * 不能阻塞发帖接口，因此这里提供专用线程池 seniorAiExecutor。
 *
 * 队列有界 + 拒绝时记日志：AI 调用偶发抖动不应该拖垮主线程池。
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("seniorAiExecutor")
    public Executor seniorAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("senior-ai-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler((runnable, pool) ->
                log.warn("seniorAiExecutor 队列已满，本次 AI 生成任务被拒绝"));
        executor.initialize();
        return executor;
    }

    /**
     * 兜底：未指定线程池名称的 @Async 也走一个有界线程池，
     * 避免 Spring 默认的无界 SimpleAsyncTaskExecutor 无限制起线程。
     */
    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("app-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
