package com.student.platform.service.impl;

import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.KnowledgeNodeDTO;
import com.student.platform.dto.KnowledgeRelationDTO;
import com.student.platform.entity.KnowledgeNode;
import com.student.platform.entity.KnowledgeRelation;
import com.student.platform.mapper.KnowledgeNodeMapper;
import com.student.platform.mapper.KnowledgeRelationMapper;
import com.student.platform.service.AiService;
import com.student.platform.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final KnowledgeRelationMapper knowledgeRelationMapper;
    private final AiService aiService;

    @Cacheable(value = "knowledgeGraph", key = "#bookId")
    public KnowledgeGraphDTO getKnowledgeGraphByBook(Long bookId) {
        List<KnowledgeNode> nodes = knowledgeNodeMapper.findByBookId(bookId);
        List<KnowledgeRelation> relations = new ArrayList<>();

        if (!nodes.isEmpty()) {
            // 批量查询所有关系，避免N+1查询
            List<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toList());
            if (!nodeIds.isEmpty()) {
                relations = knowledgeRelationMapper.findBySourceNodeIds(nodeIds);
            }
        }

        // 如果数据库中没有节点，返回基础结构并异步生成完整图谱
        if (nodes.isEmpty()) {
            log.info("数据库中没有书籍的知识图谱数据，返回基础结构并异步生成，书籍ID: {}", bookId);
            // 返回基础知识图谱结构
            KnowledgeGraphDTO basicGraph = generateBasicKnowledgeGraph(bookId);
            // 异步生成完整知识图谱
            CompletableFuture.runAsync(() -> {
                try {
                    aiService.generateKnowledgeGraph(bookId);
                } catch (Exception e) {
                    log.error("异步生成知识图谱失败: {}", e.getMessage(), e);
                }
            });
            return basicGraph;
        }

        // 构建节点ID到节点的映射，用于快速查找
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

    /**
     * 生成基础知识图谱结构
     */
    private KnowledgeGraphDTO generateBasicKnowledgeGraph(Long bookId) {
        List<KnowledgeNodeDTO> nodes = new ArrayList<>();
        List<KnowledgeRelationDTO> relations = new ArrayList<>();
        
        // 创建一个基础节点
        KnowledgeNodeDTO rootNode = new KnowledgeNodeDTO();
        rootNode.setId(bookId);
        rootNode.setBookId(bookId);
        rootNode.setName("知识图谱");
        rootNode.setDescription("正在生成完整知识图谱...");
        rootNode.setLevel(1);
        rootNode.setCategory(0);
        rootNode.setValue(50);
        rootNode.setSymbolSize(80);
        
        nodes.add(rootNode);
        return new KnowledgeGraphDTO(nodes, relations);
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
            sourceNode = relation.getSourceNodeId() != null ? knowledgeNodeMapper.selectById(relation.getSourceNodeId()) : null;
            targetNode = relation.getTargetNodeId() != null ? knowledgeNodeMapper.selectById(relation.getTargetNodeId()) : null;
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