package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNodeDTO {
    private Long id;
    private Long bookId;
    private Long parentId;
    private String name;
    private String description;
    private String content;
    private Integer level;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<KnowledgeNodeDTO> children;
}
