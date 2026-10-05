package com.student.platform.exercise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.common.dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.student.platform.common.result.Result;
import com.student.platform.exercise.service.AiService;
import com.student.platform.exercise.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exercise")
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping("/batch")
    @Operation(summary = "批量获取题目", description = "根据ID列表批量获取题目详情")
    public Result<List<ExerciseDTO>> getExercisesByIds(@RequestParam String ids) {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();

        List<ExerciseDTO> exercises = new java.util.ArrayList<>();
        for (Long id : idList) {
            try {
                ExerciseDTO exercise = exerciseService.getExerciseById(id);
                if (exercise != null) {
                    exercises.add(exercise);
                }
            } catch (Exception e) {
                log.warn("获取题目失败, id: {}", id, e);
            }
        }

        return Result.success(exercises);
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

    @PostMapping("/wrong-book")
    @Operation(summary = "添加错题", description = "将题目添加到错题本")
    public Result<WrongBookDTO> addToWrongBook(@RequestBody AddToWrongBookRequest request) {
        return Result.success(exerciseService.addToWrongBook(request));
    }

    @PostMapping("/wrong-book/filter")
    @Operation(summary = "筛选错题本", description = "根据条件筛选错题本")
    public Result<List<WrongBookDTO>> getWrongBookByFilter(@RequestBody WrongBookFilterRequest request) {
        return Result.success(exerciseService.getWrongBookByFilter(request));
    }

    @GetMapping("/wrong-book/stats")
    @Operation(summary = "获取错题本统计", description = "获取错题本的统计数据")
    public Result<WrongBookStatsDTO> getWrongBookStats() {
        return Result.success(exerciseService.getWrongBookStats());
    }

    @PutMapping("/wrong-book/{wrongBookId}")
    @Operation(summary = "更新错题", description = "更新错题本中的错题信息")
    public Result<WrongBookDTO> updateWrongBook(
            @PathVariable Long wrongBookId,
            @RequestBody AddToWrongBookRequest request) {
        return Result.success(exerciseService.updateWrongBook(wrongBookId, request));
    }

    @PostMapping("/wrong-book/{wrongBookId}/review")
    @Operation(summary = "标记复习", description = "标记错题为已复习")
    public Result<WrongBookDTO> markAsReviewed(@PathVariable Long wrongBookId) {
        return Result.success(exerciseService.markAsReviewed(wrongBookId));
    }

    @PostMapping("/wrong-book/{wrongBookId}/redo")
    @Operation(summary = "重做错题", description = "重做错题并更新状态")
    public Result<WrongBookDTO> redoWrongBook(
            @PathVariable Long wrongBookId,
            @RequestParam String userAnswer,
            @RequestParam boolean isCorrect) {
        return Result.success(exerciseService.redoWrongBook(wrongBookId, userAnswer, isCorrect));
    }

    @GetMapping("/wrong-book/export")
    @Operation(summary = "导出错题本", description = "导出错题本数据")
    public ResponseEntity<byte[]> exportWrongBook(
            @RequestParam String format,
            @RequestBody(required = false) WrongBookFilterRequest request) {
        if (request == null) {
            request = new WrongBookFilterRequest();
        }
        byte[] data = exerciseService.exportWrongBook(format, request);

        String contentType = "json".equalsIgnoreCase(format) ? "application/json" : "text/csv";
        String filename = "wrongbook_" + System.currentTimeMillis() + "." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @GetMapping("/exercises")
    @Operation(summary = "获取习题列表", description = "根据知识点和难度获取习题列表")
    public Result<List<ExerciseDTO>> getExercises(
            @RequestParam(value = "knowledgePoint", required = false) String knowledgePoint,
            @RequestParam(value = "difficulty", required = false) String difficulty) {
        try {
            if (knowledgePoint == null || knowledgePoint.trim().isEmpty()) {
                knowledgePoint = "高等数学";
            }
            if (difficulty == null || difficulty.trim().isEmpty()) {
                difficulty = "all";
            }
            String aiResponse = aiService.generateExercises(knowledgePoint, difficulty);

            String jsonContent = extractJsonFromResponse(aiResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            Map responseMap = objectMapper.readValue(jsonContent, Map.class);
            List<Map<String, Object>> exercisesList = (List<Map<String, Object>>) responseMap.get("exercises");

            List<ExerciseDTO> exerciseDTOs = new ArrayList<>();
            if (exercisesList != null) {
                for (int i = 0; i < exercisesList.size(); i++) {
                    Map<String, Object> exerciseMap = exercisesList.get(i);
                    ExerciseDTO exerciseDTO = new ExerciseDTO();
                    Object idObj = exerciseMap.get("id");
                    if (idObj != null) {
                        if (idObj instanceof Integer) {
                            exerciseDTO.setId(((Integer) idObj).longValue());
                        } else if (idObj instanceof Long) {
                            exerciseDTO.setId((Long) idObj);
                        } else if (idObj instanceof String) {
                            exerciseDTO.setId(Long.parseLong((String) idObj));
                        }
                    } else {
                        exerciseDTO.setId(System.currentTimeMillis() + i);
                    }
                    String title = cleanHtml((String) exerciseMap.get("title"));
                    String description = cleanHtml((String) exerciseMap.get("description"));
                    exerciseDTO.setTitle(title);
                    exerciseDTO.setContent(description);

                    String type = (String) exerciseMap.get("type");
                    Object optionsObj = exerciseMap.get("options");
                    String optionsStr = "";
                    boolean hasOptions = false;

                    if (optionsObj instanceof List<?> optionsList && !optionsList.isEmpty()) {
                        optionsStr = String.join("@@@", optionsList.stream().map(Object::toString).toArray(String[]::new));
                        hasOptions = true;
                    } else if (optionsObj instanceof String && !((String) optionsObj).isEmpty()) {
                        optionsStr = (String) optionsObj;
                        hasOptions = true;
                    }

                    if (type != null && !type.isEmpty()) {
                        exerciseDTO.setQuestionType(type);
                    } else if (hasOptions) {
                        exerciseDTO.setQuestionType("选择题");
                    } else {
                        exerciseDTO.setQuestionType("填空题");
                    }

                    exerciseDTO.setDifficulty(difficulty);
                    exerciseDTO.setScore(10);
                    exerciseDTO.setAnswer(cleanHtml((String) exerciseMap.get("answer")));
                    exerciseDTO.setAnalysis(cleanHtml((String) exerciseMap.get("analysis")));
                    if (exerciseDTO.getAnalysis() == null || exerciseDTO.getAnalysis().isEmpty()) {
                        exerciseDTO.setAnalysis("这是一道关于" + knowledgePoint + "的习题");
                    }
                    exerciseDTO.setOptions(cleanHtml(optionsStr));
                    exerciseDTOs.add(exerciseDTO);
                }
            }

            return Result.success(exerciseDTOs);
        } catch (Exception e) {
            log.error("获取习题列表失败: {}", e.getMessage(), e);
            return Result.error("获取习题列表失败: " + e.getMessage());
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{\"exercises\": []}";
        }

        String trimmed = response.trim();

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return sanitizeJson(trimmed);
        }

        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return sanitizeJson(trimmed.substring(jsonStart, jsonEnd + 1));
        }

        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end != -1) {
                return sanitizeJson(trimmed.substring(start, end).trim());
            }
        }

        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end != -1) {
                return sanitizeJson(trimmed.substring(start, end).trim());
            }
        }

        return "{\"exercises\": []}";
    }

    private String sanitizeJson(String json) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':
                    case '\\':
                    case '/':
                    case 'b':
                    case 'f':
                    case 'n':
                    case 'r':
                    case 't':
                        sb.append(c).append(next);
                        i++;
                        break;
                    case 'u':
                        if (i + 5 <= json.length()) {
                            String unicode = json.substring(i + 2, i + 6);
                            if (unicode.matches("[0-9a-fA-F]{4}")) {
                                sb.append(c).append(next).append(unicode);
                                i += 5;
                                break;
                            }
                        }
                    default:
                        sb.append(c);
                        sb.append(next);
                        i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String cleaned = html.replaceAll("<[^>]*>", "");
        cleaned = cleaned.replace("&nbsp;", " ");
        cleaned = cleaned.replace("&amp;", "&");
        cleaned = cleaned.replace("&lt;", "<");
        cleaned = cleaned.replace("&gt;", ">");
        return cleaned;
    }
}
