package com.student.platform.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.learning.entity.UserSubjectPreference;
import java.util.List;

public interface UserSubjectPreferenceMapper extends BaseMapper<UserSubjectPreference> {

    List<UserSubjectPreference> selectByUserId(Long userId);

    UserSubjectPreference selectByUserIdAndSubjectId(Long userId, String subjectId);
}
