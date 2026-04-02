package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.ExamQuestion;
import java.util.List;
import java.util.Map;

/**
 * 真题题目Mapper
 */
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {
    
    /**
     * 根据试卷ID获取题目列表
     */
    List<ExamQuestion> selectByPaperId(String paperId);
    
    /**
     * 根据学科ID获取题目列表
     */
    List<ExamQuestion> selectBySubjectId(String subjectId);
    
    /**
     * 根据知识点ID获取题目列表
     */
    List<ExamQuestion> selectByKnowledgeId(String knowledgeId);
    
    /**
     * 根据难度级别获取题目
     */
    List<ExamQuestion> selectByDifficultyLevel(Integer difficultyLevel);
    
    /**
     * 根据标签获取题目
     */
    List<ExamQuestion> selectByTag(String tagName);
    
    /**
     * 获取题目详情
     */
    Map<String, Object> selectQuestionDetail(String questionId);
}
