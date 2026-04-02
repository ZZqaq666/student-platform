package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.ExamPaper;
import java.util.List;
import java.util.Map;

/**
 * 真题试卷Mapper
 */
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {
    
    /**
     * 根据学科ID获取试卷列表
     */
    List<ExamPaper> selectBySubjectId(String subjectId);
    
    /**
     * 根据考试类型获取试卷列表
     */
    List<ExamPaper> selectByExamType(String examType);
    
    /**
     * 根据难度范围获取试卷
     */
    List<ExamPaper> selectByDifficultyRange(Double minDifficulty, Double maxDifficulty);
    
    /**
     * 获取试卷详情
     */
    Map<String, Object> selectPaperDetail(String paperId);
}
