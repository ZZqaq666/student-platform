package com.student.platform.knowledge.service;

import com.student.platform.common.dto.KnowledgeGraphDTO;
import com.student.platform.common.dto.KnowledgeNodeDTO;
import com.student.platform.common.dto.KnowledgeRelationDTO;

import java.util.List;

public interface KnowledgeGraphService {

    KnowledgeGraphDTO getKnowledgeGraphByBook(Long bookId);

    List<KnowledgeNodeDTO> getKnowledgeTreeByBook(Long bookId);

    KnowledgeNodeDTO getKnowledgeNodeById(Long id);

    List<KnowledgeRelationDTO> getRelationsByNode(Long nodeId);

    List<KnowledgeNodeDTO> searchKnowledgeNodes(Long bookId, String keyword);
}
