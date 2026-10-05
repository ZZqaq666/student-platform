package com.student.platform.qa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 答疑历史记录实体
 * 
 * 对应数据库表：qa_history
 * 
 * 功能说明：
 * 存储用户的答疑历史，包括问题、回答、评分等信息
 * 支持查询、删除等操作
 * 
 * 使用场景：
 * - 用户查看历史答疑记录
 * - AI模型训练数据收集
 * - 用户反馈和质量评估
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("qa_history")
public class QaHistory {

    /**
     * 主键ID
     * 自增生成策略
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     * 
     * 说明：提问用户的唯一标识
     * 关联：user表的id字段
     * 必填：是
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 教材ID
     * 
     * 说明：用户提问时所在的教材上下文
     * 关联：book表的id字段
     * 必填：否
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 知识点ID
     * 
     * 说明：用户提问相关的知识点
     * 关联：knowledge_node表的id字段
     * 必填：否
     */
    @TableField("knowledge_node_id")
    private Long knowledgeNodeId;

    /**
     * 问题类型
     * 
     * 说明：用于区分不同类型的答疑问题
     * 取值示例：
     * - NORMAL：普通答疑
     * - PROCESSED：文本处理后保存
     * - EXAM：考试相关问题
     * - COURSE：课程相关问题
     * 必填：否，默认NORMAL
     */
    @TableField("question_type")
    private String questionType;

    /**
     * 会话ID
     * 
     * 说明：用于标识同一会话中的多条问答记录
     * 用途：历史记录按会话聚合展示
     * 必填：否
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 用户问题
     * 
     * 说明：用户提出的原始问题
     * 长度限制：建议不超过5000字符
     * 安全处理：保存前经过XSS过滤
     */
    @TableField("question")
    private String question;

    /**
     * AI回答
     * 
     * 说明：AI模型生成的回答内容
     * 长度限制：建议不超过10000字符
     * 安全处理：保存前经过XSS过滤
     */
    @TableField("answer")
    private String answer;

    /**
     * 上下文信息
     * 
     * 说明：保存提问时的上下文，如科目、教材章节等
     * 用途：用于后续检索和上下文理解
     * 安全处理：保存前经过XSS过滤
     */
    @TableField("context")
    private String context;

    /**
     * 使用的Token数量
     * 
     * 说明：回答这个问题消耗的Token数量
     * 用途：用于计费和成本分析
     * 必填：否
     */
    @TableField("tokens_used")
    private Integer tokensUsed;

    /**
     * 用户评分
     * 
     * 说明：用户对AI回答的满意度评分
     * 取值范围：1-5（1=非常不满意，5=非常满意）
     * 用途：用于评估回答质量和持续优化
     */
    @TableField("rating")
    private Integer rating;

    /**
     * 用户反馈
     * 
     * 说明：用户对回答的详细反馈
     * 用途：收集用户意见，改进AI回答
     * 必填：否
     */
    @TableField("feedback")
    private String feedback;

    /**
     * 创建时间
     * 
     * 说明：答疑记录的创建时间
     * 默认值：当前时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 
     * 说明：答疑记录的最后更新时间
     * 触发：用户评分、反馈等操作时更新
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
