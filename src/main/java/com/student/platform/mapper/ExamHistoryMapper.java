package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.ExamHistory;

import java.util.List;

public interface ExamHistoryMapper extends BaseMapper<ExamHistory> {
    List<ExamHistory> selectByUserId(Long userId);
}
