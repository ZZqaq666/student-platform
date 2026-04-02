package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.BookKnowledgePoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookKnowledgePointMapper extends BaseMapper<BookKnowledgePoint> {
    
    @Select("SELECT * FROM book_knowledge_point WHERE book_id = #{bookId} AND type = #{type}")
    List<BookKnowledgePoint> findByBookIdAndType(Long bookId, String type);
    
    @Select("SELECT * FROM book_knowledge_point WHERE book_id = #{bookId}")
    List<BookKnowledgePoint> findByBookId(Long bookId);
}
