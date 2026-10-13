package com.student.platform.qa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.common.config.CurrentUserProvider;
import com.student.platform.common.dto.QaRequest;
import com.student.platform.common.dto.QaResponse;
import com.student.platform.common.dto.QaResultDTO;
import com.student.platform.common.dto.QaSessionDTO;
import com.student.platform.common.result.Result;
import com.student.platform.common.dto.SseMessage;

import com.student.platform.qa.service.AiService;
import com.student.platform.qa.service.ConversationService;
import com.student.platform.qa.service.ContextService;
import com.student.platform.qa.service.CourseRecommendService;
import com.student.platform.qa.service.MessageService;
import com.student.platform.qa.service.QaService;
import com.student.platform.qa.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI问答", description = "AI问答相关接口")
public class QaController {

    private final QaService qaService;
    private final AiService aiService;

    /**
     * 非流式问答的等待池。按名注入而不用 final + @Qualifier：
     * 类上是 @RequiredArgsConstructor 且仓库没有 lombok.config，
     * @Qualifier 不会被复制进构造参数，而容器里有 3 个 Executor Bean，
     * 按类型注入会直接歧义失败。
     */
    @Resource(name = "aiRequestExecutor")
    private Executor aiRequestExecutor;
    private final CourseRecommendService courseRecommendService;
    private final SessionService sessionService;
    private final ContextService contextService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 安全基线：userId 一律取自 {@link CurrentUserProvider}（JwtAuthFilter 注入的 X-User-Id /
     * SecurityContext），**绝不接受**请求参数或请求体传入的 userId，避免水平越权。
     *
     * 对于允许匿名使用的 AI 问答接口，取不到用户时返回 null（仅不落历史），而不是回落请求参数。
     */
    private Long currentUserIdOrNull() {
        CurrentUserProvider.CurrentUser user = currentUserProvider.find();
        return user == null ? null : user.id();
    }



    @GetMapping("/history")
    @Operation(summary = "获取历史记录", description = "获取当前用户的问答历史记录")
    public Result<List<QaResponse>> getHistory() {
        Long userId = currentUserProvider.require().id();
        log.info("获取问答历史，userId: {}", userId);
        try {
            List<QaResponse> histories = qaService.getHistory(userId);
            log.info("获取到的历史记录数量: {}", histories.size());
            return Result.success(histories);
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage(), e);
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/sessions")
    @Operation(summary = "获取历史会话列表（按sessionId聚合）", description = "获取用户的历史会话，同一sessionId的消息会被聚合到一起")
    public Result<List<QaSessionDTO>> getHistorySessions() {
        Long userId = currentUserProvider.require().id();
        log.info("获取历史会话，userId: {}", userId);
        try {
            List<QaSessionDTO> sessions = qaService.getHistorySessions(userId);
            log.info("获取到的会话数量: {}", sessions.size());
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取历史会话失败: {}", e.getMessage(), e);
            return Result.error("获取历史会话失败: " + e.getMessage());
        }
    }

    @PostMapping("/history")
    @Operation(summary = "保存对话历史", description = "前端主动保存对话历史到数据库")
    public Result<Void> saveHistory(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String answer = (String) request.get("answer");
            String subject = (String) request.get("subject");

            if (question == null || answer == null) {
                return Result.error("问题和回答不能为空");
            }

            // 安全基线：userId 只认当前登录用户，忽略请求体中可能伪造的 userId 字段
            Long userId = currentUserProvider.require().id();

            // 获取可选参数
            Long bookId = null;
            Long knowledgeNodeId = null;
            Object bookIdObj = request.get("bookId");
            Object knowledgeNodeIdObj = request.get("knowledgeNodeId");

            if (bookIdObj != null) {
                if (bookIdObj instanceof Long) {
                    bookId = (Long) bookIdObj;
                } else if (bookIdObj instanceof Integer) {
                    bookId = ((Integer) bookIdObj).longValue();
                } else if (bookIdObj instanceof String) {
                    try {
                        bookId = Long.parseLong((String) bookIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析bookId: {}", bookIdObj);
                    }
                }
            }

            if (knowledgeNodeIdObj != null) {
                if (knowledgeNodeIdObj instanceof Long) {
                    knowledgeNodeId = (Long) knowledgeNodeIdObj;
                } else if (knowledgeNodeIdObj instanceof Integer) {
                    knowledgeNodeId = ((Integer) knowledgeNodeIdObj).longValue();
                } else if (knowledgeNodeIdObj instanceof String) {
                    try {
                        knowledgeNodeId = Long.parseLong((String) knowledgeNodeIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析knowledgeNodeId: {}", knowledgeNodeIdObj);
                    }
                }
            }

            // 构建上下文信息
            String context = null;
            if (subject != null) {
                context = "学科：" + subject;
            }

            // 获取会话ID
            String sessionId = (String) request.get("sessionId");

            qaService.saveHistory(userId, sessionId, question, answer, context, bookId, knowledgeNodeId, "NORMAL");
            log.info("对话历史保存成功，用户ID: {}, 会话ID: {}, 问题: {}", userId, sessionId, question);

            return Result.success();
        } catch (Exception e) {
            log.error("保存对话历史失败: {}", e.getMessage(), e);
            return Result.error("保存对话历史失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除历史记录", description = "删除指定的问答历史记录")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        Long userId = currentUserProvider.require().id();
        qaService.deleteHistory(id, userId);
        return Result.success();
    }
    
    /**
     * 流式答疑接口 - 实时返回AI回答
     * 
     * 功能说明：
     * 1. 接收用户问题，通过AI实时生成回答
     * 2. 采用SSE（Server-Sent Events）技术实现流式输出
     * 3. AI回答完成后，自动保存到答疑历史记录
     * 
     * 请求参数（QaRequest）：
     * - question: 用户问题
     * - subject: 科目/学科
     * - bookId: 关联的教材ID（可选）
     * - knowledgeNodeId: 关联的知识点ID（可选）
     * - userId: 用户ID（必需，用于保存历史记录）
     * 
     * 返回格式：SSE流
     * - data: 开头的行表示AI回答内容片段
     * - event: complete 表示流式响应完成
     * - event: error 表示发生错误
     * 
     * @param request 答疑请求对象
     * @return Flux<SseMessage> SSE流式响应
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提问", description = "向AI提问并获取流式回答")
    public Flux<SseMessage> askQuestionStream(@RequestBody QaRequest request) {
        // 步骤1：构建系统提示词，包含上下文信息
        String context = buildSystemPrompt(request);
        
        // 步骤1.5：获取用户ID（优先从请求参数获取，否则从SecurityContext获取）
        // 安全基线：忽略外部传入的 userId，只认当前登录用户（游客为 null，仅不落历史）
        Long userId = currentUserIdOrNull();
        // 步骤2：创建StringBuilder用于收集完整的AI回答
        // 注意：流式响应会将回答分成多个片段发送，这里需要收集完整内容用于保存历史
        final Long finalUserId = userId;
        StringBuilder fullResponse = new StringBuilder();
        
        // 步骤3：使用Flux.defer实现惰性订阅，确保每次请求都创建新的流
        // Sinks.many().unicast().onBackpressureBuffer() 创建单播接收器，支持背压
        return Flux.defer(() -> {
            // 创建SSE消息的接收器，用于向客户端发送消息
            Sinks.Many<SseMessage> sink = Sinks.many().unicast().onBackpressureBuffer();
            
            // 步骤4：订阅AI流式服务，处理返回的每个消息片段
            // 验证或创建会话ID
            String currentSessionId = request.getSessionId();
            boolean isNewConversation = false;
            
            if (currentSessionId != null && !currentSessionId.isEmpty()) {
                // 验证会话ID是否有效
                if (!sessionService.validateSession(currentSessionId)) {
                    log.warn("无效或过期的会话ID，创建新会话: {}", currentSessionId);
                    currentSessionId = sessionService.createSession();
                    // 如果会话失效，清空旧上下文
                    contextService.clearContext(currentSessionId);
                    isNewConversation = true;
                } else {
                    log.debug("使用现有会话: {}", currentSessionId);
                }
            } else {
                // 创建新会话
                currentSessionId = sessionService.createSession();
                log.debug("创建新会话: {}", currentSessionId);
                isNewConversation = true;
            }
            final String finalSessionId = currentSessionId;
            final boolean finalIsNewConversation = isNewConversation;
            
            // 生成消息ID
            final String userMessageId = "msg_" + System.currentTimeMillis() + "_user";
            final String assistantMessageId = "msg_" + System.currentTimeMillis() + "_assistant";
            
            // 步骤4.5：如果是新会话且用户已登录，创建数据库会话记录
            if (finalIsNewConversation && finalUserId != null) {
                conversationService.createConversation(finalUserId, request.getQuestion());
            }
            
            // 步骤4.6：保存用户消息到上下文
            contextService.saveMessage(finalSessionId, userMessageId, "user", request.getQuestion());
            
            // 步骤4.7：如果用户已登录，保存用户消息到数据库
            if (finalUserId != null) {
                messageService.saveMessage(finalSessionId, userMessageId, "user", request.getQuestion(), 0, "SUCCESS");
            }
            
            // 步骤4.8：构建完整的上下文（包含历史对话）
            String fullContext = contextService.getContext(finalSessionId, request.getQuestion());
            log.debug("构建的上下文长度: {} 字符", fullContext.length());
            
            // 标记是否是第一个消息
            final boolean[] isFirstMessage = {true};
            
            aiService.generateStreamResponseWithContext(fullContext, request.getQuestion(), currentSessionId).subscribe(
                // onNext: 处理每个流式消息片段
                message -> {
                    // 如果是数据消息（AI回答内容），则收集到fullResponse中
                    if (message.isData()) {
                        // 第一个数据消息需要包含sessionId，让前端知道当前会话ID
                        if (isFirstMessage[0]) {
                            message.setSessionId(finalSessionId);
                            isFirstMessage[0] = false;
                        }
                        fullResponse.append(message.getContent());
                    }
                    // 将消息转发给客户端
                    sink.tryEmitNext(message);
                },
                // onError: 处理流式响应过程中的错误
                error -> {
                    log.error("流式响应错误：{}", error.getMessage(), error);
                    sink.tryEmitError(error);
                },
                // onComplete: 流式响应完成时的回调
                () -> {
                    // 步骤5：流结束时，保存答疑历史记录和上下文
                    try {
                        // 保存AI回复到上下文
                        if (!fullResponse.isEmpty()) {
                            contextService.saveMessage(finalSessionId, assistantMessageId, "assistant", fullResponse.toString());
                            log.debug("已保存AI回复到上下文，会话ID: {}", finalSessionId);
                        }
                        
                        // 如果用户已登录，保存助手消息到数据库并更新会话
                        if (finalUserId != null && !fullResponse.isEmpty()) {
                            // 保存助手消息到数据库
                            messageService.saveMessage(finalSessionId, assistantMessageId, "assistant", 
                                fullResponse.toString(), 0, "SUCCESS");
                            
                            // 更新会话信息（最后消息预览、消息数量、更新时间）
                            conversationService.updateConversation(finalSessionId, fullResponse.toString());
                            
                            // 调用服务层保存历史记录
                            qaService.saveHistory(
                                finalUserId,                  // 用户ID
                                finalSessionId,               // 会话ID
                                request.getQuestion(),        // 用户问题
                                fullResponse.toString(),     // AI完整回答
                                context,                      // 上下文信息
                                request.getBookId(),          // 关联教材ID
                                request.getKnowledgeNodeId(), // 关联知识点ID
                                null                          // 问题类型（默认）
                            );
                            log.info("已保存答疑历史，用户ID: {}, 会话ID: {}, 问题: {}", 
                                finalUserId, finalSessionId, request.getQuestion());
                        } else {
                            // 用户未登录或回答为空，不保存历史
                            log.warn("无法保存答疑历史：userId={}, 回答长度={}", 
                                finalUserId, fullResponse.length());
                        }
                    } catch (Exception e) {
                        // 记录保存失败日志，但不中断流程
                        // 因为流式响应已经完成，主要功能不受影响
                        log.error("保存答疑历史失败：{}", e.getMessage(), e);
                    }
                    // 通知客户端流已结束，包含sessionId
                    sink.tryEmitNext(SseMessage.complete(finalSessionId));
                }
            );
            
            // 返回转换后的Flux流
            return sink.asFlux();
        });
    }
    

    
    @PostMapping("/ask")
    @Operation(summary = "非流式提问", description = "向AI提问并获取完整回答")
    public CompletableFuture<Result<QaResultDTO>> askQuestion(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        // 如果请求没有提供sessionId，就创建一个新的
        String sessionId = request.getSessionId() != null ? request.getSessionId() : aiService.createSession();

        // 身份必须在切到异步线程之前取好：SecurityContextHolder 与 RequestContextHolder
        // 都是 ThreadLocal，换到 ai-request- 线程上就取不到了
        Long userId = currentUserIdOrNull();

        try {
            // 非流式提问必然要等 AI 出完整答案，但不该由 Tomcat 请求线程去等：
            // 交给 MVC 异步，容器线程立刻归还，等待发生在专用的有界池上。
            return CompletableFuture.supplyAsync(() -> {
                String response = aiService.generateResponseWithContext(context, request.getQuestion(), sessionId);

                // 保存历史记录
                // 安全基线：只认当前登录用户（游客为 null，仅不落历史）
                if (userId != null && response != null && !response.isEmpty()) {
                    qaService.saveHistory(userId, sessionId, request.getQuestion(), response, context,
                            request.getBookId(), request.getKnowledgeNodeId(), null);
                    log.info("已保存非流式问答历史，用户ID: {}, 会话ID: {}", userId, sessionId);
                }

                return Result.success(new QaResultDTO(response, sessionId));
            }, aiRequestExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("AI 请求线程池已满，拒绝本次提问, userId: {}", userId);
            return CompletableFuture.completedFuture(Result.error(503, "AI 服务繁忙，请稍后重试"));
        }
    }
    
    @PostMapping("/save-processed-text")
    @Operation(summary = "保存处理后的文本", description = "保存前端处理后的文本数据到历史记录")
    public Result<Void> saveProcessedText(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String processedText = (String) request.get("processedText");
            String subject = (String) request.get("subject");
            
            if (question == null || processedText == null) {
                return Result.error("问题和处理后的文本不能为空");
            }
            
            // 安全基线：userId 只认当前登录用户，忽略请求体中可能伪造的 userId 字段
            Long userId = currentUserProvider.require().id();
            
            String sessionId = (String) request.get("sessionId");
            qaService.saveProcessedText(question, processedText, subject, userId, sessionId);
            log.info("处理后的文本保存成功，用户ID: {}, 会话ID: {}", userId, sessionId);
            
            return Result.success();
        } catch (Exception e) {
            log.error("保存处理后的文本失败: {}", e.getMessage(), e);
            return Result.error("保存处理后的文本失败: " + e.getMessage());
        }
    }
    
    private String buildSystemPrompt(QaRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的学科问答助手，请根据用户的问题提供详细、准确的回答。\n\n");
        prompt.append("【重要】请使用纯文本格式回答，遵循以下规则：\n");
        prompt.append("1. 不要使用Markdown语法（如**加粗**、*斜体*、## 标题等）\n");
        prompt.append("2. 列表请使用阿拉伯数字加点（如1. 第一点，2. 第二点）\n");
        prompt.append("3. 数学公式必须使用LaTeX格式：\n");
        prompt.append("   - 行内公式用 \\( ... \\) 包裹，如 \\(x^2 + y^2 = z^2\\)\n");
        prompt.append("   - 独立公式用 \\[ ... \\] 包裹，如 \\[\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1\\]\n");
        prompt.append("4. 换行用普通换行符即可\n");
        prompt.append("5. 代码块使用普通缩进，不要用```包裹\n");
        
        if (request.getSubject() != null) {
            prompt.append("\n学科：").append(request.getSubject());
        }
        
        return prompt.toString();
    }
    
    @GetMapping("/courses/recommend")
    @Operation(summary = "获取推荐课程", description = "基于用户提问历史推荐网课")
    public CompletableFuture<Result<List<Map<String, Object>>>> getRecommendedCourses(
        @RequestParam(defaultValue = "5") Integer limit) {
        try {
            Long userId = currentUserProvider.require().id();
            log.info("获取推荐课程，用户ID: {}, 限制: {}", userId, limit);
            
            return courseRecommendService.recommendCoursesByQuestionHistoryAsync(userId, limit)
                .thenApply(Result::success)
                .exceptionally(ex -> {
                    log.error("获取推荐课程失败: {}", ex.getMessage(), ex);
                    return Result.error("获取推荐课程失败: " + ex.getMessage());
                });
        } catch (Exception e) {
            log.error("获取推荐课程失败: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(Result.error("获取推荐课程失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/courses/recommend/exam")
    @Operation(summary = "获取考试准备推荐课程", description = "基于用户考试准备Q&A历史推荐网课")
    public CompletableFuture<Result<List<Map<String, Object>>>> getExamRecommendedCourses(
        @RequestParam(defaultValue = "5") Integer limit) {
        try {
            Long userId = currentUserProvider.require().id();
            log.info("获取考试准备推荐课程，用户ID: {}, 限制: {}", userId, limit);
            
            return courseRecommendService.recommendCoursesByExamQaHistoryAsync(userId, limit)
                .thenApply(Result::success)
                .exceptionally(ex -> {
                    log.error("获取考试准备推荐课程失败: {}", ex.getMessage(), ex);
                    return Result.error("获取考试准备推荐课程失败: " + ex.getMessage());
                });
        } catch (Exception e) {
            log.error("获取考试准备推荐课程失败: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(Result.error("获取考试准备推荐课程失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/ask/image")
    @Operation(summary = "图片问答", description = "上传图片并向AI提问")
    public CompletableFuture<Result<String>> askImageQuestion(
            @RequestParam("image") MultipartFile image, 
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        try {
            log.info("处理图片问答请求，图片大小: {}KB", image.getSize() / 1024);
            
            // 验证图片大小
            if (image.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return CompletableFuture.completedFuture(Result.error("图片大小不能超过10MB"));
            }
            
            // 验证图片类型
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return CompletableFuture.completedFuture(Result.error("请上传有效的图片文件"));
            }
            
            // 对问题进行sanitize处理，防止XSS攻击
            String sanitizedQuestion = question != null ? sanitizeInput(question) : null;

            // 字节必须在请求线程上读完：MultipartFile 绑定原始请求的生命周期，
            // 到异步线程再 getBytes() 时临时文件可能已被清理。
            byte[] imageBytes = image.getBytes();

            // 身份同样要在切线程前取（ThreadLocal）
            Long userId = currentUserIdOrNull();

            return CompletableFuture.supplyAsync(() -> {
                String response = aiService.generateResponseFromImage(imageBytes, contentType, sanitizedQuestion);

                // 保存历史记录
                // 安全基线：只认当前登录用户（游客为 null，仅不落历史）
                if (userId != null && response != null && !response.isEmpty()) {
                    qaService.saveHistory(userId, sessionId,
                            sanitizedQuestion != null ? sanitizedQuestion : "[图片提问]", response, null, null, null, null);
                    log.info("已保存图片问答历史，用户ID: {}, 会话ID: {}", userId, sessionId);
                }

                return Result.success(response);
            }, aiRequestExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("AI 请求线程池已满，拒绝本次图片提问");
            return CompletableFuture.completedFuture(Result.error(503, "AI 服务繁忙，请稍后重试"));
        } catch (Exception e) {
            log.error("处理图片问答失败: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(Result.error("处理图片问答失败: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = "/ask/image/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式图片问答", description = "上传图片并向AI提问，获取流式回答")
    public Flux<SseMessage> askImageQuestionStream(
            @RequestParam("image") MultipartFile image, 
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        log.info("处理流式图片问答请求，图片大小: {}KB", image.getSize() / 1024);
        
        // 验证图片大小
        if (image.getSize() > 10 * 1024 * 1024) { // 10MB限制
            return Flux.just(SseMessage.error("图片大小不能超过10MB"));
        }
        
        // 验证图片类型
        String contentType = image.getContentType();
        if (!contentType.startsWith("image/")) {
            return Flux.just(SseMessage.error("请上传有效的图片文件"));
        }
        
        // 对问题进行sanitize处理，防止XSS攻击
        String sanitizedQuestion = question != null ? sanitizeInput(question) : null;
        
        // 获取用户ID
        // 安全基线：忽略外部传入的 userId，只认当前登录用户（游客为 null，仅不落历史）
        Long userId = currentUserIdOrNull();
        final Long finalUserId = userId;
        final String finalQuestion = sanitizedQuestion != null ? sanitizedQuestion : "[图片提问]";
        final String finalSessionId = sessionId;
        
        // 调用AI服务处理图片，返回流式响应
        Flux<SseMessage> response = aiService.generateStreamResponseFromImage(image, sanitizedQuestion);
        
        // 如果有用户ID，收集完整回答并保存
        if (finalUserId != null) {
            StringBuilder fullResponse = new StringBuilder();
            return response.doOnNext(msg -> {
                if (msg.isData()) {
                    fullResponse.append(msg.getData());
                }
            }).doOnComplete(() -> {
                if (fullResponse.length() > 0) {
                    try {
                        qaService.saveHistory(finalUserId, finalSessionId, finalQuestion, fullResponse.toString(), null, null, null, null);
                        log.info("已保存流式图片问答历史，用户ID: {}, 会话ID: {}", finalUserId, finalSessionId);
                    } catch (Exception e) {
                        log.error("保存流式图片问答历史失败: {}", e.getMessage(), e);
                    }
                }
            });
        }
        
        return response;
    }
    
    @GetMapping("/courses/recommendations")
    @Operation(summary = "获取课程推荐", description = "基于QA历史数据生成课程推荐JSON数据")
    public Result<Map<String, Object>> getCourseRecommendations() {
        try {
            Map<String, Object> recommendations = qaService.getCourseRecommendations();
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取课程推荐失败: {}", e.getMessage(), e);
            return Result.error("获取课程推荐失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/exam/qa/history")
    @Operation(summary = "获取考试准备Q&A历史记录", description = "获取当前用户的考试准备Q&A历史记录")
    public Result<List<Map<String, Object>>> getExamQaHistory() {
        Long userId = currentUserProvider.require().id();
        log.info("获取考试准备Q&A历史，userId: {}", userId);
        try {
            List<Map<String, Object>> histories = qaService.getExamQaHistory(userId);
            log.info("获取到的考试准备Q&A历史记录数量: {}", histories.size());
            return Result.success(histories);
        } catch (Exception e) {
            log.error("获取考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            return Result.error("获取考试准备Q&A历史记录失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/exam/qa/history/{id}")
    @Operation(summary = "删除考试准备Q&A历史记录", description = "删除指定的考试准备Q&A历史记录")
    public Result<Void> deleteExamQaHistory(@PathVariable Long id) {
        Long userId = currentUserProvider.require().id();
        qaService.deleteExamQaHistory(id, userId);
        return Result.success();
    }
    
    @PostMapping("/exam/qa/save")
    @Operation(summary = "保存考试准备Q&A历史记录", description = "保存考试准备Q&A历史记录到数据库")
    public Result<Void> saveExamQaHistory(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String answer = (String) request.get("answer");
            String subject = (String) request.get("subject");
            
            if (question == null || answer == null) {
                return Result.error("问题和回答不能为空");
            }
            
            // 安全基线：userId 只认当前登录用户，忽略请求体中可能伪造的 userId 字段
            Long userId = currentUserProvider.require().id();
            
            String sessionId = (String) request.get("sessionId");
            qaService.saveExamQaHistory(question, answer, subject, userId, sessionId);
            log.info("考试准备Q&A历史记录保存成功，用户ID: {}, 会话ID: {}", userId, sessionId);
            
            return Result.success();
        } catch (Exception e) {
            log.error("保存考试准备Q&A历史记录失败: {}", e.getMessage(), e);
            return Result.error("保存考试准备Q&A历史记录失败: " + e.getMessage());
        }
    }
    
    // 输入sanitization方法，防止XSS攻击
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;");
    }
}
