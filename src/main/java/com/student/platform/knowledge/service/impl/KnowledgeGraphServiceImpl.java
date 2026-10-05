package com.student.platform.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.common.dto.KnowledgeGraphDTO;
import com.student.platform.common.dto.KnowledgeNodeDTO;
import com.student.platform.common.dto.KnowledgeRelationDTO;
import com.student.platform.knowledge.entity.KnowledgeNode;
import com.student.platform.knowledge.entity.KnowledgeRelation;
import com.student.platform.knowledge.mapper.KnowledgeNodeMapper;
import com.student.platform.knowledge.mapper.KnowledgeRelationMapper;
import com.student.platform.knowledge.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;

    private final KnowledgeRelationMapper knowledgeRelationMapper;

    public KnowledgeGraphDTO getKnowledgeGraphByBook(Long bookId) {
        log.debug("获取书籍知识图谱, bookId: {}", bookId);

        List<KnowledgeNode> nodes = knowledgeNodeMapper.findByBookId(bookId);
        List<KnowledgeRelation> relations = new ArrayList<>();

        if (!nodes.isEmpty()) {
            List<Long> nodeIds = nodes.stream()
                    .map(KnowledgeNode::getId)
                    .collect(Collectors.toList());
            if (!nodeIds.isEmpty()) {
                relations = knowledgeRelationMapper.findBySourceNodeIds(nodeIds);
            }
        }

        if (nodes.isEmpty()) {
            log.info("数据库中没有书籍的知识图谱数据，返回空结构，书籍ID: {}", bookId);
            return new KnowledgeGraphDTO(new ArrayList<>(), new ArrayList<>());
        }

        Map<Long, KnowledgeNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, node -> node));

        List<KnowledgeNodeDTO> nodeDTOs = nodes.stream()
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());

        List<KnowledgeRelationDTO> relationDTOs = relations.stream()
                .map(relation -> convertToRelationDTO(relation, nodeMap))
                .collect(Collectors.toList());

        return new KnowledgeGraphDTO(nodeDTOs, relationDTOs);
    }

    public List<KnowledgeNodeDTO> getKnowledgeTreeByBook(Long bookId) {
        log.debug("获取书籍知识树, bookId: {}", bookId);

        QueryWrapper<KnowledgeNode> queryWrapper = new QueryWrapper<>();
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
        log.debug("获取知识节点详情, id: {}", id);

        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("知识节点不存在");
        }
        return convertToNodeDTO(node);
    }

    public List<KnowledgeRelationDTO> getRelationsByNode(Long nodeId) {
        log.debug("获取节点关系, nodeId: {}", nodeId);

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
        log.debug("搜索知识节点, bookId: {}, keyword: {}", bookId, keyword);

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
        Integer category = determineCategory(node);
        Integer symbolSize = calculateSymbolSize(category);
        Integer value = calculateNodeValue(node);

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
                null,
                category,
                value,
                symbolSize
        );
    }

    private Integer determineCategory(KnowledgeNode node) {
        if (node.getParentId() == null) {
            return 0;
        }

        int depth = getNodeDepth(node);
        return Math.min(depth, 3);
    }

    private int getNodeDepth(KnowledgeNode node) {
        int depth = 0;
        KnowledgeNode current = node;
        while (current.getParentId() != null && depth < 10) {
            current = knowledgeNodeMapper.selectById(current.getParentId());
            if (current == null) break;
            depth++;
        }
        return depth;
    }

    private Integer calculateSymbolSize(Integer category) {
        switch (category) {
            case 0: return 80;
            case 1: return 60;
            case 2: return 45;
            case 3: return 35;
            default: return 40;
        }
    }

    private Integer calculateNodeValue(KnowledgeNode node) {
        List<KnowledgeNode> children = knowledgeNodeMapper.findByParentId(node.getId());
        return 50 + children.size() * 10;
    }

    private KnowledgeRelationDTO convertToRelationDTO(KnowledgeRelation relation) {
        return convertToRelationDTO(relation, null);
    }

    private KnowledgeRelationDTO convertToRelationDTO(KnowledgeRelation relation, Map<Long, KnowledgeNode> nodeMap) {
        KnowledgeNode sourceNode = null;
        KnowledgeNode targetNode = null;

        if (nodeMap != null) {
            sourceNode = nodeMap.get(relation.getSourceNodeId());
            targetNode = nodeMap.get(relation.getTargetNodeId());
        } else {
            sourceNode = relation.getSourceNodeId() != null
                    ? knowledgeNodeMapper.selectById(relation.getSourceNodeId()) : null;
            targetNode = relation.getTargetNodeId() != null
                    ? knowledgeNodeMapper.selectById(relation.getTargetNodeId()) : null;
        }

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
