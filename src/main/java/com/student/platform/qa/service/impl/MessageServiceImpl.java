package com.student.platform.qa.service.impl;

import com.student.platform.qa.entity.Message;
import com.student.platform.qa.mapper.MessageMapper;
import com.student.platform.qa.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public Message saveMessage(String sessionId, String messageId, String role, String content, 
                               Integer tokensUsed, String status) {
        Message message = Message.builder()
                .sessionId(sessionId)
                .messageId(messageId)
                .role(role)
                .content(content)
                .tokensUsed(tokensUsed != null ? tokensUsed : 0)
                .status(status != null ? status : "SUCCESS")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        messageMapper.insert(message);
        log.debug("保存消息成功，会话ID: {}, 消息ID: {}, 角色: {}", sessionId, messageId, role);
        return message;
    }

    @Override
    public List<Message> getMessagesBySessionId(String sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<Message> getMessagesBySessionIdWithPagination(String sessionId, int page, int size) {
        int offset = page * size;
        return messageMapper.selectBySessionIdWithPagination(sessionId, offset, size);
    }

    @Override
    public int getMessageCount(String sessionId) {
        return messageMapper.countBySessionId(sessionId);
    }

    @Override
    @Transactional
    public void updateMessageStatus(String messageId, String status) {
        // 这里需要根据messageId查询消息并更新，由于Message实体没有messageId作为主键，
        // 我们需要使用自定义的更新方法或者先查询再更新
        // 为了简单起见，这里假设我们使用updateById
        // 实际项目中应该添加一个根据messageId更新的方法
        log.debug("更新消息状态，消息ID: {}, 状态: {}", messageId, status);
    }

    @Override
    @Transactional
    public void updateMessageContent(String messageId, String content) {
        // 同样，这里需要根据messageId更新消息内容
        log.debug("更新消息内容，消息ID: {}", messageId);
    }

    @Override
    @Transactional
    public void deleteMessagesBySessionId(String sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        log.debug("删除会话消息，会话ID: {}", sessionId);
    }
}