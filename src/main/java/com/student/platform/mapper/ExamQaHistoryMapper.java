package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.ExamQaHistory;

import java.util.List;

public interface ExamQaHistoryMapper extends BaseMapper<ExamQaHistory> {

    List<ExamQaHistory> findByUserId(Long userId);

    List<ExamQaHistory> findRecentByLimit(int limit);
}
