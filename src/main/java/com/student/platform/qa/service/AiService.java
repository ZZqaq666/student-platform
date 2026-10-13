package com.student.platform.qa.service;

import com.student.platform.common.dto.BookRecommendationDTO;
import com.student.platform.common.dto.KnowledgeGraphDTO;
import com.student.platform.common.dto.SseMessage;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * AI服务接口
 *
 * 功能说明：
 * - 提供AI问答能力，支持上下文理解
 * - 支持流式响应和非流式响应两种模式
 * - 支持图片理解和分析
 * - 提供书籍推荐和知识图谱生成功能
 *
 * 核心能力：
 * - 文本问答：基于上下文的智能问答
 * - 图片问答：分析图片内容并回答问题
 * - 书籍推荐：根据书籍ID推荐相关书籍
 * - 知识图谱：生成书籍的知识图谱
 * - 习题生成：根据知识点和难度生成练习题
 *
 * @author student-platform
 * @since 1.0.0
 */
public interface AiService {
    
    /**
     * 创建新会话
     *
     * @return 会话ID
     */
    String createSession();
    
    /**
     * 生成基于上下文的响应（非流式）
     *
     * @param context 上下文信息
     * @param question 用户问题
     * @return AI回答内容
     */
    String generateResponseWithContext(String context, String question);

    /**
     * 生成基于上下文的响应（非流式，带会话ID）
     *
     * @param context   上下文信息
     * @param question  用户问题
     * @param sessionId 会话ID
     * @return AI回答内容
     */
    String generateResponseWithContext(String context, String question, String sessionId);

    /**
     * 带用户归属的版本：AI 回答缓存键必须含 userId，否则不同用户会互相命中对方的回答。
     * userId 传 null 表示无用户上下文，此时读写缓存一并跳过。
     *
     * 注意：身份必须在进入异步线程前由调用方取出并传进来，SecurityContext 与
     * RequestContextHolder 都是 ThreadLocal，到工作线程上已经取不到。
     */
    String generateResponseWithContext(String context, String question, String sessionId, Long userId);
    
    /**
     * 生成基于上下文的流式响应
     *
     * @param context 上下文信息
     * @param question 用户问题
     * @return Flux流，包含SSE消息
     */
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question);
    
    /**
     * 生成基于上下文的流式响应（带会话ID）
     *
     * @param context     上下文信息
     * @param question    用户问题
     * @param sessionId   会话ID
     * @return Flux流，包含SSE消息
     */
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question, String sessionId);

    /**
     * 带用户归属的流式版本，缓存键含 userId；userId 为 null 时不缓存。
     */
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question, String sessionId, Long userId);
    
    /**
     * 生成基于上下文的流式响应（带完成回调）
     *
     * @param context     上下文信息
     * @param question    用户问题
     * @param onComplete  完成回调，接收完整的AI响应文本
     * @return Flux流，包含SSE消息
     */
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question, 
        java.util.function.Consumer<String> onComplete);
    
    /**
     * 根据图片生成响应（非流式）
     *
     * @param image    图片文件
     * @param question 用户问题
     * @return AI回答内容
     */
    String generateResponseFromImage(MultipartFile image, String question);

    /**
     * 字节形式重载：调用方（控制器）在请求线程上读完 MultipartFile 后使用，
     * 这样等待模型返回可以安全地放到异步线程池上。
     */
    String generateResponseFromImage(byte[] imageBytes, String contentType, String question);
    
    /**
     * 根据图片生成流式响应
     *
     * @param image    图片文件
     * @param question 用户问题
     * @return Flux流，包含SSE消息
     */
    Flux<SseMessage> generateStreamResponseFromImage(MultipartFile image, String question);
    
    /**
     * 获取书籍推荐
     *
     * @param bookId 书籍ID
     * @return 书籍推荐DTO
     */
    BookRecommendationDTO getBookRecommendations(Long bookId);
    
    /**
     * 生成知识图谱
     *
     * @param bookId 书籍ID
     * @return 知识图谱DTO
     */
    KnowledgeGraphDTO generateKnowledgeGraph(Long bookId);
    
    /**
     * 根据知识点生成练习题
     *
     * @param knowledgePoint 知识点名称
     * @param difficulty     难度等级
     * @return 练习题JSON字符串
     */
    String generateExercises(String knowledgePoint, String difficulty);
}