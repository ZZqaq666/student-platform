package com.student.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.user.entity.UserProfile;

public interface UserProfileMapper extends BaseMapper<UserProfile> {

    UserProfile selectByUserId(Long userId);
}
