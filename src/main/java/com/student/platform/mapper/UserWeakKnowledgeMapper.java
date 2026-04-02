package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.UserWeakKnowledge;
import java.util.List;

/**
 * 用户薄弱知识点Mapper
 */
public interface UserWeakKnowledgeMapper extends BaseMapper<UserWeakKnowledge> {
    
    /**
     * 根据用户ID获取薄弱知识点列表
     */
    List<UserWeakKnowledge> selectByUserId(Long userId);
    
    /**
     * 根据用户ID获取掌握程度最低的前N个知识点
     */
    List<UserWeakKnowledge> selectWeakestByUserId(Long userId, Integer limit);
}
