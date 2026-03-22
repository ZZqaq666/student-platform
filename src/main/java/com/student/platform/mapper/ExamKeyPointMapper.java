package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.ExamKeyPoint;

import java.util.List;

public interface ExamKeyPointMapper extends BaseMapper<ExamKeyPoint> {
    List<ExamKeyPoint> selectBySubjectId(String subjectId);
}
