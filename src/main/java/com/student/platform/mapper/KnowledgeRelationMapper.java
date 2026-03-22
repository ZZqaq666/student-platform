package com.student.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.platform.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {
    
    List<KnowledgeRelation> findBySourceNodeId(Long sourceNodeId);
    
    List<KnowledgeRelation> findByTargetNodeId(Long targetNodeId);
    
    List<KnowledgeRelation> findByRelationType(String relationType);
}
