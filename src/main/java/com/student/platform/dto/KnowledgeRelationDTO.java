package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelationDTO {
    private Long id;
    private Long sourceNodeId;
    private String sourceNodeName;
    private Long targetNodeId;
    private String targetNodeName;
    private String relationType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
