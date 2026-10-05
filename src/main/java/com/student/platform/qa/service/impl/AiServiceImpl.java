package com.student.platform.qa.service.impl;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.Delta;
import ai.z.openapi.service.model.ImageUrl;
import ai.z.openapi.service.model.MessageContent;
import io.reactivex.rxjava3.disposables.Disposable;
import com.student.platform.common.dto.BookRecommendationDTO;
import com.student.platform.common.dto.KnowledgeGraphDTO;
import com.student.platform.common.dto.SseMessage;
import com.student.platform.qa.service.AiService;
import com.student.platform.qa.service.util.AiCacheManager;
import com.student.platform.qa.service.util.ErrorHandler;
import com.student.platform.qa.service.util.SessionManager;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
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
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    @Value("${ai.stream.chunk-size:100}")
    private int streamChunkSize;

    @Value("${ai.stream.buffer-size:1024}")
    private int streamBufferSize;

    @Value("${langchain4j.open-ai.vision-chat-model.api-key:}")
    private String visionApiKey;

    private final StreamingChatModel streamingChatModel;
    private final StreamingChatModel visionChatModel;
    private final AiCacheManager aiCacheManager;
    private final SessionManager sessionManager;
    private final ErrorHandler errorHandler;

    @Autowired
    public AiServiceImpl(
            StreamingChatModel streamingChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("visionStreamingChatModel")
            org.springframework.beans.factory.ObjectProvider<StreamingChatModel> visionChatModelProvider,
            AiCacheManager aiCacheManager,
            SessionManager sessionManager,
            ErrorHandler errorHandler) {
        log.info("初始化 AI 服务（使用 LangChain4j Spring Boot Starter）");
        this.streamingChatModel = streamingChatModel;
        // GLM_API_KEY 未配置时视觉模型 bean 不存在，注入 null，业务侧已有 null 判断降级
        this.visionChatModel = visionChatModelProvider.getIfAvailable();
        this.aiCacheManager = aiCacheManager;
        this.sessionManager = sessionManager;
        this.errorHandler = errorHandler;
    }

    @Override
    public String createSession() {
        return sessionManager.createSession();
    }

    @Override
    public String generateResponseWithContext(String context, String question) {
        return generateResponseWithContext(context, question, null);
    }

    @Override
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question) {
        return generateStreamResponseWithContextInternal(context, question, null);
    }

    @Override
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question, String sessionId) {
        return generateStreamResponseWithContextInternal(context, question, sessionId);
    }

    @Override
    public Flux<SseMessage> generateStreamResponseWithContext(String context, String question, 
            java.util.function.Consumer<String> onComplete) {
        return Flux.concat(
            generateStreamResponseWithContextInternal(context, question, null),
            Flux.defer(() -> {
                if (onComplete != null) {
                    try {
                        onComplete.accept(null);
                    } catch (Exception e) {
                        log.error("执行完成回调失败：{}", e.getMessage(), e);
                    }
                }
                return Flux.empty();
            })
        );
    }

    public String generateResponseWithContext(String context, String question, String sessionId) {
        try {
            log.info("生成 AI 响应，会话ID：{}", sessionId);

            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return "请提供有效的问题。";
            }

            String fullContext = buildSessionContext(context, question, sessionId);
            String cacheKey = buildCacheKey(sessionId, question);
            String cachedResponse = aiCacheManager.get(cacheKey);
            
            if (cachedResponse != null) {
                updateSessionMessages(sessionId, question, cachedResponse);
                return cachedResponse;
            }

            String prompt = buildPrompt(fullContext, question);

            if (streamingChatModel != null) {
                return generateWithModel(prompt, cacheKey, sessionId, question);
            } else {
                log.warn("AI模型未初始化");
                return "AI服务暂时不可用，请稍后重试。";
            }
        } catch (Exception e) {
            log.error("生成响应异常: {}", e.getMessage(), e);
            return "AI响应生成异常，请稍后重试。";
        }
    }

    public Flux<SseMessage> generateStreamResponseWithContextInternal(String context, String question, String sessionId) {
        try {
            log.info("生成 AI 流式响应，会话ID：{}", sessionId);

            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return Flux.just(SseMessage.error("请提供有效的问题。"));
            }

            String fullContext = buildSessionContext(context, question, sessionId);
            String cacheKey = buildCacheKey(sessionId, question);
            String cachedResponse = aiCacheManager.get(cacheKey);
            
            Flux<SseMessage> responseFlux;
            if (cachedResponse != null) {
                log.info("从缓存中获取流式响应");
                updateSessionMessages(sessionId, question, cachedResponse);
                responseFlux = createCachedStream(cachedResponse);
            } else {
                String prompt = buildPrompt(fullContext, question);

                if (streamingChatModel != null) {
                    responseFlux = createStreamWithModel(prompt, cacheKey, sessionId, question);
                } else {
                    log.warn("AI模型未初始化，使用模拟响应");
                    String mockResponse = "这是一个模拟的 AI 流式响应：" + prompt;
                    aiCacheManager.put(cacheKey, mockResponse);
                    updateSessionMessages(sessionId, question, mockResponse);
                    responseFlux = createCachedStream(mockResponse);
                }
            }
            
            // 返回响应流（sessionId通过SseMessage对象的字段传递，不在消息内容中）
            return responseFlux;
        } catch (IllegalArgumentException e) {
            log.error("参数错误：{}", e.getMessage(), e);
            return Flux.just(SseMessage.error("参数错误：" + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("运行时错误：{}", e.getMessage(), e);
            return Flux.just(SseMessage.error("系统运行错误，请稍后重试。"));
        } catch (Exception e) {
            log.error("生成流式响应失败：{}", e.getMessage(), e);
            return Flux.just(SseMessage.error("生成响应失败：" + e.getMessage()));
        }
    }

    @Override
    public String generateResponseFromImage(MultipartFile image, String question) {
        try {
            log.info("从图像生成 AI 响应");

            if (image == null || image.isEmpty()) {
                log.warn("图像参数为空");
                return "请提供有效的图像。";
            }
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return "请提供有效的问题。";
            }

            String contentType = image.getContentType();
            if (!contentType.startsWith("image/")) {
                log.warn("无效的文件类型：{}", contentType);
                return "请提供有效的图像文件。";
            }

            if (visionChatModel == null) {
                log.error("视觉模型未初始化，请检查API Key配置");
                return "图片问答功能未启用，请联系管理员配置视觉模型。";
            }

            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.info("图像转换为base64成功，大小: {}KB", imageBytes.length / 1024);

            UserMessage userMessage = UserMessage.from(
                    TextContent.from(question)
            );

            List<dev.langchain4j.data.message.ChatMessage> chatMessages = List.of(userMessage);

            log.info("发送多模态消息到GLM-4.6V模型");

            StringBuilder fullResponse = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            visionChatModel.chat(chatMessages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    fullResponse.append(partialResponse);
                }

                @Override
                public void onError(Throwable error) {
                    log.error("GLM-4.6V生成响应失败：{}", error.getMessage(), error);
                    errorRef.set(error);
                    latch.countDown();
                }

                @Override
                public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                    log.info("GLM-4.6V响应生成完成，响应ID：{}", response.id());
                    latch.countDown();
                }
            });

            boolean completed = latch.await(300, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("GLM-4.6V生成响应超时");
                return "AI响应超时，请稍后重试。";
            }

            if (errorRef.get() != null) {
                log.error("GLM-4.6V生成响应发生错误: {}", errorRef.get().getMessage());
                return "AI服务暂时不可用，请稍后重试。";
            }

            String response = fullResponse.toString().trim();
            if (response.isEmpty()) {
                log.warn("GLM-4.6V返回空响应");
                return "AI返回空响应，请重新提问。";
            }

            log.info("GLM-4.6V响应生成成功，响应长度: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("生成响应异常: {}", e.getMessage(), e);
            return "调用AI服务失败：" + e.getMessage();
        }
    }

    @Override
    public Flux<SseMessage> generateStreamResponseFromImage(MultipartFile image, String question) {
        try {
            log.info("流式从图像生成 AI 响应");

            if (image == null || image.isEmpty()) {
                log.warn("图像参数为空");
                return Flux.just(SseMessage.error("请提供有效的图像。"));
            }
            if (question == null || question.trim().isEmpty()) {
                log.warn("问题参数为空");
                return Flux.just(SseMessage.error("请提供有效的问题。"));
            }

            String contentType = image.getContentType();
            if (!contentType.startsWith("image/")) {
                log.warn("无效的文件类型：{}", contentType);
                return Flux.just(SseMessage.error("请提供有效的图像文件。"));
            }

            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.info("图像转换为base64成功，大小: {}KB", imageBytes.length / 1024);

            return createImageStream(base64Image, question);
        } catch (IllegalArgumentException e) {
            String errorMessage = errorHandler.handleParameterError(e.getMessage(), e);
            return Flux.just(SseMessage.error(errorMessage));
        } catch (Exception e) {
            String errorMessage = errorHandler.handleImageError(e.getMessage(), e);
            return Flux.just(SseMessage.error(errorMessage));
        }
    }

    @Override
    public BookRecommendationDTO getBookRecommendations(Long bookId) {
        try {
            log.info("获取书籍推荐，书籍ID：{}", bookId);

            if (bookId == null || bookId <= 0) {
                log.warn("无效的书籍ID：{}", bookId);
                return null;
            }

            BookRecommendationDTO recommendation = new BookRecommendationDTO();
            recommendation.setRecommendedBooks(new java.util.ArrayList<>());
            recommendation.setUserReviews(new java.util.ArrayList<>());
            return recommendation;
        } catch (Exception e) {
            errorHandler.handleBookRecommendationError(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public KnowledgeGraphDTO generateKnowledgeGraph(Long bookId) {
        try {
            log.info("生成知识图谱，书籍ID：{}", bookId);

            if (bookId == null || bookId <= 0) {
                log.warn("无效的书籍ID：{}", bookId);
                return null;
            }

            KnowledgeGraphDTO graph = new KnowledgeGraphDTO();
            graph.setNodes(new java.util.ArrayList<>());
            graph.setRelations(new java.util.ArrayList<>());
            return graph;
        } catch (Exception e) {
            errorHandler.handleKnowledgeGraphError(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateExercises(String knowledgePoint, String difficulty) {
        try {
            log.info("生成练习题，知识点：{}，难度：{}", knowledgePoint, difficulty);

            if (knowledgePoint == null || knowledgePoint.trim().isEmpty()) {
                log.warn("知识点参数为空");
                return "{\"error\": \"请提供有效的知识点。\"}";
            }

            if (difficulty == null || difficulty.trim().isEmpty()) {
                difficulty = "MEDIUM";
            }

            String difficultyDesc = switch (difficulty.toUpperCase()) {
                case "BASIC", "EASY", "basic" -> "基础";
                case "ADVANCED", "HARD", "advanced" -> "进阶";
                default -> "中等";
            };

            String systemPrompt = """
                你是一位专业的教育专家，擅长设计高质量的练习题目。
                请根据用户的要求，生成指定数量和类型的练习题目。
                
                输出格式要求（JSON数组）：
                {
                  "exercises": [
                    {
                      "type": "SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|FILL_BLANK|SHORT_ANSWER|PROGRAMMING",
                      "title": "题目内容",
                      "description": "题目详细描述（含公式）",
                      "options": ["A. 选项内容", "B. 选项内容", "C. 选项内容", "D. 选项内容"],
                      "answer": "正确答案",
                      "analysis": "解析说明"
                    }
                  ]
                }
                
                注意：
                1. 只输出JSON格式，不要包含其他文字
                2. 单选题和多选题必须有options字段
                3. 判断题options为["A. 正确","B. 错误"]
                4. 填空题、简答题、编程题不需要options字段
                5. 每道题必须包含title、description、answer、analysis字段
                6. 所有数学公式必须用 $...$ 包裹，例如 $\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1$
                """;

            String userPrompt = String.format("""
                请为以下主题生成5道练习题：
                
                课程名称：%s
                主题：%s
                难度：%s
                题目类型：单选题, 多选题, 判断题, 填空题
                
                要求：
                1. 题目要有实际意义，考察核心知识点
                2. 答案要准确，解析要详细
                3. 难度要符合要求
                4. 只输出JSON格式
                5. 所有数学公式必须用 $...$ 包裹（重要！）
                """,
                knowledgePoint,
                knowledgePoint,
                difficultyDesc
            );

            if (streamingChatModel != null) {
                String fullPrompt = systemPrompt + "\n\n" + userPrompt;
                
                StringBuilder fullResponse = new StringBuilder();
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                streamingChatModel.chat(fullPrompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        fullResponse.append(partialResponse);
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("生成练习题失败：{}", error.getMessage(), error);
                        errorRef.set(error);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        log.info("练习题生成完成");
                        latch.countDown();
                    }
                });

                boolean completed = latch.await(120, TimeUnit.SECONDS);
                if (!completed) {
                    log.warn("生成练习题超时");
                    return "{\"error\": \"练习题生成超时，请稍后重试。\"}";
                }

                if (errorRef.get() != null) {
                    log.error("生成练习题发生错误: {}", errorRef.get().getMessage());
                    return "{\"error\": \"练习题生成失败，请稍后重试。\"}";
                }

                String response = fullResponse.toString().trim();
                if (response.isEmpty()) {
                    log.warn("AI返回空响应");
                    return "{\"error\": \"未生成练习题，请重新提问。\"}";
                }

                log.info("练习题生成成功，响应长度: {}", response.length());
                return response;
            } else {
                log.warn("AI模型未初始化，使用模拟响应");
                return "{\"error\": \"AI服务暂时不可用，请稍后重试。\"}";
            }
        } catch (Exception e) {
            log.error("生成练习题异常: {}", e.getMessage(), e);
            return "{\"error\": \"生成练习题异常，请稍后重试。\"}";
        }
    }

    private String buildSessionContext(String context, String question, String sessionId) {
        StringBuilder sessionContext = new StringBuilder();
        if (context != null && !context.trim().isEmpty()) {
            sessionContext.append(context).append("\n\n");
        }

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

    private String buildCacheKey(String sessionId, String question) {
        return (sessionId != null ? sessionId + ":" : "") + question.trim();
    }

    private String buildPrompt(String context, String question) {
        return "基于以下上下文，回答用户问题：\n\n" + context + "\n\n用户问题：" + question;
    }

    private String generateWithModel(String prompt, String cacheKey, String sessionId, String question) {
        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullResponse.append(partialResponse);
            }

            @Override
            public void onError(Throwable error) {
                log.error("生成响应失败：{}", error.getMessage(), error);
                errorRef.set(error);
                latch.countDown();
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                log.info("响应生成完成，响应ID：{}", response.id());
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(120, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("生成响应超时");
                return "AI响应超时，请稍后重试。";
            }

            if (errorRef.get() != null) {
                log.error("生成响应发生错误: {}", errorRef.get().getMessage());
                return "AI响应生成失败，请稍后重试。";
            }

            String response = fullResponse.toString().trim();
            if (response.isEmpty()) {
                log.warn("AI返回空响应");
                return "AI返回空响应，请重新提问。";
            }

            log.info("AI 响应生成成功，响应长度: {}", response.length());
            aiCacheManager.put(cacheKey, response);
            updateSessionMessages(sessionId, question, response);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("生成响应被中断");
            return "AI响应生成被中断，请稍后重试。";
        }
    }

    private Flux<SseMessage> createCachedStream(String cachedResponse) {
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

    private Flux<SseMessage> createStreamWithModel(String prompt, String cacheKey, String sessionId, String question) {
        log.info("使用流式 API 生成响应");
        Sinks.Many<SseMessage> sink = Sinks.many().unicast().onBackpressureBuffer();

        StringBuilder fullResponse = new StringBuilder();
        StringBuilder batchBuffer = new StringBuilder();
        
        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    fullResponse.append(partialResponse);
                    batchBuffer.append(partialResponse);
                    
                    if (batchBuffer.length() >= 15 || partialResponse.contains("\n")) {
                        String batchContent = batchBuffer.toString();
                        batchBuffer.setLength(0);
                        sink.tryEmitNext(SseMessage.data(batchContent));
                    }
                } catch (Exception e) {
                    log.error("发送部分响应失败：{}", e.getMessage(), e);
                    sink.tryEmitNext(SseMessage.error("发送响应失败：" + e.getMessage()));
                    sink.tryEmitComplete();
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式生成失败：{}", error.getMessage(), error);
                sink.tryEmitNext(SseMessage.error("生成响应失败：" + error.getMessage()));
                sink.tryEmitComplete();
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                try {
                    if (batchBuffer.length() > 0) {
                        sink.tryEmitNext(SseMessage.data(batchBuffer.toString()));
                    }
                    log.info("流式生成完成");
                    aiCacheManager.put(cacheKey, fullResponse.toString());
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
                .timeout(Duration.ofMinutes(2))
                .onErrorResume(error -> {
                    log.error("流式响应错误：{}", error.getMessage(), error);
                    return Flux.just(SseMessage.error("生成响应失败：" + error.getMessage()));
                })
                .onBackpressureBuffer(streamBufferSize);
    }

    private Flux<SseMessage> createImageStream(String base64Image, String question) {
        Sinks.Many<SseMessage> sink = Sinks.many().unicast().onBackpressureBuffer();

        if (visionApiKey == null || visionApiKey.isEmpty()) {
            log.error("视觉模型API Key未配置");
            return Flux.just(SseMessage.error("图片问答功能未启用，请联系管理员配置视觉模型。"));
        }

        log.info("使用智谱GLM-4.6V SDK从图像生成响应");

        try {
            ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
                    .apiKey(visionApiKey)
                    .build();

            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model("glm-4.6v")
                    .stream(true)
                    .messages(List.of(
                            ChatMessage.builder()
                                    .role(ChatMessageRole.USER.value())
                                    .content(List.of(
                                            MessageContent.builder()
                                                    .type("image_url")
                                                    .imageUrl(ImageUrl.builder()
                                                            .url(base64Image)
                                                            .build())
                                                    .build(),
                                            MessageContent.builder()
                                                    .type("text")
                                                    .text(question)
                                                    .build()))
                                    .build()))
                    .build();

            log.info("发送多模态消息到GLM-4.6V模型，问题: {}, 图片大小: {}KB", 
                    question, base64Image.length() / 1024 / 4);

            ChatCompletionResponse response = client.chat().createChatCompletion(request);

            log.info("GLM-4.6V响应状态: success={}, msg={}", response.isSuccess(), response.getMsg());

            if (response.isSuccess()) {
                StringBuilder fullResponse = new StringBuilder();
                StringBuilder buffer = new StringBuilder();

                final Disposable[] subscriptionHolder = new Disposable[1];

                subscriptionHolder[0] = response.getFlowable().subscribe(
                        data -> {
                            if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                                Delta delta = data.getChoices().get(0).getDelta();
                                if (delta != null && delta.getContent() != null) {
                                    String partialContent = delta.getContent();
                                    fullResponse.append(partialContent);
                                    buffer.append(partialContent);

                                    if (buffer.length() >= 10 || partialContent.contains("\n")) {
                                        String batchContent = buffer.toString();
                                        buffer.setLength(0);
                                        sink.tryEmitNext(SseMessage.data(batchContent));
                                    }
                                }
                            }
                        },
                        error -> {
                            log.error("GLM-4.6V流式响应错误：{}", error.getMessage(), error);
                            sink.tryEmitNext(SseMessage.error("AI服务暂时不可用，请稍后重试。"));
                            sink.tryEmitComplete();
                        },
                        () -> {
                            if (!buffer.isEmpty()) {
                                sink.tryEmitNext(SseMessage.data(buffer.toString()));
                            }
                            log.info("GLM-4.6V流式生成完成，响应长度: {}", fullResponse.length());
                            sink.tryEmitNext(SseMessage.complete());
                            sink.tryEmitComplete();
                            if (subscriptionHolder[0] != null) {
                                subscriptionHolder[0].dispose();
                            }
                        }
                );
            } else {
                log.error("GLM-4.6V请求失败: {}", response.getMsg());
                sink.tryEmitNext(SseMessage.error("AI服务暂时不可用，请稍后重试。"));
                sink.tryEmitComplete();
            }

            return sink.asFlux()
                    .timeout(Duration.ofMinutes(5))
                    .onErrorResume(error -> {
                        log.error("GLM-4.6V流式响应错误：{}", error.getMessage(), error);
                        return Flux.just(SseMessage.error("AI服务暂时不可用，请稍后重试。"));
                    })
                    .onBackpressureBuffer();
        } catch (Exception e) {
            log.error("调用GLM-4.6V失败：{}", e.getMessage(), e);
            return Flux.just(SseMessage.error("调用AI服务失败：" + e.getMessage()));
        }
    }

    private void updateSessionMessages(String sessionId, String question, String response) {
        sessionManager.addMessage(sessionId, "用户：" + question);
        sessionManager.addMessage(sessionId, "AI：" + response);
    }
}