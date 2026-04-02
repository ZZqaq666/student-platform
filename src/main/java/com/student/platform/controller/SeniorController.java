package com.student.platform.controller;

import com.student.platform.dto.Result;
import com.student.platform.service.SeniorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/senior")
public class SeniorController {

    @Autowired
    private SeniorService seniorService;

    // 获取所有学长学姐
    @GetMapping("/list")
    public Result<?> getSeniors() {
        return Result.success(seniorService.getSeniors());
    }

    // 获取学长学姐详情
    @GetMapping("/{id}")
    public Result<?> getSeniorById(@PathVariable String id) {
        return Result.success(seniorService.getSeniorById(id));
    }

    // 获取问题列表
    @GetMapping("/questions")
    public Result<?> getQuestions(@RequestParam(required = false) String seniorId,
                                 @RequestParam(required = false) String filter,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "6") int pageSize,
                                 @RequestParam(required = false) Integer userId) {
        // userId参数由前端自动添加，但后端暂不使用
        return Result.success(seniorService.getQuestions(seniorId, filter, sort, page, pageSize));
    }

    // 获取问题详情
    @GetMapping("/questions/{id}")
    public Result<?> getQuestionById(@PathVariable String id) {
        return Result.success(seniorService.getQuestionById(id));
    }

    // 发布问题
    @PostMapping("/questions")
    public Result<?> publishQuestion(@RequestBody Map<String, Object> data) {
        return Result.success(seniorService.publishQuestion(data));
    }

    // 点赞回答
    @PostMapping("/answers/{answerId}/like")
    public Result<?> likeAnswer(@PathVariable String answerId) {
        seniorService.likeAnswer(answerId);
        return Result.success();
    }

    // 采纳回答
    @PutMapping("/questions/{questionId}/accept")
    public Result<?> acceptAnswer(@PathVariable String questionId, @RequestBody Map<String, String> data) {
        seniorService.acceptAnswer(questionId, data.get("answerId"));
        return Result.success();
    }

    // 提交追问
    @PostMapping("/questions/{questionId}/follow-up")
    public Result<?> submitFollowUp(@PathVariable String questionId, @RequestBody Map<String, String> data) {
        seniorService.submitFollowUp(questionId, data.get("content"));
        return Result.success();
    }
}
