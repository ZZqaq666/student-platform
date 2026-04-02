package com.student.platform.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 错误处理器
 * 负责统一的错误处理和错误响应格式化
 */
@Slf4j
@Component
public class ErrorHandler {

    /**
     * 处理参数错误
     * @param message 错误消息
     * @param e 异常对象
     * @return 格式化的错误消息
     */
    public String handleParameterError(String message, IllegalArgumentException e) {
        log.error("参数错误：{}", message, e);
        return "参数错误：" + message;
    }

    /**
     * 处理运行时错误
     * @param message 错误消息
     * @param e 异常对象
     * @return 格式化的错误消息
     */
    public String handleRuntimeError(String message, RuntimeException e) {
        log.error("运行时错误：{}", message, e);
        return "系统运行错误，请稍后重试。";
    }

    /**
     * 处理通用错误
     * @param message 错误消息
     * @param e 异常对象
     * @return 格式化的错误消息
     */
    public String handleGeneralError(String message, Exception e) {
        log.error("处理错误：{}", message, e);
        return "抱歉，我无法处理您的请求，请稍后重试。";
    }

    /**
     * 处理图像相关错误
     * @param message 错误消息
     * @param e 异常对象
     * @return 格式化的错误消息
     */
    public String handleImageError(String message, Exception e) {
        log.error("图像处理错误：{}", message, e);
        return "图像处理失败：" + message;
    }

    /**
     * 处理书籍推荐错误
     * @param message 错误消息
     * @param e 异常对象
     * @return null，表示需要返回空对象
     */
    public Object handleBookRecommendationError(String message, Exception e) {
        log.error("书籍推荐错误：{}", message, e);
        return null;
    }

    /**
     * 处理知识图谱错误
     * @param message 错误消息
     * @param e 异常对象
     * @return null，表示需要返回空对象
     */
    public Object handleKnowledgeGraphError(String message, Exception e) {
        log.error("知识图谱生成错误：{}", message, e);
        return null;
    }
}