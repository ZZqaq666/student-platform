package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    
    @Select("SELECT * FROM course WHERE status = 'ACTIVE' ORDER BY created_at DESC")
    List<Course> findActiveCourses();
    
    @Select("SELECT * FROM course WHERE name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    List<Course> findByKeyword(@Param("keyword") String keyword);
}
