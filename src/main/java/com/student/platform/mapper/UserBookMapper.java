package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.UserBook;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserBookMapper extends BaseMapper<UserBook> {
    
    List<UserBook> findByUserId(Long userId);
    
    UserBook findByUserIdAndBookId(Long userId, Long bookId);
    
    List<UserBook> findByUserIdAndStatus(Long userId, String status);
}
