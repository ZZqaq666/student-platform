package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.Exercise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExerciseMapper extends BaseMapper<Exercise> {
    
    List<Exercise> findByBookId(Long bookId);
    
    List<Exercise> findByKnowledgeNodeId(Long knowledgeNodeId);
    
    List<Exercise> findByDifficulty(String difficulty);
    
    List<Exercise> findByQuestionType(String questionType);
}
