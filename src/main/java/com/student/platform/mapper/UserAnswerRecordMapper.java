package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.UserAnswerRecord;
import java.util.List;
import java.util.Map;

/**
 * 用户答题记录Mapper
 */
public interface UserAnswerRecordMapper extends BaseMapper<UserAnswerRecord> {
    
    /**
     * 根据用户ID获取答题记录列表
     */
    List<UserAnswerRecord> selectByUserId(Long userId);
    
    /**
     * 根据用户ID和学科ID获取答题统计
     */
    Map<String, Object> selectStatisticsByUserIdAndSubjectId(Long userId, String subjectId);
    
    /**
     * 获取用户在指定知识点的答题记录
     */
    List<UserAnswerRecord> selectByUserIdAndKnowledgeId(Long userId, String knowledgeId);
}
