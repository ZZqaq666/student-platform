package com.student.platform.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.learning.entity.UserProfile;

public interface UserProfileMapper extends BaseMapper<UserProfile> {

    UserProfile selectByUserId(Long userId);
}
