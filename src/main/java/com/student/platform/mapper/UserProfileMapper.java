package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.UserProfile;

/**
 * 用户画像Mapper
 */
public interface UserProfileMapper extends BaseMapper<UserProfile> {
    
    /**
     * 根据用户ID获取用户画像
     */
    UserProfile selectByUserId(Long userId);
}
