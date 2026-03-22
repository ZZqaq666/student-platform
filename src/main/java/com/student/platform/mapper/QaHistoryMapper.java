package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.QaHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QaHistoryMapper extends BaseMapper<QaHistory> {
    
    List<QaHistory> findByUserId(Long userId);
    
    List<QaHistory> findByBookId(Long bookId);
    
    List<QaHistory> findByKnowledgeNodeId(Long knowledgeNodeId);
    
    List<QaHistory> findByQuestionType(String questionType);
}
