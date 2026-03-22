package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.QaRequest;
import com.student.platform.dto.QaResponse;
import com.student.platform.dto.Result;
import com.student.platform.dto.SseMessage;
import com.student.platform.service.AiService;
import com.student.platform.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@Tag(name = "AI问答", description = "AI问答相关接口")
public class QaController {

    private final QaService qaService;
    private final AiService aiService;



    @GetMapping("/history")
    @Operation(summary = "获取历史记录", description = "获取当前用户的问答历史记录")
    public Result<List<QaResponse>> getHistory() {
        List<QaResponse> histories = qaService.getHistory();
        return Result.success(histories);
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除历史记录", description = "删除指定的问答历史记录")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        qaService.deleteHistory(id);
        return Result.success();
    }
    
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提问", description = "向AI提问并获取流式回答")
    public Flux<SseMessage> askQuestionStream(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        return aiService.generateStreamResponseWithContext(context, request.getQuestion());
    }
    
    @PostMapping(value = "/ask/stream/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提问测试", description = "向AI提问并获取流式回答（无需认证）")
    public Flux<SseMessage> askQuestionStreamTest(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        return aiService.generateStreamResponseWithContext(context, request.getQuestion());
    }
    
    @PostMapping("/ask")
    @Operation(summary = "非流式提问", description = "向AI提问并获取完整回答")
    public Result<String> askQuestion(@RequestBody QaRequest request) {
        String context = buildSystemPrompt(request);
        String response = aiService.generateResponseWithContext(context, request.getQuestion());
        return Result.success(response);
    }
    
    private String buildSystemPrompt(QaRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的学科问答助手，请根据用户的问题提供详细、准确的回答。");
        
        if (request.getSubject() != null) {
            prompt.append("\n学科：").append(request.getSubject());
        }
        
        return prompt.toString();
    }
    
    @GetMapping("/exam/subjects")
    @Operation(summary = "获取考试科目", description = "获取所有可用的考试科目")
    public Result<List<Map<String, Object>>> getExamSubjects() {
        List<Map<String, Object>> subjects = qaService.getExamSubjects();
        return Result.success(subjects);
    }
    
    @GetMapping("/exam/subjects/{subjectId}/key-points")
    @Operation(summary = "获取知识点", description = "获取指定科目的知识点")
    public Result<List<Map<String, Object>>> getKeyPoints(@PathVariable String subjectId) {
        List<Map<String, Object>> keyPoints = qaService.getKeyPoints(subjectId);
        return Result.success(keyPoints);
    }
    
    @GetMapping("/exam/history")
    @Operation(summary = "获取考试历史", description = "获取当前用户的考试历史")
    public Result<List<Map<String, Object>>> getExamHistory() {
        List<Map<String, Object>> examHistory = qaService.getExamHistory();
        return Result.success(examHistory);
    }
    
    @GetMapping("/books")
    @Operation(summary = "获取书籍列表", description = "获取所有可用的书籍")
    public Result<List<Map<String, Object>>> getBooks() {
        List<Map<String, Object>> books = qaService.getBooks();
        return Result.success(books);
    }
    
    @GetMapping("/courses")
    @Operation(summary = "获取课程列表", description = "获取所有可用的课程")
    public Result<List<Map<String, Object>>> getCourses() {
        List<Map<String, Object>> courses = qaService.getCourses();
        return Result.success(courses);
    }
}
