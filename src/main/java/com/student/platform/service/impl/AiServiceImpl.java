package com.student.platform.service.impl;

import com.student.platform.dto.SseMessage;
import com.student.platform.service.AiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final StreamingChatModel streamingChatModel;
    private final Map<String, String> responseCache = new HashMap<>();

    @Autowired
    public AiServiceImpl(StreamingChatModel streamingChatModel) {
        log.info("初始化 AI 服务（使用 LangChain4j Spring Boot Starter）");
        this.streamingChatModel = streamingChatModel;
        
        // 预加载常见问题的缓存
        preloadCache();
    }
    
    private void preloadCache() {
        // 预加载一些常见问题的回答（使用 Markdown 格式）
        responseCache.put("什么是 Java", "# Java 简介\n\nJava 是一种广泛使用的**高级、面向对象、跨平台**的编程语言和计算平台。\n\n## 主要特点\n\n- **面向对象**：支持封装、继承、多态等面向对象特性\n- **跨平台**：一次编写，到处运行\n- **自动内存管理**：通过垃圾回收机制管理内存\n- **强类型**：编译时类型检查\n- **多线程支持**：内置多线程处理能力\n- **丰富的标准库**：提供大量实用工具类\n\n## 应用领域\n\n- 企业级应用开发\n- Android 移动应用开发\n- 大数据与云计算\n- 嵌入式系统与物联网\n- 科学计算与金融系统");
        responseCache.put("Java 的核心特性", "# Java 核心特性\n\nJava 的核心特性包括：\n\n## 1. 面向对象\n\nJava 是一种纯面向对象的编程语言，支持：\n- 封装\n- 继承\n- 多态\n- 抽象\n\n## 2. 跨平台性\n\n通过 JVM（Java 虚拟机）实现跨平台运行：\n```\n一次编写，到处运行 (Write Once, Run Anywhere)\n```\n\n## 3. 自动内存管理\n\n- 垃圾回收机制\n- 减少内存泄漏风险\n\n## 4. 强类型\n\n- 编译时类型检查\n- 提高代码安全性\n\n## 5. 多线程支持\n\n- 内置多线程 API\n- 简化并发编程\n\n## 6. 丰富的标准库\n\n- 集合框架\n- IO 操作\n- 网络编程\n- 数据库访问\n- 安全框架");
        responseCache.put("Java 的应用领域", "# Java 应用领域\n\nJava 在以下领域有广泛应用：\n\n## 1. 企业级应用\n\n- 大型分布式系统\n- 电子商务平台\n- 金融交易系统\n- 企业资源规划 (ERP)\n\n## 2. Android 开发\n\n- 移动应用开发\n- 游戏开发\n- 物联网应用\n\n## 3. 大数据与云计算\n\n- Hadoop 生态系统\n- Spark 数据分析\n- 云服务后端\n\n## 4. 嵌入式系统与物联网\n\n- 智能设备\n- 传感器网络\n- 工业控制系统\n\n## 5. 科学计算与金融系统\n\n- 数值模拟\n- 风险分析\n- 高频交易系统\n\n## 技术栈示例\n\n```java\n// Spring Boot 企业应用\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```");
        log.info("预加载缓存完成，缓存大小：{}", responseCache.size());
    }

    @Override
    public String generateResponseWithContext(String context, String question) {
        try {
            log.info("生成 AI 响应");
            
            // 检查缓存
            String cacheKey = question.trim();
            if (responseCache.containsKey(cacheKey)) {
                log.info("从缓存中获取响应");
                return responseCache.get(cacheKey);
            }
            
            // 构建完整的提示信息
            String prompt = "基于以下上下文，回答用户问题：\n\n" + context + "\n\n用户问题：" + question;
            
            // 使用 LangChain4j 生成响应
            if (streamingChatModel != null) {
                // 使用流式模型获取完整响应
                StringBuilder fullResponse = new StringBuilder();
                streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        fullResponse.append(partialResponse);
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        log.error("生成响应失败：{}", error.getMessage());
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        // 完成生成
                    }
                });
                log.info("AI 响应生成成功");
                // 缓存响应
                responseCache.put(cacheKey, fullResponse.toString());
                return fullResponse.toString();
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成响应");
                String mockResponse = "这是一个模拟的 AI 响应：" + prompt;
                // 缓存模拟响应
                responseCache.put(cacheKey, mockResponse);
                return mockResponse;
            }
        } catch (Exception e) {
            log.error("生成响应失败：{}", e.getMessage(), e);
            return "抱歉，我无法处理您的请求，请稍后重试。";
        }
    }

    @Override
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question) {
        try {
            log.info("生成 AI 流式响应");
            
            // 检查缓存
            String cacheKey = question.trim();
            if (responseCache.containsKey(cacheKey)) {
                log.info("从缓存中获取流式响应");
                // 流式输出缓存的响应
                return Flux.create(sink -> {
                    String cachedResponse = responseCache.get(cacheKey);
                    // 分块发送响应
                    int chunkSize = 50;
                    for (int i = 0; i < cachedResponse.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, cachedResponse.length());
                        final String chunk = cachedResponse.substring(i, end);
                        sink.next(SseMessage.data(chunk));
                    }
                    sink.next(SseMessage.complete());
                    sink.complete();
                });
            }
            
            // 构建完整的提示信息
            String prompt = "基于以下上下文，回答用户问题：\n\n" + context + "\n\n用户问题：" + question;
            
            // 使用真正的流式 API
            if (streamingChatModel != null) {
                log.info("使用 OpenAI 流式 API 生成响应");
                Sinks.Many<SseMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
                
                // 流式生成响应
                StringBuilder fullResponse = new StringBuilder();
                streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        // 处理每个生成的块
                        fullResponse.append(partialResponse);
                        // 发送块到客户端
                        sink.tryEmitNext(SseMessage.data(partialResponse));
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        // 处理错误
                        log.error("流式生成失败：{}", error.getMessage());
                        sink.tryEmitNext(SseMessage.error("生成响应失败：" + error.getMessage()));
                        sink.tryEmitComplete();
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        // 完成生成
                        log.info("流式生成完成");
                        // 缓存完整响应
                        responseCache.put(cacheKey, fullResponse.toString());
                        sink.tryEmitNext(SseMessage.complete());
                        sink.tryEmitComplete();
                    }
                });
                
                return sink.asFlux()
                    // 添加超时处理
                    .timeout(Duration.ofMinutes(2))
                    // 错误处理
                    .onErrorResume(error -> {
                        log.error("流式响应错误：{}", error.getMessage());
                        return Flux.just(SseMessage.error("生成响应失败：" + error.getMessage()));
                    });
            } else {
                // 如果模型未初始化，使用模拟响应
                log.warn("使用模拟 AI 生成流式响应");
                String mockResponse = "这是一个模拟的 AI 流式响应：" + prompt;
                responseCache.put(cacheKey, mockResponse);
                
                // 流式输出模拟响应
                return Flux.create(sink -> {
                    int chunkSize = 50;
                    for (int i = 0; i < mockResponse.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, mockResponse.length());
                        final String chunk = mockResponse.substring(i, end);
                        sink.next(SseMessage.data(chunk));
                    }
                    sink.next(SseMessage.complete());
                    sink.complete();
                });
            }
        } catch (Exception e) {
            log.error("生成流式响应失败：{}", e.getMessage(), e);
            // 流式输出错误响应
            return Flux.create(sink -> {
                sink.next(SseMessage.error("生成响应失败：" + e.getMessage()));
                sink.complete();
            });
        }
    }
}
