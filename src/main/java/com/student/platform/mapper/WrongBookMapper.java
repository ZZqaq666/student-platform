package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.WrongBook;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WrongBookMapper extends BaseMapper<WrongBook> {
    
    List<WrongBook> findByUserId(Long userId);
    
    WrongBook findByUserIdAndExerciseId(Long userId, Long exerciseId);
    
    List<WrongBook> findByUserIdAndMasterStatus(Long userId, String masterStatus);
}
