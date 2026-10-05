package com.student.platform.qa.service;

import com.student.platform.common.dto.QaResponse;
import com.student.platform.common.dto.QaSessionDTO;

import java.util.List;
import java.util.Map;

/**
 * 答疑服务接口
 * 
 * 提供问答系统的核心业务逻辑，包括：
 * - 答疑历史记录管理
 * - 考试相关问答
 * - 教材和课程信息查询
 * - AI辅助功能
 */
public interface QaService {
    
    /**
     * 获取用户答疑历史记录
     * 
     * @param userId 用户ID
     * @return 答疑历史列表
     */
    List<QaResponse> getHistory(Long userId);
    
    /**
     * 获取用户的答疑历史会话（按sessionId聚合）
     * 
     * @param userId 用户ID
     * @return 会话列表，每个会话包含多条消息
     */
    List<QaSessionDTO> getHistorySessions(Long userId);
    
    /**
     * 删除指定的历史记录
     * 
     * @param id 历史记录ID
     */
    void deleteHistory(Long id);
    
    /**
     * 保存处理后的文本数据到历史记录
     * 用于保存用户上传文本的处理结果
     * 
     * @param question 用户原始问题
     * @param processedText 处理后的文本
     * @param subject 科目
     * @param userId 用户ID
     */
    void saveProcessedText(String question, String processedText, String subject, Long userId);

    /**
     * 保存处理后的文本数据到历史记录（带会话ID）
     * 
     * @param question 用户原始问题
     * @param processedText 处理后的文本
     * @param subject 科目
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void saveProcessedText(String question, String processedText, String subject, Long userId, String sessionId);
    
    /**
     * 保存答疑历史记录
     * 
     * 功能说明：
     * 1. 在流式答疑完成后调用，保存用户的提问和AI的回答
     * 2. 对输入内容进行XSS防护处理，防止恶意脚本注入
     * 3. 自动记录创建时间
     * 
     * 保存的数据字段：
     * - userId: 用户ID（必需）
     * - question: 用户问题（必需）
     * - answer: AI回答（必需）
     * - context: 上下文信息（如科目、教材等）
     * - bookId: 关联的教材ID（可选）
     * - knowledgeNodeId: 关联的知识点ID（可选）
     * - questionType: 问题类型（如NORMAL、PROCESSED等）
     * 
     * @param userId 用户ID
     * @param question 用户问题
     * @param answer AI回答
     * @param context 上下文信息
     * @param bookId 关联教材ID（可选）
     * @param knowledgeNodeId 关联知识点ID（可选）
     * @param questionType 问题类型（可选，默认NORMAL）
     */
    void saveHistory(Long userId, String question, String answer, String context, 
                     Long bookId, Long knowledgeNodeId, String questionType);

    /**
     * 保存问答历史记录（带会话ID）
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param question 用户问题
     * @param answer AI回答
     * @param context 上下文信息
     * @param bookId 关联教材ID（可选）
     * @param knowledgeNodeId 关联知识点ID（可选）
     * @param questionType 问题类型（可选）
     */
    void saveHistory(Long userId, String sessionId, String question, String answer, 
                     String context, Long bookId, Long knowledgeNodeId, String questionType);

    /**
     * 获取考试准备Q&A历史记录
     * 
     * @param userId 用户ID
     * @return 考试Q&A历史列表
     */
    List<Map<String, Object>> getExamQaHistory(Long userId);
    
    /**
     * 删除考试Q&A历史记录
     * 
     * @param id 历史记录ID
     */
    void deleteExamQaHistory(Long id);
    
    /**
     * 保存考试Q&A历史记录
     * 
     * @param question 问题
     * @param answer 回答
     * @param subject 科目
     * @param userId 用户ID
     */
    void saveExamQaHistory(String question, String answer, String subject, Long userId);

    /**
     * 保存考试Q&A历史记录（带会话ID）
     * 
     * @param question 问题
     * @param answer 回答
     * @param subject 科目
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void saveExamQaHistory(String question, String answer, String subject, Long userId, String sessionId);

    /**
     * 获取课程推荐
     * 
     * @return 推荐的课程信息
     */
    Map<String, Object> getCourseRecommendations();
    
}