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