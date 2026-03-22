package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.LearningProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LearningProgressMapper extends BaseMapper<LearningProgress> {
    
    List<LearningProgress> findByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    List<LearningProgress> findByUserIdAndDateRange(
            @Param("userId") Long userId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate
    );
    
    List<LearningProgress> findByUserIdAndBookIdAndDateRange(
            @Param("userId") Long userId, 
            @Param("bookId") Long bookId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate
    );
    
    LearningProgress findByUserIdAndBookIdAndDate(
            @Param("userId") Long userId, 
            @Param("bookId") Long bookId, 
            @Param("date") LocalDate date
    );
}
