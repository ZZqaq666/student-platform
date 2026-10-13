package com.student.platform.qa.controller;

import com.student.platform.common.config.CurrentUserProvider;
import com.student.platform.qa.entity.Conversation;
import com.student.platform.qa.entity.Message;
import com.student.platform.qa.service.ConversationService;
import com.student.platform.qa.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 */
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "会话管理", description = "历史对话会话管理相关接口")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 获取用户的所有会话列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有活跃会话，按更新时间倒序排列")
    public Map<String, Object> getConversationList() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return buildErrorResponse(401, "用户未登录");
        }

        List<Conversation> conversations = conversationService.getUserConversations(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", conversations);
        return response;
    }

    /**
     * 获取单个会话的详细信息和消息列表
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息和所有消息")
    public Map<String, Object> getConversationDetail(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return buildErrorResponse(401, "用户未登录");
        }

        // 权限验证
        if (!conversationService.isConversationOwner(sessionId, userId)) {
            return buildErrorResponse(403, "无权限访问该会话");
        }

        Conversation conversation = conversationService.getConversationBySessionId(sessionId);
        List<Message> messages = messageService.getMessagesBySessionId(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("conversation", conversation);
        data.put("messages", messages);
        response.put("data", data);
        
        return response;
    }

    /**
     * 获取会话的消息列表（分页）
     */
    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "获取会话消息", description = "分页获取指定会话的消息列表")
    public Map<String, Object> getConversationMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return buildErrorResponse(401, "用户未登录");
        }

        // 权限验证
        if (!conversationService.isConversationOwner(sessionId, userId)) {
            return buildErrorResponse(403, "无权限访问该会话");
        }

        List<Message> messages = messageService.getMessagesBySessionIdWithPagination(sessionId, page, size);
        int total = messageService.getMessageCount(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        response.put("data", data);
        
        return response;
    }

    /**
     * 创建新会话
     */
    @PostMapping("/create")
    @Operation(summary = "创建会话", description = "创建新会话，需要提供第一条消息内容")
    public Map<String, Object> createConversation(@RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return buildErrorResponse(401, "用户未登录");
        }

        String firstMessage = request.get("firstMessage");
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return buildErrorResponse(400, "第一条消息不能为空");
        }

        Conversation conversation = conversationService.createConversation(userId, firstMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", conversation);
        return response;
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除会话", description = "逻辑删除指定会话，同时删除该会话下的所有消息")
    public Map<String, Object> deleteConversation(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return buildErrorResponse(401, "用户未登录");
        }

        conversationService.deleteConversation(sessionId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        response.put("data", null);
        return response;
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        CurrentUserProvider.CurrentUser currentUser = currentUserProvider.find();
        return currentUser != null ? currentUser.id() : null;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}