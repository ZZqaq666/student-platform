package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.UserSubjectPreference;
import java.util.List;

/**
 * 用户学科偏好Mapper
 */
public interface UserSubjectPreferenceMapper extends BaseMapper<UserSubjectPreference> {
    
    /**
     * 根据用户ID获取学科偏好列表
     */
    List<UserSubjectPreference> selectByUserId(Long userId);
    
    /**
     * 根据用户ID和学科ID获取偏好记录
     */
    UserSubjectPreference selectByUserIdAndSubjectId(Long userId, String subjectId);
}
