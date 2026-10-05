package com.student.platform.qa.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;

/**
 * AI模型配置类
 *
 * 功能说明：
 * - 配置AI聊天模型，支持流式响应
 * - 配置视觉理解模型，支持图像问答
 * - 使用LangChain4j框架与AI模型交互
 *
 * 支持的模型（统一使用 DeepSeek）：
 * - 主聊天模型：deepseek-flash（DeepSeek-V4.1-Flash，速度快）
 * - 视觉模型：deepseek-flash（原生多模态，支持图像理解和问答）
 *
 * 提速要点：
 * - DeepSeek 默认开启思考模式（effort=high），问答场景关闭以大幅降低首字延迟
 * - 关闭请求/响应日志，减少序列化开销
 *
 * 配置方式：
 * 通过环境变量 DEEPSEEK_API_KEY 提供 API Key（见 application.yml）
 *
 * @author student-platform
 */
@Configuration
@Slf4j
public class AiConfig {

    /**
     * 关闭思考模式：问答场景追求响应速度，避免默认 high 档思考拖慢首字
     */
    private static final Map<String, Object> THINKING_DISABLED =
            Map.of("thinking", Map.of("type", "disabled"));

    /**
     * 流式聊天模型的API Key
     */
    @Value("${langchain4j.open-ai.streaming-chat-model.api-key:}")
    private String streamingApiKey;

    /**
     * 流式聊天模型名称
     */
    @Value("${langchain4j.open-ai.streaming-chat-model.model-name:deepseek-flash}")
    private String streamingModelName;

    /**
     * 视觉模型的API Key（与主模型共用 DeepSeek Key）
     */
    @Value("${langchain4j.open-ai.vision-chat-model.api-key:}")
    private String visionApiKey;

    /**
     * 视觉模型名称
     */
    @Value("${langchain4j.open-ai.vision-chat-model.model-name:deepseek-flash}")
    private String visionModelName;

    /**
     * 创建主流式聊天模型（用于普通文本对话）
     *
     * @return StreamingChatModel 流式聊天模型实例
     */
    @Bean
    @Primary
    public StreamingChatModel streamingChatModel() {
        log.info("正在初始化主流式聊天模型: {}", streamingModelName);

        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")  // API基础URL
                .apiKey(streamingApiKey)                 // API密钥
                .modelName(streamingModelName)           // 模型名称
                .timeout(Duration.ofSeconds(120))        // 超时时间（120秒）
                .customParameters(THINKING_DISABLED)     // 关闭思考模式，加速响应
                .build();
    }

    /**
     * 创建视觉聊天模型（用于图像理解）
     *
     * 使用 DeepSeek-V4.1-Flash（deepseek-flash）原生多模态能力，支持图像分析和问答。
     * 如果没有配置有效的API Key，返回null
     *
     * @return StreamingChatModel 视觉模型实例，如果未配置API Key返回null
     */
    @Bean(name = "visionStreamingChatModel")
    public StreamingChatModel visionStreamingChatModel() {
        // 检查API Key是否有效
        if (visionApiKey == null || visionApiKey.isEmpty()) {
            log.warn("视觉模型API密钥未配置，图片功能将被禁用");
            return null;
        }

        log.info("正在初始化视觉流式聊天模型: {}", visionModelName);

        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")  // DeepSeek API URL
                .apiKey(visionApiKey)                    // API密钥
                .modelName(visionModelName)              // 视觉模型名称（deepseek-flash）
                .timeout(Duration.ofMinutes(3))          // 超时时间（3分钟）
                .customParameters(THINKING_DISABLED)     // 关闭思考模式，加速响应
                .build();
    }
}
