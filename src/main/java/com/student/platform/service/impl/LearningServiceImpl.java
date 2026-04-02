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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        
        List<LearningProgress> progressList = learningProgressMapper.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // 如果没有数据，返回模拟数据
        if (progressList == null || progressList.isEmpty()) {
            return getMockLearningHistory();
        }
        
        return progressList;
    }
    
    /**
     * 获取模拟的学习历史数据
     *
     * @return 模拟的学习历史数据
     */
    private List<LearningProgress> getMockLearningHistory() {
        log.debug("返回模拟的学习历史数据");
        List<LearningProgress> mockData = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        BigDecimal baseProgress = new BigDecimal(10);
        
        // 生成最近7天的模拟数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LearningProgress progress = new LearningProgress();
            progress.setId((long) (7 - i));
            progress.setUserId(1L); // 模拟用户ID
            progress.setBookId(205L); // 高等数学（上册）
            progress.setDate(date);
            
            // 每天增加5%的进度
            BigDecimal currentProgress = baseProgress.add(new BigDecimal(5 * (6 - i)));
            if (currentProgress.compareTo(new BigDecimal(100)) > 0) {
                currentProgress = new BigDecimal(100);
            }
            progress.setProgress(currentProgress);
            
            // 模拟每天阅读的页数
            progress.setPagesRead(20 + (6 - i) * 5);
            
            // 模拟每天学习时间（分钟）
            progress.setStudyTimeMinutes(30 + (6 - i) * 5);
            
            progress.setCreatedAt(date.atStartOfDay().plusHours(20));
            progress.setUpdatedAt(date.atStartOfDay().plusHours(21));
            
            mockData.add(progress);
        }
        
        return mockData;
    }
}
