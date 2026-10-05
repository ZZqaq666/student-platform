package com.student.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.user.entity.UserWeakKnowledge;
import java.util.List;

public interface UserWeakKnowledgeMapper extends BaseMapper<UserWeakKnowledge> {

    List<UserWeakKnowledge> selectByUserId(Long userId);

    List<UserWeakKnowledge> selectWeakestByUserId(Long userId, Integer limit);
}
