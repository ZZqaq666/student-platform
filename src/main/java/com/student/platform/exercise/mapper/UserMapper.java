package com.student.platform.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(String username);

    User findByEmail(String email);

    User findByPhone(String phone);
}
