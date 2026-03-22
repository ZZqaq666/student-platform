package com.student.platform.controller;

import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.KnowledgeNodeDTO;
import com.student.platform.dto.KnowledgeRelationDTO;
import com.student.platform.dto.Result;
import com.student.platform.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge-graph")
@RequiredArgsConstructor
@Tag(name = "知识图谱", description = "知识图谱相关接口")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/book/{bookId}")
    @Operation(summary = "获取书籍知识图谱", description = "获取指定书籍的完整知识图谱")
    public Result<KnowledgeGraphDTO> getKnowledgeGraphByBook(@PathVariable Long bookId) {
        return Result.success(knowledgeGraphService.getKnowledgeGraphByBook(bookId));
    }

    @GetMapping("/book/{bookId}/tree")
    @Operation(summary = "获取书籍知识树", description = "获取指定书籍的知识树形结构")
    public Result<List<KnowledgeNodeDTO>> getKnowledgeTreeByBook(@PathVariable Long bookId) {
        return Result.success(knowledgeGraphService.getKnowledgeTreeByBook(bookId));
    }

    @GetMapping("/node/{id}")
    @Operation(summary = "获取知识节点详情", description = "根据ID获取知识节点的详细信息")
    public Result<KnowledgeNodeDTO> getKnowledgeNodeById(@PathVariable Long id) {
        return Result.success(knowledgeGraphService.getKnowledgeNodeById(id));
    }

    @GetMapping("/node/{nodeId}/relations")
    @Operation(summary = "获取节点关系", description = "获取指定知识节点的所有关系")
    public Result<List<KnowledgeRelationDTO>> getRelationsByNode(@PathVariable Long nodeId) {
        return Result.success(knowledgeGraphService.getRelationsByNode(nodeId));
    }

    @GetMapping("/book/{bookId}/search")
    @Operation(summary = "搜索知识节点", description = "在指定书籍中搜索知识节点")
    public Result<List<KnowledgeNodeDTO>> searchKnowledgeNodes(
            @PathVariable Long bookId,
            @RequestParam String keyword) {
        return Result.success(knowledgeGraphService.searchKnowledgeNodes(bookId, keyword));
    }
}
