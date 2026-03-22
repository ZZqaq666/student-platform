package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphDTO {
    private List<KnowledgeNodeDTO> nodes;
    private List<KnowledgeRelationDTO> relations;
}
