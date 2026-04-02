package com.student.platform.service.impl;

import com.student.platform.dto.BookRecommendationDTO;
import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.SseMessage;
import com.student.platform.service.AiService;
import com.student.platform.service.util.AiCacheManager;
import com.student.platform.service.util.ErrorHandler;
import com.student.platform.service.util.SessionManager;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    // 流式响应配置
    @Value("${ai.stream.chunk-size:100}")
    private int streamChunkSize;
    
    @Value("${ai.stream.buffer-size:1024}")
    private int streamBufferSize;
    
    private final StreamingChatModel streamingChatModel;
    private final AiCacheManager aiCacheManager;
    private final SessionManager sessionManager;
    private final ErrorHandler errorHandler;
    
    /**
     * 创建新会话
     * @return 会话ID
     */
    public String createSession() {
        return sessionManager.createSession();
    }

    @Autowired
    public AiServiceImpl(StreamingChatModel streamingChatModel, AiCacheManager aiCacheManager, SessionManager sessionManager, ErrorHandler errorHandler) {
        log.info("初始化 AI 服务（使用 LangChain4j Spring Boot Starter）");
        this.streamingChatModel = streamingChatModel;
        this.aiCacheManager = aiCacheManager;
        this.sessionManager = sessionManager;
        this.errorHandler = errorHandler;
        
    }

    @Override
    public String generateResponseWithContext(String context, String question) {
        return generateResponseWithContext(context, question, null);
    }
    
    /**
     * 生成 AI 响应（支持会话）
     * @param context 上下文
     * @param question 问题
     * @param sessionId 会话ID
     * @return AI 响应
     */
    public String generateResponseWithContext(String context, String question, String sessionId) {
        try {
            log.info("生成 AI 响应，会话ID：{}", sessionId);
            
            // 验证参数
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return "请提供有效的问题。";
            }
            
            // 构建会话上下文
            String fullContext = buildSessionContext(context, question, sessionId);
            
            // 检查缓存（使用会话ID作为缓存键的一部分）
            String cacheKey = (sessionId != null ? sessionId + ":" : "") + question.trim();
            String cachedResponse = aiCacheManager.get(cacheKey);
            if (cachedResponse != null) {
                // 更新会话消息
                updateSessionMessages(sessionId, question, cachedResponse);
                return cachedResponse;
            }
            
            // 构建完整的提示信息
            String prompt = "基于以下上下文，回答用户问题：\n\n" + fullContext + "\n\n用户问题：" + question;
            
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
                        log.error("生成响应失败：{}", error.getMessage(), error);
                        // 可以在这里添加错误处理逻辑，例如设置错误标志等
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        // 完成生成，可以在这里添加额外的处理逻辑
                        log.info("响应生成完成，响应ID：{}", response.id());
                    }
                });
                log.info("AI 响应生成成功");
                // 缓存响应
                aiCacheManager.put(cacheKey, fullResponse.toString());
                // 更新会话消息
                updateSessionMessages(sessionId, question, fullResponse.toString());
                return fullResponse.toString();
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成响应");
                String mockResponse = "这是一个模拟的 AI 响应：" + prompt;
                // 缓存模拟响应
                aiCacheManager.put(cacheKey, mockResponse);
                // 更新会话消息
                updateSessionMessages(sessionId, question, mockResponse);
                return mockResponse;
            }
        } catch (IllegalArgumentException e) {
            return errorHandler.handleParameterError(e.getMessage(), e);
        } catch (RuntimeException e) {
            return errorHandler.handleRuntimeError(e.getMessage(), e);
        } catch (Exception e) {
            return errorHandler.handleGeneralError(e.getMessage(), e);
        }
    }

    @Override
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question) {
        return generateStreamResponseWithContext(context, question, null);
    }
    
    /**
     * 生成 AI 流式响应（支持会话）
     * @param context 上下文
     * @param question 问题
     * @param sessionId 会话ID
     * @return 流式响应
     */
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question, String sessionId) {
        try {
            log.info("生成 AI 流式响应，会话ID：{}", sessionId);
            
            // 验证参数
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return Flux.create(sink -> {
                    sink.next(SseMessage.error("请提供有效的问题。"));
                    sink.complete();
                });
            }
            
            // 构建会话上下文
            String fullContext = buildSessionContext(context, question, sessionId);
            
            // 检查缓存（使用会话ID作为缓存键的一部分）
            String cacheKey = (sessionId != null ? sessionId + ":" : "") + question.trim();
            String cachedResponse = aiCacheManager.get(cacheKey);
            if (cachedResponse != null) {
                log.info("从缓存中获取流式响应");
                // 更新会话消息
                updateSessionMessages(sessionId, question, cachedResponse);
                // 流式输出缓存的响应
                return Flux.generate(
                    () -> 0,
                    (state, sink) -> {
                        try {
                            if (state >= cachedResponse.length()) {
                                sink.next(SseMessage.complete());
                                sink.complete();
                                return -1;
                            }
                            int end = Math.min(state + streamChunkSize, cachedResponse.length());
                            String chunk = cachedResponse.substring(state, end);
                            sink.next(SseMessage.data(chunk));
                            return end;
                        } catch (Exception e) {
                            log.error("流式输出缓存响应失败：{}", e.getMessage(), e);
                            sink.error(new RuntimeException("处理缓存响应失败：" + e.getMessage()));
                            return -1;
                        }
                    }
                );
            }
            
            // 构建完整的提示信息
            String prompt = "基于以下上下文，回答用户问题：\n\n" + fullContext + "\n\n用户问题：" + question;
            
            if (streamingChatModel != null) {
                log.info("使用 OpenAI 流式 API 生成响应");
                Sinks.Many<SseMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
                
                // 流式生成响应
                StringBuilder fullResponse = new StringBuilder();
                streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            // 处理每个生成的块
                            fullResponse.append(partialResponse);
                            // 发送块到客户端，使用非阻塞方式
                            Sinks.EmitResult result = sink.tryEmitNext(SseMessage.data(partialResponse));
                            if (result.isFailure()) {
                                log.warn("发送部分响应失败：{}", result);
                            }
                        } catch (Exception e) {
                            log.error("发送部分响应失败：{}", e.getMessage(), e);
                            sink.tryEmitNext(SseMessage.error("发送响应失败：" + e.getMessage()));
                            sink.tryEmitComplete();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        // 处理错误
                        log.error("流式生成失败：{}", error.getMessage(), error);
                        sink.tryEmitNext(SseMessage.error("生成响应失败：" + error.getMessage()));
                        sink.tryEmitComplete();
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        try {
                            // 完成生成
                            log.info("流式生成完成");
                            // 缓存完整响应
                            aiCacheManager.put(cacheKey, fullResponse.toString());
                            // 更新会话消息
                            updateSessionMessages(sessionId, question, fullResponse.toString());
                            sink.tryEmitNext(SseMessage.complete());
                            sink.tryEmitComplete();
                        } catch (Exception e) {
                            log.error("完成流式生成失败：{}", e.getMessage(), e);
                            sink.tryEmitNext(SseMessage.error("处理响应完成失败：" + e.getMessage()));
                            sink.tryEmitComplete();
                        }
                    }
                });
                
                return sink.asFlux()
                    // 添加超时处理
                    .timeout(Duration.ofMinutes(2))
                    // 错误处理
                    .onErrorResume(error -> {
                        log.error("流式响应错误：{}", error.getMessage(), error);
                        return Flux.just(SseMessage.error("生成响应失败：" + error.getMessage()));
                    })
                    // 添加背压处理
                    .onBackpressureBuffer(streamBufferSize);
            } else {
                // 如果模型未初始化，使用模拟响应
                log.warn("使用模拟 AI 生成流式响应");
                String mockResponse = "这是一个模拟的 AI 流式响应：" + prompt;
                aiCacheManager.put(cacheKey, mockResponse);
                // 更新会话消息
                updateSessionMessages(sessionId, question, mockResponse);
                
                // 流式输出模拟响应，使用更高效的 Flux.generate
                return Flux.generate(
                    () -> 0,
                    (state, sink) -> {
                        try {
                            if (state >= mockResponse.length()) {
                                sink.next(SseMessage.complete());
                                sink.complete();
                                return -1;
                            }
                            int end = Math.min(state + streamChunkSize, mockResponse.length());
                            String chunk = mockResponse.substring(state, end);
                            sink.next(SseMessage.data(chunk));
                            return end;
                        } catch (Exception e) {
                            log.error("流式输出模拟响应失败：{}", e.getMessage(), e);
                            sink.error(new RuntimeException("处理模拟响应失败：" + e.getMessage()));
                            return -1;
                        }
                    }
                );
            }
        } catch (IllegalArgumentException e) {
            log.error("参数错误：{}", e.getMessage(), e);
            return Flux.create(sink -> {
                sink.next(SseMessage.error("参数错误：" + e.getMessage()));
                sink.complete();
            });
        } catch (RuntimeException e) {
            log.error("运行时错误：{}", e.getMessage(), e);
            return Flux.create(sink -> {
                sink.next(SseMessage.error("系统运行错误，请稍后重试。"));
                sink.complete();
            });
        } catch (Exception e) {
            log.error("生成流式响应失败：{}", e.getMessage(), e);
            // 流式输出错误响应
            return Flux.create(sink -> {
                sink.next(SseMessage.error("生成响应失败：" + e.getMessage()));
                sink.complete();
            });
        }
    }
    
    /**
     * 构建会话上下文
     * @param context 原始上下文
     * @param question 当前问题
     * @param sessionId 会话ID
     * @return 完整的会话上下文
     */
    private String buildSessionContext(String context, String question, String sessionId) {
        StringBuilder sessionContext = new StringBuilder();
        
        // 添加原始上下文
        if (context != null && !context.trim().isEmpty()) {
            sessionContext.append(context).append("\n\n");
        }
        
        // 添加会话历史
        SessionManager.Session session = sessionManager.getSession(sessionId);
        if (session != null) {
            List<String> messages = session.getMessages();
            if (!messages.isEmpty()) {
                sessionContext.append("## 会话历史\n\n");
                for (String message : messages) {
                    sessionContext.append(message).append("\n\n");
                }
            }
        }
        
        return sessionContext.toString();
    }
    
    /**
     * 更新会话消息
     * @param sessionId 会话ID
     * @param question 问题
     * @param response 响应
     */
    private void updateSessionMessages(String sessionId, String question, String response) {
        sessionManager.addMessage(sessionId, "用户：" + question);
        sessionManager.addMessage(sessionId, "AI：" + response);
    }

    @Override
    public String generateResponseFromImage(MultipartFile image, String question) {
        try {
            log.info("从图像生成 AI 响应");
            
            // 验证参数
            if (image == null || image.isEmpty()) {
                log.warn("图像参数为空");
                return "请提供有效的图像。";
            }
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return "请提供有效的问题。";
            }
            
            // 检查文件类型
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("无效的文件类型：{}", contentType);
                return "请提供有效的图像文件。";
            }
            
            // 构建提示信息
            String prompt = "基于以下图像，回答用户问题：\n\n用户问题：" + question;
            
            // 使用 LangChain4j 生成响应
            if (streamingChatModel != null) {
                // 注意：这里需要集成图像识别能力，暂时使用模拟响应
                log.info("使用模拟 AI 从图像生成响应");
                String mockResponse = "这是一个基于图像的模拟 AI 响应：" + prompt;
                return mockResponse;
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成响应");
                String mockResponse = "这是一个模拟的 AI 响应：" + prompt;
                return mockResponse;
            }
        } catch (IllegalArgumentException e) {
            return errorHandler.handleParameterError(e.getMessage(), e);
        } catch (Exception e) {
            return errorHandler.handleImageError(e.getMessage(), e);
        }
    }

    @Override
    public Flux<SseMessage> generateStreamResponseFromImage(MultipartFile image, String question) {
        try {
            log.info("流式从图像生成 AI 响应");
            
            // 验证参数
            if (image == null || image.isEmpty()) {
                log.warn("图像参数为空");
                return Flux.create(sink -> {
                    sink.next(SseMessage.error("请提供有效的图像。"));
                    sink.complete();
                });
            }
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return Flux.create(sink -> {
                    sink.next(SseMessage.error("请提供有效的问题。"));
                    sink.complete();
                });
            }
            
            // 检查文件类型
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("无效的文件类型：{}", contentType);
                return Flux.create(sink -> {
                    sink.next(SseMessage.error("请提供有效的图像文件。"));
                    sink.complete();
                });
            }
            
            // 构建提示信息
            String prompt = "基于以下图像，回答用户问题：\n\n用户问题：" + question;
            
            // 使用 LangChain4j 生成流式响应
            if (streamingChatModel != null) {
                // 注意：这里需要集成图像识别能力，暂时使用模拟响应
                log.info("使用模拟 AI 流式从图像生成响应");
                String mockResponse = "这是一个基于图像的模拟 AI 流式响应：" + prompt;
                
                // 流式输出模拟响应
                return Flux.generate(
                    () -> 0,
                    (state, sink) -> {
                        try {
                            if (state >= mockResponse.length()) {
                                sink.next(SseMessage.complete());
                                sink.complete();
                                return -1;
                            }
                            int end = Math.min(state + streamChunkSize, mockResponse.length());
                            String chunk = mockResponse.substring(state, end);
                            sink.next(SseMessage.data(chunk));
                            return end;
                        } catch (Exception e) {
                            log.error("流式输出模拟响应失败：{}", e.getMessage(), e);
                            sink.error(new RuntimeException("处理模拟响应失败：" + e.getMessage()));
                            return -1;
                        }
                    }
                );
            } else {
                // 如果模型未初始化，使用模拟响应
                log.warn("使用模拟 AI 生成流式响应");
                String mockResponse = "这是一个模拟的 AI 流式响应：" + prompt;
                
                // 流式输出模拟响应
                return Flux.generate(
                    () -> 0,
                    (state, sink) -> {
                        try {
                            if (state >= mockResponse.length()) {
                                sink.next(SseMessage.complete());
                                sink.complete();
                                return -1;
                            }
                            int end = Math.min(state + streamChunkSize, mockResponse.length());
                            String chunk = mockResponse.substring(state, end);
                            sink.next(SseMessage.data(chunk));
                            return end;
                        } catch (Exception e) {
                            log.error("流式输出模拟响应失败：{}", e.getMessage(), e);
                            sink.error(new RuntimeException("处理模拟响应失败：" + e.getMessage()));
                            return -1;
                        }
                    }
                );
            }
        } catch (IllegalArgumentException e) {
            String errorMessage = errorHandler.handleParameterError(e.getMessage(), e);
            return Flux.create(sink -> {
                sink.next(SseMessage.error(errorMessage));
                sink.complete();
            });
        } catch (Exception e) {
            String errorMessage = errorHandler.handleImageError(e.getMessage(), e);
            return Flux.create(sink -> {
                sink.next(SseMessage.error(errorMessage));
                sink.complete();
            });
        }
    }

    @Override
    public BookRecommendationDTO getBookRecommendations(Long bookId) {
        try {
            log.info("获取书籍推荐，书籍ID：{}", bookId);
            
            // 验证参数
            if (bookId == null || bookId <= 0) {
                log.warn("无效的书籍ID：{}", bookId);
                return null;
            }
            
            // 构建提示信息
            String prompt = "基于书籍ID为" + bookId + "的书籍，推荐5本相关书籍，包括书名、作者和推荐理由。";
            
            // 使用 LangChain4j 生成响应
            if (streamingChatModel != null) {
                // 注意：这里需要集成书籍推荐逻辑，暂时使用模拟响应
                log.info("使用模拟 AI 生成书籍推荐");
                BookRecommendationDTO recommendation = new BookRecommendationDTO();
                recommendation.setRecommendedBooks(new ArrayList<>());
                recommendation.setUserReviews(new ArrayList<>());
                return recommendation;
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成书籍推荐");
                BookRecommendationDTO recommendation = new BookRecommendationDTO();
                recommendation.setRecommendedBooks(new ArrayList<>());
                recommendation.setUserReviews(new ArrayList<>());
                return recommendation;
            }
        } catch (IllegalArgumentException e) {
            errorHandler.handleBookRecommendationError(e.getMessage(), e);
            return null;
        } catch (Exception e) {
            errorHandler.handleBookRecommendationError(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public KnowledgeGraphDTO generateKnowledgeGraph(Long bookId) {
        try {
            log.info("生成知识图谱，书籍ID：{}", bookId);
            
            // 验证参数
            if (bookId == null || bookId <= 0) {
                log.warn("无效的书籍ID：{}", bookId);
                return null;
            }
            
            // 构建提示信息
            String prompt = "基于书籍ID为" + bookId + "的书籍，生成知识图谱，包括核心概念、关系和层次结构。";
            
            // 使用 LangChain4j 生成响应
            if (streamingChatModel != null) {
                // 注意：这里需要集成知识图谱生成逻辑，暂时使用模拟响应
                log.info("使用模拟 AI 生成知识图谱");
                KnowledgeGraphDTO knowledgeGraph = new KnowledgeGraphDTO();
                knowledgeGraph.setNodes(new ArrayList<>());
                knowledgeGraph.setRelations(new ArrayList<>());
                return knowledgeGraph;
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成知识图谱");
                KnowledgeGraphDTO knowledgeGraph = new KnowledgeGraphDTO();
                knowledgeGraph.setNodes(new ArrayList<>());
                knowledgeGraph.setRelations(new ArrayList<>());
                return knowledgeGraph;
            }
        } catch (IllegalArgumentException e) {
            errorHandler.handleKnowledgeGraphError(e.getMessage(), e);
            return null;
        } catch (Exception e) {
            errorHandler.handleKnowledgeGraphError(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateExercises(String knowledgePoint, String difficulty) {
        try {
            log.info("生成练习，知识点：{}，难度：{}", knowledgePoint, difficulty);
            
            // 验证参数
            if (knowledgePoint == null || knowledgePoint.trim().isEmpty()) {
                log.warn("知识点参数为空");
                return "请提供有效的知识点。";
            }
            if (difficulty == null || difficulty.trim().isEmpty()) {
                log.warn("难度参数为空");
                return "请提供有效的难度级别。";
            }
            
            // 构建提示信息
            String prompt = "基于知识点\"" + knowledgePoint + "\"，生成难度为\"" + difficulty + "\"的练习题目，包括选择题、填空题和简答题。";
            
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
                        log.error("生成练习失败：{}", error.getMessage(), error);
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        log.info("练习生成完成，响应ID：{}", response.id());
                    }
                });
                log.info("练习生成成功");
                return fullResponse.toString();
            } else {
                // 如果模型未初始化，返回模拟响应
                log.warn("使用模拟 AI 生成练习");
                String mockResponse = "这是一个模拟的练习生成响应：" + prompt;
                return mockResponse;
            }
        } catch (IllegalArgumentException e) {
            return errorHandler.handleParameterError(e.getMessage(), e);
        } catch (Exception e) {
            return errorHandler.handleGeneralError(e.getMessage(), e);
        }
    }
}
