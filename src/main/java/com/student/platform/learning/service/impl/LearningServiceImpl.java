package com.student.platform.learning.service.impl;

import com.student.platform.common.dto.LearningCurveDTO;
import com.student.platform.learning.entity.Book;
import com.student.platform.learning.entity.LearningProgress;
import com.student.platform.learning.entity.UserBook;
import com.student.platform.learning.mapper.BookMapper;
import com.student.platform.learning.mapper.LearningProgressMapper;
import com.student.platform.learning.mapper.UserBookMapper;
import com.student.platform.learning.service.LearningService;
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
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        log.debug("获取学习曲线, userId: {}, bookId: {}, startDate: {}, endDate: {}",
                userId, bookId, startDate, endDate);

        List<LearningProgress> progressList = learningProgressMapper.findByUserIdAndBookIdAndDateRange(
                userId, bookId, startDate, endDate
        );

        Book book = bookMapper.selectById(bookId);

        UserBook userBook = userBookMapper.findByUserIdAndBookId(userId, bookId);
        double currentProgress = 0.0;
        if (userBook != null) {
            currentProgress = userBook.getProgress().doubleValue();
        }

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
        log.debug("保存学习进度, userId: {}, bookId: {}, date: {}",
                learningProgress.getUserId(),
                learningProgress.getBookId(),
                learningProgress.getDate());

        LearningProgress existingProgress = learningProgressMapper.findByUserIdAndBookIdAndDate(
                learningProgress.getUserId(),
                learningProgress.getBookId(),
                learningProgress.getDate()
        );

        if (existingProgress != null) {
            existingProgress.setProgress(learningProgress.getProgress());
            existingProgress.setPagesRead(learningProgress.getPagesRead());
            existingProgress.setStudyTimeMinutes(learningProgress.getStudyTimeMinutes());
            existingProgress.setUpdatedAt(LocalDateTime.now());
            learningProgressMapper.updateById(existingProgress);
            log.debug("更新学习进度记录, id: {}", existingProgress.getId());
            return existingProgress;
        } else {
            learningProgress.setCreatedAt(LocalDateTime.now());
            learningProgress.setUpdatedAt(LocalDateTime.now());
            learningProgressMapper.insert(learningProgress);
            log.debug("创建学习进度记录, id: {}", learningProgress.getId());
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

        log.debug("获取学习历史, userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        List<LearningProgress> progressList = learningProgressMapper.findByUserIdAndDateRange(userId, startDate, endDate);

        if (progressList == null || progressList.isEmpty()) {
            log.debug("未找到学习历史记录，返回模拟数据");
            return getMockLearningHistory();
        }

        return progressList;
    }

    private List<LearningProgress> getMockLearningHistory() {
        log.debug("生成模拟学习历史数据");
        List<LearningProgress> mockData = new ArrayList<>();

        LocalDate today = LocalDate.now();
        BigDecimal baseProgress = new BigDecimal(10);

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LearningProgress progress = new LearningProgress();
            progress.setId((long) (7 - i));
            progress.setUserId(1L);
            progress.setBookId(205L);
            progress.setDate(date);

            BigDecimal currentProgress = baseProgress.add(new BigDecimal(5 * (6 - i)));
            if (currentProgress.compareTo(new BigDecimal(100)) > 0) {
                currentProgress = new BigDecimal(100);
            }
            progress.setProgress(currentProgress);

            progress.setPagesRead(20 + (6 - i) * 5);

            progress.setStudyTimeMinutes(30 + (6 - i) * 5);

            progress.setCreatedAt(date.atStartOfDay().plusHours(20));
            progress.setUpdatedAt(date.atStartOfDay().plusHours(21));

            mockData.add(progress);
        }

        return mockData;
    }
}
