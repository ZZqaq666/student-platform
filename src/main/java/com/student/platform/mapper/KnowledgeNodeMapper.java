package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.KnowledgeNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeNodeMapper extends BaseMapper<KnowledgeNode> {
    
    List<KnowledgeNode> findByBookId(Long bookId);
    
    List<KnowledgeNode> findByParentId(Long parentId);
    
    List<KnowledgeNode> findByBookIdAndLevel(Long bookId, Integer level);
}
