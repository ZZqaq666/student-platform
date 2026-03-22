package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.AnswerRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {
    
    List<AnswerRecord> findByUserId(Long userId);
    
    List<AnswerRecord> findByExerciseId(Long exerciseId);
    
    List<AnswerRecord> findByUserIdAndIsCorrect(Long userId, Boolean isCorrect);
    
    AnswerRecord findLatestByUserIdAndExerciseId(Long userId, Long exerciseId);
}
