package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.QaRequest;
import com.student.platform.dto.QaResponse;
import com.student.platform.dto.Result;
import com.student.platform.dto.SseMessage;

import com.student.platform.service.AiService;
import com.student.platform.service.CourseRecommendService;
import com.student.platform.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import com.student.platform.config.UserIdInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI问答", description = "AI问答相关接口")
public class QaController {

    private final QaService qaService;
    private final AiService aiService;
    private final CourseRecommendService courseRecommendService;
    private final com.student.platform.mapper.UserMapper userMapper;



    @GetMapping("/history")
    @Operation(summary = "获取历史记录", description = "获取当前用户的问答历史记录")
    public Result<List<QaResponse>> getHistory(@RequestParam("userId") Long userId) {
        log.info("从请求参数获取到的userId: {}", userId);
        try {
            List<QaResponse> histories = qaService.getHistory(userId);
            log.info("获取到的历史记录数量: {}", histories.size());
            return Result.success(histories);
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage(), e);
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除历史记录", description = "删除指定的问答历史记录")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        qaService.deleteHistory(id);
        return Result.success();
    }
    
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提问", description = "向AI提问并获取流式回答")
    public Flux<SseMessage> askQuestionStream(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        return aiService.generateStreamResponseWithContext(context, request.getQuestion());
    }
    

    
    @PostMapping("/ask")
    @Operation(summary = "非流式提问", description = "向AI提问并获取完整回答")
    public Result<String> askQuestion(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        String response = aiService.generateResponseWithContext(context, request.getQuestion());
        return Result.success(response);
    }
    
    @PostMapping("/save-processed-text")
    @Operation(summary = "保存处理后的文本", description = "保存前端处理后的文本数据到历史记录")
    public Result<Void> saveProcessedText(HttpServletRequest httpRequest, @RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String processedText = (String) request.get("processedText");
            String subject = (String) request.get("subject");
            
            if (question == null || processedText == null) {
                return Result.error("问题和处理后的文本不能为空");
            }
            
            Long userId = null;
            
            // 首先从请求体中获取userId
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析userId: {}", userIdObj);
                    }
                }
            }
            
            // 从UserIdInterceptor获取userId
            if (userId == null) {
                userId = (Long) httpRequest.getAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE);
            }
            
            // 从认证上下文获取userId
            if (userId == null) {
               Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                    // 这里需要从userDetails中获取userId，但是UserDetails没有userId属性
                    // 所以我们需要从数据库中根据用户名查询用户
                    try {
                        // 直接注入UserMapper而不是使用SpringContextUtil
                        com.student.platform.entity.User user = userMapper.findByUsername(userDetails.getUsername());
                        if (user != null) {
                            userId = user.getId();
                        }
                    } catch (Exception e) {
                        log.error("从认证上下文获取用户信息失败: {}", e.getMessage());
                    }
                }
            }
            
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            qaService.saveProcessedText(question, processedText, subject, userId);
            log.info("处理后的文本保存成功，用户ID: {}", userId);
            
            return Result.success();
        } catch (Exception e) {
            log.error("保存处理后的文本失败: {}", e.getMessage(), e);
            return Result.error("保存处理后的文本失败: " + e.getMessage());
        }
    }
    
    private String buildSystemPrompt(QaRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的学科问答助手，请根据用户的问题提供详细、准确的回答。");
        
        if (request.getSubject() != null) {
            prompt.append("\n学科：").append(request.getSubject());
        }
        
        return prompt.toString();
    }
    
    @GetMapping("/exam/subjects")
    @Operation(summary = "获取考试科目", description = "获取所有可用的考试科目")
    public Result<List<Map<String, Object>>> getExamSubjects(@RequestParam("userId") Long userId) {
        log.info("获取考试科目，用户ID: {}", userId);
        List<Map<String, Object>> subjects = qaService.getExamSubjects();
        return Result.success(subjects);
    }
    
    @GetMapping("/exam/subjects/{subjectId}/key-points")
    @Operation(summary = "获取知识点", description = "获取指定科目的知识点")
    public Result<List<Map<String, Object>>> getKeyPoints(@PathVariable String subjectId) {
        List<Map<String, Object>> keyPoints = qaService.getKeyPoints(subjectId);
        return Result.success(keyPoints);
    }

    @GetMapping("/exam/subjects/{subjectId}/papers")
    @Operation(summary = "获取真题试卷", description = "获取指定科目的真题试卷列表")
    public Result<List<Map<String, Object>>> getExamPapers(@PathVariable String subjectId) {
        List<Map<String, Object>> papers = qaService.getExamPapers(subjectId);
        return Result.success(papers);
    }
    
    @GetMapping("/exam/subjects/{subjectId}/papers/ai-generate")
    @Operation(summary = "AI生成真题推荐", description = "使用AI为指定科目生成真题推荐")
    public Result<List<Map<String, Object>>> generateExamPapersByAI(@PathVariable String subjectId) {
        try {
            List<Map<String, Object>> papers = qaService.generateExamPapersByAI(subjectId);
            return Result.success(papers);
        } catch (Exception e) {
            log.error("AI生成真题推荐失败: {}", e.getMessage(), e);
            return Result.error("AI生成真题推荐失败: " + e.getMessage());
        }
    }

    @GetMapping("/exam/papers/{paperId}/questions")
    @Operation(summary = "获取试卷题目", description = "获取指定试卷的练习题目")
    public Result<List<Map<String, Object>>> getExamPaperQuestions(@PathVariable String paperId) {
        try {
            List<Map<String, Object>> questions = qaService.getExamPaperQuestions(paperId);
            return Result.success(questions);
        } catch (Exception e) {
            log.error("获取试卷题目失败: {}", e.getMessage(), e);
            return Result.error("获取试卷题目失败: " + e.getMessage());
        }
    }

    @GetMapping("/exam/history")
    @Operation(summary = "获取考试历史", description = "获取当前用户的考试历史")
    public Result<List<Map<String, Object>>> getExamHistory() {
        List<Map<String, Object>> examHistory = qaService.getExamHistory();
        return Result.success(examHistory);
    }
    
    @GetMapping("/books")
    @Operation(summary = "获取书籍列表", description = "获取所有可用的书籍")
    public Result<List<Map<String, Object>>> getBooks(@RequestParam("userId") Long userId) {
        log.info("获取书籍列表，用户ID: {}", userId);
        List<Map<String, Object>> books = qaService.getBooks();
        return Result.success(books);
    }

    @GetMapping("/books/{bookId}/chapters")
    @Operation(summary = "获取书籍章节", description = "获取指定书籍的章节列表")
    public Result<List<Map<String, Object>>> getChapters(@PathVariable Long bookId) {
        try {
            log.info("获取书籍章节，书籍ID: {}", bookId);
            List<Map<String, Object>> chapters = qaService.getChapters(bookId);
            return Result.success(chapters);
        } catch (Exception e) {
            log.error("获取章节列表失败: {}", e.getMessage(), e);
            return Result.error("获取章节列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/courses")
    @Operation(summary = "获取课程列表", description = "获取所有可用的课程")
    public Result<List<Map<String, Object>>> getCourses() {
        List<Map<String, Object>> courses = qaService.getCourses();
        return Result.success(courses);
    }
    
    @GetMapping("/courses/recommend")
    @Operation(summary = "获取推荐课程", description = "基于用户提问历史推荐网课")
    public CompletableFuture<Result<List<Map<String, Object>>>> getRecommendedCourses(
        @RequestParam("userId") Long userId,
        @RequestParam(defaultValue = "5") Integer limit) {
        try {
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
        @RequestParam("userId") Long userId,
        @RequestParam(defaultValue = "5") Integer limit) {
        try {
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
    public Result<String> askImageQuestion(@RequestParam("image") MultipartFile image, @RequestParam(value = "question", required = false) String question) {
        try {
            log.info("处理图片问答请求，图片大小: {}KB", image.getSize() / 1024);
            
            // 验证图片大小
            if (image.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return Result.error("图片大小不能超过10MB");
            }
            
            // 验证图片类型
            String contentType = image.getContentType();
            if (!contentType.startsWith("image/")) {
                return Result.error("请上传有效的图片文件");
            }
            
            // 对问题进行sanitize处理，防止XSS攻击
            String sanitizedQuestion = question != null ? sanitizeInput(question) : null;
            
            // 调用AI服务处理图片
            String response = aiService.generateResponseFromImage(image, sanitizedQuestion);
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("处理图片问答失败: {}", e.getMessage(), e);
            return Result.error("处理图片问答失败: " + e.getMessage());
        }
    }
    
    @PostMapping(value = "/ask/image/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式图片问答", description = "上传图片并向AI提问，获取流式回答")
    public Flux<SseMessage> askImageQuestionStream(@RequestParam("image") MultipartFile image, @RequestParam(value = "question", required = false) String question) {
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
        
        // 调用AI服务处理图片，返回流式响应
        return aiService.generateStreamResponseFromImage(image, sanitizedQuestion);
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
    
    @GetMapping("/exam/subjects/{subjectId}/courses")
    @Operation(summary = "获取指定科目的课程", description = "获取指定科目的相关课程列表")
    public Result<List<Map<String, Object>>> getExamCourses(@PathVariable String subjectId) {
        try {
            List<Map<String, Object>> courses = qaService.getExamCourses(subjectId);
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取科目课程失败: {}", e.getMessage(), e);
            return Result.error("获取科目课程失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/exam/qa/history")
    @Operation(summary = "获取考试准备Q&A历史记录", description = "获取当前用户的考试准备Q&A历史记录")
    public Result<List<Map<String, Object>>> getExamQaHistory(@RequestParam("userId") Long userId) {
        log.info("从请求参数获取到的userId: {}", userId);
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
        qaService.deleteExamQaHistory(id);
        return Result.success();
    }
    
    @PostMapping("/exam/qa/save")
    @Operation(summary = "保存考试准备Q&A历史记录", description = "保存考试准备Q&A历史记录到数据库")
    public Result<Void> saveExamQaHistory(HttpServletRequest httpRequest, @RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String answer = (String) request.get("answer");
            String subject = (String) request.get("subject");
            
            if (question == null || answer == null) {
                return Result.error("问题和回答不能为空");
            }
            
            Long userId = null;
            
            // 首先从请求体中获取userId
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析userId: {}", userIdObj);
                    }
                }
            }
            
            // 从UserIdInterceptor获取userId
            if (userId == null) {
                userId = (Long) httpRequest.getAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE);
            }
            
            // 从认证上下文获取userId
            if (userId == null) {
               Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                    // 这里需要从userDetails中获取userId，但是UserDetails没有userId属性
                    // 所以我们需要从数据库中根据用户名查询用户
                    try {
                        // 直接注入UserMapper而不是使用SpringContextUtil
                        com.student.platform.entity.User user = userMapper.findByUsername(userDetails.getUsername());
                        if (user != null) {
                            userId = user.getId();
                        }
                    } catch (Exception e) {
                        log.error("从认证上下文获取用户信息失败: {}", e.getMessage());
                    }
                }
            }
            
            if (userId == null) {
                return Result.error("用户未登录");
            }
            
            qaService.saveExamQaHistory(question, answer, subject, userId);
            log.info("考试准备Q&A历史记录保存成功，用户ID: {}", userId);
            
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
