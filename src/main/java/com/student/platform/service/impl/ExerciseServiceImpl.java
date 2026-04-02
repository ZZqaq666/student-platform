package com.student.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.dto.*;
import com.student.platform.entity.*;
import com.student.platform.exception.BusinessException;
import com.student.platform.mapper.*;
import com.student.platform.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 练习服务实现类
 * <p>
 * 提供练习题管理、答题记录、错题本等功能
 * 使用Spring Cache优化查询性能
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseMapper exerciseMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongBookMapper wrongBookMapper;
    private final UserMapper userMapper;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String QUESTION_TYPE_SINGLE_CHOICE = "SINGLE_CHOICE";
    private static final String QUESTION_TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    private static final String MASTER_STATUS_MASTERED = "MASTERED";
    private static final String MASTER_STATUS_MASTERING = "MASTERING";
    private static final String MASTER_STATUS_NOT_MASTERED = "NOT_MASTERED";
    private static final int MASTER_THRESHOLD = 3; // 掌握阈值：连续答对3次

    /**
     * 根据书籍ID获取练习题列表
     *
     * @param bookId 书籍ID
     * @return 练习题列表
     */
    @Override
    @Cacheable(value = "exercises", key = "'book:' + #bookId")
    public List<ExerciseDTO> getExercisesByBook(Long bookId) {
        log.debug("查询书籍练习题, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        List<Exercise> exercises = exerciseMapper.findByBookId(bookId);
        if (CollectionUtils.isEmpty(exercises)) {
            return Collections.emptyList();
        }

        return exercises.stream()
                .map(this::convertToExerciseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据知识节点ID获取练习题列表
     *
     * @param knowledgeNodeId 知识节点ID
     * @return 练习题列表
     */
    @Override
    @Cacheable(value = "exercises", key = "'knowledge:' + #knowledgeNodeId")
    public List<ExerciseDTO> getExercisesByKnowledgeNode(Long knowledgeNodeId) {
        log.debug("查询知识节点练习题, knowledgeNodeId: {}", knowledgeNodeId);
        if (knowledgeNodeId == null || knowledgeNodeId <= 0) {
            throw new BusinessException(400, "知识节点ID无效");
        }

        List<Exercise> exercises = exerciseMapper.findByKnowledgeNodeId(knowledgeNodeId);
        if (CollectionUtils.isEmpty(exercises)) {
            return Collections.emptyList();
        }

        return exercises.stream()
                .map(this::convertToExerciseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取练习题详情
     *
     * @param id 练习题ID
     * @return 练习题DTO
     * @throws BusinessException 如果练习题不存在
     */
    @Override
    @Cacheable(value = "exercises", key = "#id")
    public ExerciseDTO getExerciseById(Long id) {
        log.debug("查询练习题详情, id: {}", id);
        if (id == null || id <= 0) {
            throw new BusinessException(400, "练习题ID无效");
        }

        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw new BusinessException(404, "练习题不存在");
        }

        return convertToExerciseDTO(exercise);
    }

    /**
     * 提交答案
     *
     * @param request 答题请求
     * @return 答题记录DTO
     * @throws BusinessException 如果参数无效或题目不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"answerRecords", "wrongBooks", "exerciseStats"}, allEntries = true)
    public AnswerRecordDTO submitAnswer(SubmitAnswerRequest request) {
        log.info("提交答案, request: {}", request);
        if (request == null) {
            throw new BusinessException(400, "答题请求不能为空");
        }
        if (request.getExerciseId() == null || request.getExerciseId() <= 0) {
            throw new BusinessException(400, "练习题ID无效");
        }

        User user = getCurrentUser();
        Exercise exercise = exerciseMapper.selectById(request.getExerciseId());
        if (exercise == null) {
            throw new BusinessException(404, "练习题不存在");
        }

        // 检查答案
        boolean isCorrect = checkAnswer(exercise, request.getUserAnswer());
        int scoreObtained = isCorrect ? exercise.getScore() : 0;

        // 计算尝试次数
        int attemptCount = calculateAttemptCount(user.getId(), exercise.getId());

        // 创建答题记录
        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setUserId(user.getId());
        answerRecord.setExerciseId(exercise.getId());
        answerRecord.setUserAnswer(request.getUserAnswer());
        answerRecord.setIsCorrect(isCorrect);
        answerRecord.setScoreObtained(scoreObtained);
        answerRecord.setTimeSpent(request.getTimeSpent());
        answerRecord.setAttemptCount(attemptCount);
        answerRecord.setCreatedAt(LocalDateTime.now());
        answerRecord.setUpdatedAt(LocalDateTime.now());

        answerRecordMapper.insert(answerRecord);

        // 更新错题本
        updateWrongBook(user, exercise, answerRecord, isCorrect);

        log.info("答题完成, userId: {}, exerciseId: {}, isCorrect: {}",
                user.getId(), exercise.getId(), isCorrect);

        return convertToAnswerRecordDTO(answerRecord);
    }

    /**
     * 获取答题历史记录
     *
     * @return 答题记录列表
     */
    @Override
    @Cacheable(value = "answerRecords", key = "'user:' + @securityService.getCurrentUserId()")
    public List<AnswerRecordDTO> getAnswerHistory() {
        log.debug("获取答题历史记录");
        User user = getCurrentUser();

        List<AnswerRecord> records = answerRecordMapper.findByUserId(user.getId());
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(this::convertToAnswerRecordDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取错题本列表
     *
     * @return 错题本列表
     */
    @Override
    @Cacheable(value = "wrongBooks", key = "'user:' + @securityService.getCurrentUserId()")
    public List<WrongBookDTO> getWrongBook() {
        log.debug("获取错题本列表");
        User user = getCurrentUser();

        List<WrongBook> wrongBooks = wrongBookMapper.findByUserId(user.getId());
        if (CollectionUtils.isEmpty(wrongBooks)) {
            return Collections.emptyList();
        }

        return wrongBooks.stream()
                .map(this::convertToWrongBookDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取练习统计信息
     *
     * @return 练习统计DTO
     */
    @Override
    @Cacheable(value = "exerciseStats", key = "'user:' + @securityService.getCurrentUserId()")
    public ExerciseStatsDTO getExerciseStats() {
        log.debug("获取练习统计信息");
        User user = getCurrentUser();

        // 获取总题目数
        QueryWrapper<Exercise> exerciseQuery = new QueryWrapper<>();
        exerciseQuery.eq("status", STATUS_ACTIVE);
        long totalExercises = exerciseMapper.selectCount(exerciseQuery);

        // 获取答题记录统计
        QueryWrapper<AnswerRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", user.getId());
        long totalAttempts = answerRecordMapper.selectCount(recordQuery);

        // 获取正确答题数
        QueryWrapper<AnswerRecord> correctQuery = new QueryWrapper<>();
        correctQuery.eq("user_id", user.getId());
        correctQuery.eq("is_correct", true);
        long correctCount = answerRecordMapper.selectCount(correctQuery);

        long wrongCount = totalAttempts - correctCount;
        double accuracyRate = totalAttempts > 0 ? (double) correctCount / totalAttempts * 100 : 0;

        // 计算总分
        List<AnswerRecord> allRecords = answerRecordMapper.findByUserId(user.getId());
        int totalScore = 0;
        int obtainedScore = 0;

        if (!CollectionUtils.isEmpty(allRecords)) {
            for (AnswerRecord record : allRecords) {
                Exercise ex = exerciseMapper.selectById(record.getExerciseId());
                if (ex != null) {
                    totalScore += ex.getScore();
                }
                obtainedScore += record.getScoreObtained();
            }
        }

        return new ExerciseStatsDTO(
                totalExercises,
                totalAttempts,
                correctCount,
                wrongCount,
                Math.round(accuracyRate * 100.0) / 100.0, // 保留两位小数
                totalScore,
                obtainedScore
        );
    }

    /**
     * 更新错题本笔记
     *
     * @param wrongBookId 错题本ID
     * @param notes       笔记内容
     * @return 更新后的错题本DTO
     * @throws BusinessException 如果错题不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public WrongBookDTO updateWrongBookNotes(Long wrongBookId, String notes) {
        log.info("更新错题本笔记, wrongBookId: {}", wrongBookId);
        if (wrongBookId == null || wrongBookId <= 0) {
            throw new BusinessException(400, "错题本ID无效");
        }

        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(404, "错题不存在");
        }

        // 验证权限
        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        wrongBook.setNotes(notes);
        wrongBook.setUpdatedAt(LocalDateTime.now());
        wrongBookMapper.updateById(wrongBook);

        return convertToWrongBookDTO(wrongBook);
    }

    /**
     * 从错题本移除
     *
     * @param wrongBookId 错题本ID
     * @throws BusinessException 如果错题不存在或无权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public void removeFromWrongBook(Long wrongBookId) {
        log.info("从错题本移除, wrongBookId: {}", wrongBookId);
        if (wrongBookId == null || wrongBookId <= 0) {
            throw new BusinessException(400, "错题本ID无效");
        }

        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(404, "错题不存在");
        }

        // 验证权限
        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        wrongBookMapper.deleteById(wrongBookId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 计算尝试次数
     *
     * @param userId     用户ID
     * @param exerciseId 练习题ID
     * @return 尝试次数
     */
    private int calculateAttemptCount(Long userId, Long exerciseId) {
        QueryWrapper<AnswerRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("exercise_id", exerciseId);
        long count = answerRecordMapper.selectCount(queryWrapper);
        return (int) count + 1;
    }

    /**
     * 检查答案是否正确
     *
     * @param exercise   练习题
     * @param userAnswer 用户答案
     * @return 是否正确
     */
    private boolean checkAnswer(Exercise exercise, String userAnswer) {
        if (exercise == null || !StringUtils.hasText(exercise.getAnswer())) {
            return false;
        }
        if (!StringUtils.hasText(userAnswer)) {
            return false;
        }

        String correctAnswer = exercise.getAnswer().trim();
        String answer = userAnswer.trim();

        // 选择题不区分大小写，其他题型区分
        if (QUESTION_TYPE_SINGLE_CHOICE.equals(exercise.getQuestionType()) ||
                QUESTION_TYPE_MULTIPLE_CHOICE.equals(exercise.getQuestionType())) {
            return correctAnswer.equalsIgnoreCase(answer);
        } else {
            return correctAnswer.equals(answer);
        }
    }

    /**
     * 更新错题本
     *
     * @param user          用户
     * @param exercise      练习题
     * @param answerRecord  答题记录
     * @param isCorrect     是否正确
     */
    private void updateWrongBook(User user, Exercise exercise, AnswerRecord answerRecord, boolean isCorrect) {
        WrongBook existingWrongBook = wrongBookMapper.findByUserIdAndExerciseId(user.getId(), exercise.getId());

        if (existingWrongBook != null) {
            // 更新现有错题记录
            WrongBook wrongBook = existingWrongBook;
            if (isCorrect) {
                wrongBook.setCorrectCount(wrongBook.getCorrectCount() + 1);
                if (wrongBook.getCorrectCount() >= MASTER_THRESHOLD) {
                    wrongBook.setMasterStatus(MASTER_STATUS_MASTERED);
                } else if (wrongBook.getCorrectCount() >= 1) {
                    wrongBook.setMasterStatus(MASTER_STATUS_MASTERING);
                }
            } else {
                wrongBook.setWrongCount(wrongBook.getWrongCount() + 1);
                wrongBook.setMasterStatus(MASTER_STATUS_NOT_MASTERED);
            }
            wrongBook.setLastAttemptAt(LocalDateTime.now());
            wrongBook.setAnswerRecordId(answerRecord.getId());
            wrongBook.setUpdatedAt(LocalDateTime.now());
            wrongBookMapper.updateById(wrongBook);
        } else if (!isCorrect) {
            // 创建新的错题记录
            WrongBook wrongBook = new WrongBook();
            wrongBook.setUserId(user.getId());
            wrongBook.setExerciseId(exercise.getId());
            wrongBook.setAnswerRecordId(answerRecord.getId());
            wrongBook.setWrongCount(1);
            wrongBook.setCorrectCount(0);
            wrongBook.setLastAttemptAt(LocalDateTime.now());
            wrongBook.setMasterStatus(MASTER_STATUS_NOT_MASTERED);
            wrongBook.setCreatedAt(LocalDateTime.now());
            wrongBook.setUpdatedAt(LocalDateTime.now());
            wrongBookMapper.insert(wrongBook);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @return 用户实体
     * @throws BusinessException 如果用户未登录或不存在
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "用户未登录");
        }

        String username = authentication.getName();
        if (!StringUtils.hasText(username) || "anonymousUser".equals(username)) {
            throw new BusinessException(401, "用户未登录");
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        return user;
    }

    /**
     * 将Exercise实体转换为ExerciseDTO
     *
     * @param exercise 练习题实体
     * @return 练习题DTO
     */
    private ExerciseDTO convertToExerciseDTO(Exercise exercise) {
        if (exercise == null) {
            return null;
        }

        return new ExerciseDTO(
                exercise.getId(),
                exercise.getBookId(),
                exercise.getKnowledgeNodeId(),
                exercise.getTitle(),
                exercise.getContent(),
                exercise.getQuestionType(),
                exercise.getOptions(),
                exercise.getAnswer(),
                exercise.getAnalysis(),
                exercise.getDifficulty(),
                exercise.getScore(),
                exercise.getStatus(),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt()
        );
    }

    /**
     * 将AnswerRecord实体转换为AnswerRecordDTO
     *
     * @param record 答题记录实体
     * @return 答题记录DTO
     */
    private AnswerRecordDTO convertToAnswerRecordDTO(AnswerRecord record) {
        if (record == null) {
            return null;
        }

        return new AnswerRecordDTO(
                record.getId(),
                record.getUserId(),
                record.getExerciseId(),
                record.getUserAnswer(),
                record.getIsCorrect(),
                record.getScoreObtained(),
                record.getTimeSpent(),
                record.getAttemptCount(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    /**
     * 将WrongBook实体转换为WrongBookDTO
     *
     * @param wrongBook 错题本实体
     * @return 错题本DTO
     */
    private WrongBookDTO convertToWrongBookDTO(WrongBook wrongBook) {
        if (wrongBook == null) {
            return null;
        }

        Exercise exercise = null;
        if (wrongBook.getExerciseId() != null) {
            exercise = exerciseMapper.selectById(wrongBook.getExerciseId());
        }

        return new WrongBookDTO(
                wrongBook.getId(),
                wrongBook.getUserId(),
                exercise != null ? convertToExerciseDTO(exercise) : null,
                wrongBook.getAnswerRecordId(),
                wrongBook.getWrongCount(),
                wrongBook.getCorrectCount(),
                wrongBook.getLastAttemptAt(),
                wrongBook.getMasterStatus(),
                wrongBook.getNotes(),
                wrongBook.getTags(),
                wrongBook.getCreatedAt(),
                wrongBook.getUpdatedAt()
        );
    }
}
