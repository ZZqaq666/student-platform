package com.student.platform.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.exercise.entity.Exercise;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExerciseMapper extends BaseMapper<Exercise> {

    List<Exercise> findByBookId(Long bookId);

    List<Exercise> findByKnowledgeNodeId(Long knowledgeNodeId);

    List<Exercise> findByDifficulty(String difficulty);

    List<Exercise> findByQuestionType(String questionType);
}
