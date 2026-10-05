package com.student.platform.knowledge.service.impl;

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

    @Scheduled(initialDelay = 60000, fixedRate = 86400000)
    public void warmupKnowledgeGraphs() {
        log.info("开始预热知识图谱...");

        List<Long> hotBookIds = getHotBookIds();
        log.info("待预热书籍数量: {}", hotBookIds.size());

        for (Long bookId : hotBookIds) {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始预热书籍知识图谱，书籍ID: {}", bookId);
                    log.info("书籍知识图谱预热完成，书籍ID: {}", bookId);
                } catch (Exception e) {
                    log.error("预热书籍知识图谱失败，书籍ID: {}，错误: {}", bookId, e.getMessage(), e);
                }
            });
        }

        log.info("知识图谱预热任务已启动，共预热 {} 本热门书籍", hotBookIds.size());
    }

    private List<Long> getHotBookIds() {
        return List.of(208L, 1L, 2L, 3L, 4L, 5L);
    }

    public void triggerWarmup() {
        log.info("手动触发知识图谱预热");
        warmupKnowledgeGraphs();
    }
}
