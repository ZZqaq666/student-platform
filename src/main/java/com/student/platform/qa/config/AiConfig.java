package com.student.platform.qa.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * AI模型配置类
 *
 * 功能说明：
 * - 配置AI聊天模型，支持流式响应
 * - 配置视觉理解模型，支持图像问答
 * - 使用LangChain4j框架与AI模型交互
 *
 * 支持的模型：
 * - 主聊天模型：DeepSeek（深度求索）
 * - 视觉模型：智谱GLM-4.6V（支持图像理解和问答）
 *
 * 配置方式：
 * 通过application.yml配置文件设置API Key和模型参数
 * 
 * @author student-platform
 */
@Configuration
@Slf4j
public class AiConfig {

    /**
     * 流式聊天模型的基础URL
     * 默认使用DeepSeek API
     */
    @Value("${langchain4j.open-ai.streaming-chat-model.base-url:https://api.deepseek.com/v1}")
    private String streamingBaseUrl;

    /**
     * 流式聊天模型的API Key
     * 需要在配置文件中设置有效的API Key
     */
    @Value("${langchain4j.open-ai.streaming-chat-model.api-key:}")
    private String streamingApiKey;

    /**
     * 流式聊天模型名称
     * 默认使用deepseek-chat
     */
    @Value("${langchain4j.open-ai.streaming-chat-model.model-name:deepseek-chat}")
    private String streamingModelName;

    /**
     * 视觉模型的基础URL
     * 默认使用智谱GLM-4.6V API
     */
    @Value("${langchain4j.open-ai.vision-chat-model.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String visionBaseUrl;

    /**
     * 视觉模型的API Key
     * 需要在配置文件中设置有效的API Key
     */
    @Value("${langchain4j.open-ai.vision-chat-model.api-key:}")
    private String visionApiKey;

    /**
     * 视觉模型名称
     * 默认使用glm-4-6v
     */
    @Value("${langchain4j.open-ai.vision-chat-model.model-name:glm-4-6v}")
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
                .baseUrl(streamingBaseUrl)           // API基础URL
                .apiKey(streamingApiKey)             // API密钥
                .modelName(streamingModelName)       // 模型名称
                .timeout(Duration.ofSeconds(120))    // 超时时间（120秒）
                .build();
    }

    /**
     * 创建视觉聊天模型（用于图像理解）
     * 
     * 使用智谱GLM-4.6V视觉模型，支持图像分析和问答
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
        log.info("视觉模型原始baseUrl: {}", visionBaseUrl);
        
        // 处理baseUrl: 如果包含/chat/completions，去掉后缀（LangChain4j会自动追加）
        String processedBaseUrl = visionBaseUrl;
        if (visionBaseUrl != null && visionBaseUrl.endsWith("/chat/completions")) {
            processedBaseUrl = visionBaseUrl.substring(0, visionBaseUrl.length() - "/chat/completions".length());
            log.info("调整视觉模型baseUrl从 {} 到 {}", visionBaseUrl, processedBaseUrl);
        }
        
        log.info("最终视觉模型配置: baseUrl={}, modelName={}", processedBaseUrl, visionModelName);
        
        return OpenAiStreamingChatModel.builder()
                .baseUrl(processedBaseUrl)         // 智谱GLM API URL
                .apiKey(visionApiKey)              // API密钥
                .modelName(visionModelName)        // 视觉模型名称
                .timeout(Duration.ofMinutes(5))    // 超时时间增加到5分钟
                .build();
    }
}
