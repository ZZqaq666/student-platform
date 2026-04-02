package com.student.platform.service.impl;

import com.student.platform.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphWarmupService {

    private final AiService aiService;

    /**
     * 预热热门书籍的知识图谱
     * 在应用启动时执行一次，之后每天凌晨2点执行
     */
    @Scheduled(initialDelay = 60000, fixedRate = 86400000)
    public void warmupKnowledgeGraphs() {
        log.info("开始预热知识图谱...");
        
        // 获取热门书籍ID列表（这里使用示例数据，实际应该从数据库查询）
        List<Long> hotBookIds = getHotBookIds();
        
        for (Long bookId : hotBookIds) {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("预热书籍知识图谱，书籍ID: {}", bookId);
                    aiService.generateKnowledgeGraph(bookId);
                    log.info("书籍知识图谱预热完成，书籍ID: {}", bookId);
                } catch (Exception e) {
                    log.error("预热书籍知识图谱失败，书籍ID: {}，错误: {}", bookId, e.getMessage(), e);
                }
            });
        }
        
        log.info("知识图谱预热任务已启动，共预热 {} 本热门书籍", hotBookIds.size());
    }

    /**
     * 获取热门书籍ID列表
     * 实际应用中应该从数据库查询，这里使用示例数据
     */
    private List<Long> getHotBookIds() {
        // 示例热门书籍ID，实际应该从数据库查询
        return List.of(208L, 1L, 2L, 3L, 4L, 5L);
    }

    /**
     * 手动触发预热
     */
    public void triggerWarmup() {
        warmupKnowledgeGraphs();
    }
}
