package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookChapterMapper extends BaseMapper<BookChapter> {

    @Select("SELECT * FROM book_chapter WHERE book_id = #{bookId} AND status = 'ACTIVE' ORDER BY sort_order ASC, id ASC")
    List<BookChapter> selectByBookId(@Param("bookId") Long bookId);

    @Select("SELECT * FROM book_chapter WHERE book_id = #{bookId} AND status = 'ACTIVE' AND title LIKE CONCAT('%', #{keyword}, '%') ORDER BY sort_order ASC, id ASC")
    List<BookChapter> searchChapters(@Param("bookId") Long bookId, @Param("keyword") String keyword);
}
