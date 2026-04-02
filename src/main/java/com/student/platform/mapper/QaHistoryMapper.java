package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.QaHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QaHistoryMapper extends BaseMapper<QaHistory> {
    
    List<QaHistory> findByUserId(Long userId);
    
    List<QaHistory> findByBookId(Long bookId);
    
    List<QaHistory> findByKnowledgeNodeId(Long knowledgeNodeId);
    
    List<QaHistory> findByQuestionType(String questionType);
    
    @Select("SELECT * FROM qa_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<QaHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    @Select("SELECT * FROM qa_history ORDER BY created_at DESC LIMIT #{limit}")
    List<QaHistory> findRecentByLimit(@Param("limit") Integer limit);
}
