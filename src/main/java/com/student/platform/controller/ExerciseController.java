package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.student.platform.service.ExerciseService;
import com.student.platform.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exercise")
@RequiredArgsConstructor
@Tag(name = "练习系统", description = "练习系统相关接口")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final AiService aiService;

    @GetMapping("/book/{bookId}")
    @Operation(summary = "获取书籍题目", description = "获取指定书籍的题目列表")
    public Result<List<ExerciseDTO>> getExercisesByBook(
            @PathVariable Long bookId) {
        return Result.success(exerciseService.getExercisesByBook(bookId));
    }

    @GetMapping("/knowledge-node/{knowledgeNodeId}")
    @Operation(summary = "获取知识点题目", description = "获取指定知识点的题目列表")
    public Result<List<ExerciseDTO>> getExercisesByKnowledgeNode(
            @PathVariable Long knowledgeNodeId) {
        return Result.success(exerciseService.getExercisesByKnowledgeNode(knowledgeNodeId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情", description = "根据ID获取题目详情")
    public Result<ExerciseDTO> getExerciseById(@PathVariable Long id) {
        return Result.success(exerciseService.getExerciseById(id));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交答案", description = "提交题目答案并获取结果")
    public Result<AnswerRecordDTO> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return Result.success(exerciseService.submitAnswer(request));
    }

    @GetMapping("/history")
    @Operation(summary = "获取答题历史", description = "获取当前用户的答题历史")
    public Result<List<AnswerRecordDTO>> getAnswerHistory() {
        return Result.success(exerciseService.getAnswerHistory());
    }

    @GetMapping("/wrong-book")
    @Operation(summary = "获取错题本", description = "获取当前用户的错题本")
    public Result<List<WrongBookDTO>> getWrongBook() {
        return Result.success(exerciseService.getWrongBook());
    }

    @GetMapping("/stats")
    @Operation(summary = "获取练习统计", description = "获取当前用户的练习统计数据")
    public Result<ExerciseStatsDTO> getExerciseStats() {
        return Result.success(exerciseService.getExerciseStats());
    }

    @PutMapping("/wrong-book/{wrongBookId}/notes")
    @Operation(summary = "更新错题笔记", description = "更新错题本中的笔记")
    public Result<WrongBookDTO> updateWrongBookNotes(
            @PathVariable Long wrongBookId,
            @RequestBody String notes) {
        return Result.success(exerciseService.updateWrongBookNotes(wrongBookId, notes));
    }

    @DeleteMapping("/wrong-book/{wrongBookId}")
    @Operation(summary = "移出错题本", description = "将题目从错题本中移除")
    public Result<Void> removeFromWrongBook(@PathVariable Long wrongBookId) {
        exerciseService.removeFromWrongBook(wrongBookId);
        return Result.success();
    }

    @GetMapping("/exercises")
    @Operation(summary = "获取习题列表", description = "根据知识点和难度获取习题列表")
    public Result<List<ExerciseDTO>> getExercises(
            @RequestParam(value = "knowledgePoint") String knowledgePoint,
            @RequestParam(value = "difficulty") String difficulty) {
        try {
            // 调用AI服务生成习题
            String aiResponse = aiService.generateExercises(knowledgePoint, difficulty);
            
            // 解析AI返回的JSON数据
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(aiResponse, Map.class);
            List<Map<String, Object>> exercisesList = (List<Map<String, Object>>) responseMap.get("exercises");
            
            // 转换为ExerciseDTO列表
            List<ExerciseDTO> exerciseDTOs = new ArrayList<>();
            if (exercisesList != null) {
                for (int i = 0; i < exercisesList.size(); i++) {
                    Map<String, Object> exerciseMap = exercisesList.get(i);
                    ExerciseDTO exerciseDTO = new ExerciseDTO();
                    // 处理id类型转换
                    Object idObj = exerciseMap.get("id");
                    if (idObj != null) {
                        if (idObj instanceof Integer) {
                            exerciseDTO.setId(((Integer) idObj).longValue());
                        } else if (idObj instanceof Long) {
                            exerciseDTO.setId((Long) idObj);
                        }
                    }
                    exerciseDTO.setTitle((String) exerciseMap.get("title"));
                    exerciseDTO.setContent((String) exerciseMap.get("description"));
                    exerciseDTO.setQuestionType("选择题"); // 默认类型
                    exerciseDTO.setDifficulty(difficulty);
                    exerciseDTO.setScore(10); // 默认分数
                    exerciseDTO.setAnswer((String) exerciseMap.get("answer"));
                    exerciseDTO.setAnalysis("这是一道关于" + knowledgePoint + "的习题");
                    
                    // 设置选项
                    exerciseDTO.setOptions("选项A,选项B,选项C,选项D");
                    
                    exerciseDTOs.add(exerciseDTO);
                }
            }
            
            return Result.success(exerciseDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取习题列表失败: " + e.getMessage());
        }
    }
}
