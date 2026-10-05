package com.student.platform.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.exercise.entity.UserAnswerRecord;
import java.util.List;
import java.util.Map;

public interface UserAnswerRecordMapper extends BaseMapper<UserAnswerRecord> {

    List<UserAnswerRecord> selectByUserId(Long userId);

    Map<String, Object> selectStatisticsByUserIdAndSubjectId(Long userId, String subjectId);

    List<UserAnswerRecord> selectByUserIdAndKnowledgeId(Long userId, String knowledgeId);
}
