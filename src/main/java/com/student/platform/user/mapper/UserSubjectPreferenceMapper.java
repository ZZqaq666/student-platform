package com.student.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.user.entity.UserSubjectPreference;
import java.util.List;

public interface UserSubjectPreferenceMapper extends BaseMapper<UserSubjectPreference> {

    List<UserSubjectPreference> selectByUserId(Long userId);

    UserSubjectPreference selectByUserIdAndSubjectId(Long userId, String subjectId);
}
