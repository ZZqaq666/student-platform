package com.student.platform.service.impl;

import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.KnowledgeNodeDTO;
import com.student.platform.dto.KnowledgeRelationDTO;
import com.student.platform.entity.KnowledgeNode;
import com.student.platform.entity.KnowledgeRelation;
import com.student.platform.mapper.KnowledgeNodeMapper;
import com.student.platform.mapper.KnowledgeRelationMapper;
import com.student.platform.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final KnowledgeRelationMapper knowledgeRelationMapper;

    @Cacheable(value = "knowledgeGraph", key = "#bookId")
    public KnowledgeGraphDTO getKnowledgeGraphByBook(Long bookId) {
        List<KnowledgeNode> nodes = knowledgeNodeMapper.findByBookId(bookId);
        List<KnowledgeRelation> relations = new ArrayList<>();

        for (KnowledgeNode node : nodes) {
            relations.addAll(knowledgeRelationMapper.findBySourceNodeId(node.getId()));
        }

        List<KnowledgeNodeDTO> nodeDTOs = nodes.stream()
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());

        List<KnowledgeRelationDTO> relationDTOs = relations.stream()
                .map(this::convertToRelationDTO)
                .collect(Collectors.toList());

        return new KnowledgeGraphDTO(nodeDTOs, relationDTOs);
    }

    public List<KnowledgeNodeDTO> getKnowledgeTreeByBook(Long bookId) {
        // 查询所有根节点（parent_id 为 null）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgeNode> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("book_id", bookId);
        queryWrapper.isNull("parent_id");
        queryWrapper.eq("status", "ACTIVE");
        queryWrapper.orderByAsc("sort_order");
        List<KnowledgeNode> rootNodes = knowledgeNodeMapper.selectList(queryWrapper);
        return rootNodes.stream()
                .map(this::buildNodeTree)
                .collect(Collectors.toList());
    }

    public KnowledgeNodeDTO getKnowledgeNodeById(Long id) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("知识节点不存在");
        }
        return convertToNodeDTO(node);
    }

    public List<KnowledgeRelationDTO> getRelationsByNode(Long nodeId) {
        List<KnowledgeRelation> sourceRelations = knowledgeRelationMapper.findBySourceNodeId(nodeId);
        List<KnowledgeRelation> targetRelations = knowledgeRelationMapper.findByTargetNodeId(nodeId);

        List<KnowledgeRelation> allRelations = new ArrayList<>();
        allRelations.addAll(sourceRelations);
        allRelations.addAll(targetRelations);

        return allRelations.stream()
                .map(this::convertToRelationDTO)
                .collect(Collectors.toList());
    }

    public List<KnowledgeNodeDTO> searchKnowledgeNodes(Long bookId, String keyword) {
        List<KnowledgeNode> nodes = knowledgeNodeMapper.findByBookId(bookId);
        return nodes.stream()
                .filter(node -> node.getName().contains(keyword) ||
                        (node.getDescription() != null && node.getDescription().contains(keyword)) ||
                        (node.getContent() != null && node.getContent().contains(keyword)))
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());
    }

    private KnowledgeNodeDTO buildNodeTree(KnowledgeNode node) {
        KnowledgeNodeDTO dto = convertToNodeDTO(node);
        List<KnowledgeNode> children = knowledgeNodeMapper.findByParentId(node.getId());
        if (!children.isEmpty()) {
            dto.setChildren(children.stream()
                    .map(this::buildNodeTree)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private KnowledgeNodeDTO convertToNodeDTO(KnowledgeNode node) {
        return new KnowledgeNodeDTO(
                node.getId(),
                node.getBookId(),
                node.getParentId(),
                node.getName(),
                node.getDescription(),
                node.getContent(),
                node.getLevel(),
                node.getSortOrder(),
                node.getStatus(),
                node.getCreatedAt(),
                node.getUpdatedAt(),
                null
        );
    }

    private KnowledgeRelationDTO convertToRelationDTO(KnowledgeRelation relation) {
        KnowledgeNode sourceNode = relation.getSourceNodeId() != null ? knowledgeNodeMapper.selectById(relation.getSourceNodeId()) : null;
        KnowledgeNode targetNode = relation.getTargetNodeId() != null ? knowledgeNodeMapper.selectById(relation.getTargetNodeId()) : null;
        return new KnowledgeRelationDTO(
                relation.getId(),
                relation.getSourceNodeId(),
                sourceNode != null ? sourceNode.getName() : null,
                relation.getTargetNodeId(),
                targetNode != null ? targetNode.getName() : null,
                relation.getRelationType(),
                relation.getDescription(),
                relation.getCreatedAt(),
                relation.getUpdatedAt()
        );
    }
}