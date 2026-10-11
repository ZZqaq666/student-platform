package com.student.platform.qa.controller;

import com.student.platform.common.config.CurrentUserProvider;
import com.student.platform.common.result.Result;
import com.student.platform.qa.service.SeniorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学长学姐答疑接口。
 *
 * 登录用户统一由 {@link CurrentUserProvider} 解析，不再依赖前端传入的 userId，
 * 也不再把作者硬编码为 'u-current' / '我'。
 */
@Slf4j
@RestController
@RequestMapping("/senior")
@RequiredArgsConstructor
public class SeniorController {

    private final SeniorService seniorService;
    private final CurrentUserProvider currentUserProvider;

    // 获取统计数据
    @GetMapping("/stats")
    public Result<?> getStats() {
        return Result.success(seniorService.getStats());
    }

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

    // 获取问题列表（支持学科过滤 + 真实分页）
    @GetMapping("/questions")
    public Result<?> getQuestions(@RequestParam(required = false) String seniorId,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String filter,
                                  @RequestParam(required = false) String sort,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = currentUserIdOrNull();
        return Result.success(seniorService.getQuestions(seniorId, category, filter, sort, page, pageSize, userId));
    }

    // 获取问题详情（同时累加浏览量）
    @GetMapping("/questions/{id}")
    public Result<?> getQuestionById(@PathVariable String id) {
        Long userId = currentUserIdOrNull();
        return Result.success(seniorService.getQuestionById(id, userId));
    }

    // 发布问题（绑定真实用户，异步触发学长回答）
    @PostMapping("/questions")
    public Result<?> publishQuestion(@RequestBody Map<String, Object> data) {
        CurrentUserProvider.CurrentUser user = currentUserProvider.require();
        return Result.success(seniorService.publishQuestion(data, user.id(), user.displayName()));
    }

    // 点赞 / 取消点赞回答（按用户维度）
    @PostMapping("/answers/{answerId}/like")
    public Result<?> likeAnswer(@PathVariable String answerId) {
        CurrentUserProvider.CurrentUser user = currentUserProvider.require();
        return Result.success(seniorService.likeAnswer(answerId, user.id()));
    }

    // 采纳回答（仅提问者本人）
    @PutMapping("/questions/{questionId}/accept")
    public Result<?> acceptAnswer(@PathVariable String questionId, @RequestBody Map<String, String> data) {
        CurrentUserProvider.CurrentUser user = currentUserProvider.require();
        seniorService.acceptAnswer(questionId, data.get("answerId"), user.id());
        return Result.success();
    }

    // 提交追问（绑定真实用户，异步触发学长回复）
    @PostMapping("/questions/{questionId}/follow-up")
    public Result<?> submitFollowUp(@PathVariable String questionId, @RequestBody Map<String, String> data) {
        CurrentUserProvider.CurrentUser user = currentUserProvider.require();
        return Result.success(
                seniorService.submitFollowUp(questionId, data.get("content"), user.id(), user.displayName()));
    }

    // AI 生成失败后重试（仅提问者本人）
    @PostMapping("/questions/{questionId}/regenerate")
    public Result<?> regenerateAnswer(@PathVariable String questionId) {
        CurrentUserProvider.CurrentUser user = currentUserProvider.require();
        return Result.success(seniorService.regenerateAnswer(questionId, user.id()));
    }

    /**
     * 列表/详情允许未登录浏览，因此取不到用户时返回 null（点赞状态按未点赞处理）。
     */
    private Long currentUserIdOrNull() {
        CurrentUserProvider.CurrentUser user = currentUserProvider.find();
        return user == null ? null : user.id();
    }
}
