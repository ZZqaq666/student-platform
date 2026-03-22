package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.Book;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    
    List<Book> findBySubject(String subject);
    
    List<Book> findByGrade(String grade);
    
    List<Book> searchByTitle(String keyword);
}
