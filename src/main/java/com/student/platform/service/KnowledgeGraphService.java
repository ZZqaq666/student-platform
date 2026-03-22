package com.student.platform.service;

import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.KnowledgeNodeDTO;
import com.student.platform.dto.KnowledgeRelationDTO;

import java.util.List;

public interface KnowledgeGraphService {
    KnowledgeGraphDTO getKnowledgeGraphByBook(Long bookId);
    List<KnowledgeNodeDTO> getKnowledgeTreeByBook(Long bookId);
    KnowledgeNodeDTO getKnowledgeNodeById(Long id);
    List<KnowledgeRelationDTO> getRelationsByNode(Long nodeId);
    List<KnowledgeNodeDTO> searchKnowledgeNodes(Long bookId, String keyword);
}