package com.student.platform.service.impl;

import com.student.platform.dto.LearningCurveDTO;
import com.student.platform.entity.Book;
import com.student.platform.entity.LearningProgress;
import com.student.platform.entity.UserBook;
import com.student.platform.mapper.BookMapper;
import com.student.platform.mapper.LearningProgressMapper;
import com.student.platform.mapper.UserBookMapper;
import com.student.platform.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {
    
    private final LearningProgressMapper learningProgressMapper;
    private final UserBookMapper userBookMapper;
    private final BookMapper bookMapper;
    
    @Override
    public LearningCurveDTO getLearningCurve(Long userId, Long bookId, LocalDate startDate, LocalDate endDate) {
        // 如果未指定日期范围，默认查询最近30天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // 查询学习进度历史
        List<LearningProgress> progressList = learningProgressMapper.findByUserIdAndBookIdAndDateRange(
                userId, bookId, startDate, endDate
        );
        
        // 查询当前书籍信息
        Book book = bookMapper.selectById(bookId);
        
        // 查询当前学习进度
        UserBook userBook = userBookMapper.findByUserIdAndBookId(userId, bookId);
        double currentProgress = 0.0;
        if (userBook != null) {
            currentProgress = userBook.getProgress().doubleValue();
        }
        
        // 构建学习曲线数据
        List<LearningCurveDTO.LearningDataPoint> dataPoints = new ArrayList<>();
        for (LearningProgress progress : progressList) {
            LearningCurveDTO.LearningDataPoint point = new LearningCurveDTO.LearningDataPoint(
                    progress.getDate().toString(),
                    progress.getProgress().doubleValue(),
                    progress.getPagesRead(),
                    progress.getStudyTimeMinutes()
            );
            dataPoints.add(point);
        }
        
        // 创建并返回DTO
        LearningCurveDTO dto = new LearningCurveDTO();
        dto.setLearningCurve(dataPoints);
        dto.setCurrentProgress(currentProgress);
        if (book != null) {
            dto.setBookTitle(book.getTitle());
            dto.setBookAuthor(book.getAuthor());
        }
        
        return dto;
    }
    
    @Override
    public LearningProgress saveLearningProgress(LearningProgress learningProgress) {
        // 检查是否已存在当天的记录
        LearningProgress existingProgress = learningProgressMapper.findByUserIdAndBookIdAndDate(
                learningProgress.getUserId(),
                learningProgress.getBookId(),
                learningProgress.getDate()
        );
        
        if (existingProgress != null) {
            // 更新现有记录
            existingProgress.setProgress(learningProgress.getProgress());
            existingProgress.setPagesRead(learningProgress.getPagesRead());
            existingProgress.setStudyTimeMinutes(learningProgress.getStudyTimeMinutes());
            learningProgressMapper.updateById(existingProgress);
            return existingProgress;
        } else {
            // 创建新记录
            learningProgressMapper.insert(learningProgress);
            return learningProgress;
        }
    }
    
    @Override
    public List<LearningProgress> getLearningHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        return learningProgressMapper.findByUserIdAndDateRange(userId, startDate, endDate);
    }
}
