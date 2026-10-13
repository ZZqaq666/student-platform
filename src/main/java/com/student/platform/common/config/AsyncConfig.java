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
     * 非流式 AI 问答的等待专用池。
     *
     * 为什么不能复用 applicationTaskExecutor：它配的是 CallerRunsPolicy，一旦
     * 4~8 个线程加 200 队列被打满，任务会回到调用线程执行——而调用线程正是
     * Tomcat 请求线程，等于把这里想消除的阻塞原样搬回来，还是静默的。
     *
     * 因此用 AbortPolicy：池满时抛 RejectedExecutionException，由 QaController
     * 转成「AI 服务繁忙」的明确响应，宁可拒绝也不占容器线程。
     */
    @Bean("aiRequestExecutor")
    public Executor aiRequestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("ai-request-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
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
