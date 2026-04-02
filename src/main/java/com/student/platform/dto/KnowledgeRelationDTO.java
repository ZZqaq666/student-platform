package com.student.platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    
    public Long getSource() {
        return sourceNodeId;
    }
    
    public Long getTarget() {
        return targetNodeId;
    }
}
