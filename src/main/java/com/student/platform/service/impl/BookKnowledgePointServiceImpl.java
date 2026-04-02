package com.student.platform.service.impl;

import com.student.platform.entity.BookKnowledgePoint;
import com.student.platform.mapper.BookKnowledgePointMapper;
import com.student.platform.service.BookKnowledgePointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookKnowledgePointServiceImpl implements BookKnowledgePointService {

    private final BookKnowledgePointMapper bookKnowledgePointMapper;

    @Override
    public List<String> getRelatedKnowledgePoints(Long bookId) {
        log.debug("获取书籍关联知识点, bookId: {}", bookId);
        try {
            List<BookKnowledgePoint> points = bookKnowledgePointMapper.findByBookIdAndType(bookId, "RELATED");
            List<String> result = new ArrayList<>();
            for (BookKnowledgePoint point : points) {
                result.add(point.getContent());
            }
            log.info("返回关联知识点: {}, 数量: {}", result, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取书籍关联知识点异常: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getHighFrequencyKnowledgePoints(Long bookId) {
        log.debug("获取书籍高频知识点, bookId: {}", bookId);
        try {
            List<BookKnowledgePoint> points = bookKnowledgePointMapper.findByBookIdAndType(bookId, "HIGH_FREQUENCY");
            List<String> result = new ArrayList<>();
            for (BookKnowledgePoint point : points) {
                result.add(point.getContent());
            }
            log.info("返回高频知识点: {}, 数量: {}", result, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取书籍高频知识点异常: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveKnowledgePoints(Long bookId, List<String> relatedPoints, List<String> highFrequencyPoints) {
        log.debug("保存书籍知识点, bookId: {}", bookId);
        
        // 先删除旧的知识点
        List<BookKnowledgePoint> oldPoints = bookKnowledgePointMapper.findByBookId(bookId);
        for (BookKnowledgePoint point : oldPoints) {
            bookKnowledgePointMapper.deleteById(point.getId());
        }
        
        // 保存新的关联知识点
        for (String content : relatedPoints) {
            BookKnowledgePoint point = new BookKnowledgePoint();
            point.setBookId(bookId);
            point.setType("RELATED");
            point.setContent(content);
            point.setCreatedAt(LocalDateTime.now());
            point.setUpdatedAt(LocalDateTime.now());
            bookKnowledgePointMapper.insert(point);
        }
        
        // 保存新的高频知识点
        for (String content : highFrequencyPoints) {
            BookKnowledgePoint point = new BookKnowledgePoint();
            point.setBookId(bookId);
            point.setType("HIGH_FREQUENCY");
            point.setContent(content);
            point.setCreatedAt(LocalDateTime.now());
            point.setUpdatedAt(LocalDateTime.now());
            bookKnowledgePointMapper.insert(point);
        }
        
        log.info("保存书籍知识点完成, bookId: {}, 关联知识点数量: {}, 高频知识点数量: {}", 
                bookId, relatedPoints.size(), highFrequencyPoints.size());
    }
}
