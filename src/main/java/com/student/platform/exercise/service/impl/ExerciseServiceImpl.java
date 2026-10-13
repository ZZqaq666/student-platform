package com.student.platform.exercise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.common.dto.*;
import com.student.platform.common.entity.User;
import com.student.platform.common.exception.BusinessException;
import com.student.platform.exercise.entity.*;
import com.student.platform.exercise.mapper.*;
import com.student.platform.exercise.service.ExerciseService;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * 错题列表可排序列白名单（键为外部可传值，值为真实数据库列名）。
     * 前端 sortBy 只能命中白名单内的键，命中不了则回落 created_at，
     * 从而杜绝把任意字符串拼进 SQL ORDER BY 造成注入。
     */
    private static final Map<String, String> WRONG_BOOK_SORT_COLUMNS = Map.ofEntries(
            Map.entry("created_at", "created_at"),
            Map.entry("updated_at", "updated_at"),
            Map.entry("last_attempt_at", "last_attempt_at"),
            Map.entry("next_review_at", "next_review_at"),
            Map.entry("wrong_count", "wrong_count"),
            Map.entry("correct_count", "correct_count"),
            Map.entry("review_count", "review_count"),
            Map.entry("priority", "priority"),
            Map.entry("difficulty", "difficulty"),
            // 兼容前端驼峰写法
            Map.entry("createdAt", "created_at"),
            Map.entry("updatedAt", "updated_at"),
            Map.entry("lastAttemptAt", "last_attempt_at"),
            Map.entry("nextReviewAt", "next_review_at"),
            Map.entry("wrongCount", "wrong_count"),
            Map.entry("correctCount", "correct_count"),
            Map.entry("reviewCount", "review_count")
    );

    /**
     * 单页最大条数，防止 pageSize 被放大后一次性拖垮数据库。
     * 注意：错题列表目前由前端「全量拉取 + 本地切片」渲染（见 WrongBook.vue），
     * 这里取 1000 兼顾防护与兼容；后续若改为服务端真分页，可下调并与前端对齐。
     */
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String MASTER_STATUS_MASTERING = "MASTERING";
    private static final String MASTER_STATUS_NOT_MASTERED = "NOT_MASTERED";
    private static final int MASTER_THRESHOLD = 3;

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

        boolean isCorrect = checkAnswer(exercise, request.getUserAnswer());
        int scoreObtained = isCorrect ? exercise.getScore() : 0;

        int attemptCount = calculateAttemptCount(user.getId(), exercise.getId());

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

        updateWrongBook(user, exercise, answerRecord, isCorrect);

        log.info("答题完成, userId: {}, exerciseId: {}, isCorrect: {}",
                user.getId(), exercise.getId(), isCorrect);

        return convertToAnswerRecordDTO(answerRecord);
    }

    @Override
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

    @Override
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

    @Override
    public ExerciseStatsDTO getExerciseStats() {
        log.debug("获取练习统计信息");
        User user = getCurrentUser();

        QueryWrapper<Exercise> exerciseQuery = new QueryWrapper<>();
        exerciseQuery.eq("status", STATUS_ACTIVE);
        long totalExercises = exerciseMapper.selectCount(exerciseQuery);

        QueryWrapper<AnswerRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", user.getId());
        long totalAttempts = answerRecordMapper.selectCount(recordQuery);

        QueryWrapper<AnswerRecord> correctQuery = new QueryWrapper<>();
        correctQuery.eq("user_id", user.getId());
        correctQuery.eq("is_correct", true);
        long correctCount = answerRecordMapper.selectCount(correctQuery);

        long wrongCount = totalAttempts - correctCount;
        double accuracyRate = totalAttempts > 0 ? (double) correctCount / totalAttempts * 100 : 0;

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
                Math.round(accuracyRate * 100.0) / 100.0,
                totalScore,
                obtainedScore
        );
    }

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

        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        wrongBook.setNotes(notes);
        wrongBook.setUpdatedAt(LocalDateTime.now());
        wrongBookMapper.updateById(wrongBook);

        return convertToWrongBookDTO(wrongBook);
    }

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

        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        wrongBookMapper.deleteById(wrongBookId);
    }

    private int calculateAttemptCount(Long userId, Long exerciseId) {
        QueryWrapper<AnswerRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("exercise_id", exerciseId);
        long count = answerRecordMapper.selectCount(queryWrapper);
        return (int) count + 1;
    }

    private boolean checkAnswer(Exercise exercise, String userAnswer) {
        if (exercise == null || !StringUtils.hasText(exercise.getAnswer())) {
            return false;
        }
        if (!StringUtils.hasText(userAnswer)) {
            return false;
        }

        String correctAnswer = exercise.getAnswer().trim();
        String answer = userAnswer.trim();

        if (QUESTION_TYPE_SINGLE_CHOICE.equals(exercise.getQuestionType()) ||
                QUESTION_TYPE_MULTIPLE_CHOICE.equals(exercise.getQuestionType())) {
            return correctAnswer.equalsIgnoreCase(answer);
        } else {
            return correctAnswer.equals(answer);
        }
    }

    private void updateWrongBook(User user, Exercise exercise, AnswerRecord answerRecord, boolean isCorrect) {
        WrongBook existingWrongBook = wrongBookMapper.findByUserIdAndExerciseId(user.getId(), exercise.getId());

        if (existingWrongBook != null) {
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

    private WrongBookDTO convertToWrongBookDTO(WrongBook wrongBook) {
        if (wrongBook == null) {
            return null;
        }

        Exercise exercise = null;
        if (wrongBook.getExerciseId() != null) {
            exercise = exerciseMapper.selectById(wrongBook.getExerciseId());
        }

        Integer daysUntilReview = null;
        if (wrongBook.getNextReviewAt() != null) {
            long days = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                wrongBook.getNextReviewAt()
            ).toDays();
            daysUntilReview = (int) days;
        }

        WrongBookDTO dto = new WrongBookDTO();
        dto.setId(wrongBook.getId());
        dto.setUserId(wrongBook.getUserId());
        dto.setExercise(exercise != null ? convertToExerciseDTO(exercise) : null);
        dto.setAnswerRecordId(wrongBook.getAnswerRecordId());
        dto.setWrongCount(wrongBook.getWrongCount());
        dto.setCorrectCount(wrongBook.getCorrectCount());
        dto.setLastAttemptAt(wrongBook.getLastAttemptAt());
        dto.setMasterStatus(wrongBook.getMasterStatus());
        dto.setNotes(wrongBook.getNotes());
        dto.setTags(wrongBook.getTags());
        dto.setErrorType(wrongBook.getErrorType());
        dto.setSubject(wrongBook.getSubject());
        dto.setDifficulty(wrongBook.getDifficulty());
        dto.setPriority(wrongBook.getPriority());
        dto.setNextReviewAt(wrongBook.getNextReviewAt());
        dto.setReviewCount(wrongBook.getReviewCount());
        dto.setUserAnswer(wrongBook.getUserAnswer());
        dto.setDaysUntilReview(daysUntilReview);
        dto.setIsReviewed(wrongBook.getReviewCount() != null && wrongBook.getReviewCount() > 0);
        dto.setCreatedAt(wrongBook.getCreatedAt());
        dto.setUpdatedAt(wrongBook.getUpdatedAt());

        return dto;
    }

    @Override
    // 注：原 @Cacheable 缓存 key 未包含 userId，多用户会串数据，且错题数据变更频繁，故移除缓存
    public List<WrongBookDTO> getWrongBookByFilter(WrongBookFilterRequest request) {
        log.info("筛选错题本, request: {}", request);
        User user = getCurrentUser();

        QueryWrapper<WrongBook> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());

        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(w -> w
                .like("notes", request.getKeyword())
                .or()
                .exists("SELECT 1 FROM exercise e WHERE e.id = wrong_book.exercise_id AND e.content LIKE '%" + request.getKeyword() + "%'")
            );
        }

        if (StringUtils.hasText(request.getSubject())) {
            queryWrapper.eq("subject", request.getSubject());
        }

        if (StringUtils.hasText(request.getDifficulty())) {
            queryWrapper.eq("difficulty", request.getDifficulty());
        }

        if (StringUtils.hasText(request.getErrorType())) {
            queryWrapper.eq("error_type", request.getErrorType());
        }

        if (StringUtils.hasText(request.getMasterStatus())) {
            queryWrapper.eq("master_status", request.getMasterStatus());
        }

        if (StringUtils.hasText(request.getPriority())) {
            queryWrapper.eq("priority", request.getPriority());
        }

        if (StringUtils.hasText(request.getTag())) {
            queryWrapper.like("tags", request.getTag());
        }

        if (Boolean.TRUE.equals(request.getShowDueReview())) {
            queryWrapper.le("next_review_at", LocalDateTime.now());
            queryWrapper.or()
                .isNull("next_review_at");
        }

        // 排序列走白名单映射，未命中的一律回落到 created_at（不可把外部字符串直接拼进 SQL）
        String sortField = WRONG_BOOK_SORT_COLUMNS.getOrDefault(request.getSortBy(), "created_at");
        boolean desc = !"ASC".equalsIgnoreCase(request.getSortDirection());
        if (desc) {
            queryWrapper.orderByDesc(sortField);
        } else {
            queryWrapper.orderByAsc(sortField);
        }

        // 分页参数强校验：修正非正数，并限制单页上限；用 long 计算 offset 避免整数溢出导致负 offset
        int pageNum = request.getPageNum() != null && request.getPageNum() > 0 ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0
                ? Math.min(request.getPageSize(), MAX_PAGE_SIZE)
                : 20;
        long offset = (long) (pageNum - 1) * pageSize;
        queryWrapper.last("LIMIT " + offset + ", " + pageSize);

        List<WrongBook> wrongBooks = wrongBookMapper.selectList(queryWrapper);
        return wrongBooks.stream()
                .map(this::convertToWrongBookDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public WrongBookDTO addToWrongBook(AddToWrongBookRequest request) {
        log.info("添加题目到错题本, request: {}", request);
        User user = getCurrentUser();

        WrongBook existing = wrongBookMapper.findByUserIdAndExerciseId(user.getId(), request.getExerciseId());
        if (existing != null) {
            log.info("题目已在错题本中，更新记录");
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastAttemptAt(LocalDateTime.now());
            if (StringUtils.hasText(request.getNotes())) {
                existing.setNotes(request.getNotes());
            }
            if (StringUtils.hasText(request.getTags())) {
                existing.setTags(request.getTags());
            }
            if (StringUtils.hasText(request.getErrorType())) {
                existing.setErrorType(request.getErrorType());
            }
            if (StringUtils.hasText(request.getPriority())) {
                existing.setPriority(request.getPriority());
            }
            existing.setUpdatedAt(LocalDateTime.now());
            wrongBookMapper.updateById(existing);
            return convertToWrongBookDTO(existing);
        }

        Exercise exercise = exerciseMapper.selectById(request.getExerciseId());
        if (exercise == null) {
            throw new BusinessException(404, "练习题不存在");
        }

        WrongBook wrongBook = new WrongBook();
        wrongBook.setUserId(user.getId());
        wrongBook.setExerciseId(request.getExerciseId());
        wrongBook.setAnswerRecordId(request.getAnswerRecordId());
        wrongBook.setUserAnswer(request.getUserAnswer());
        wrongBook.setWrongCount(1);
        wrongBook.setCorrectCount(0);
        wrongBook.setLastAttemptAt(LocalDateTime.now());
        wrongBook.setMasterStatus(MASTER_STATUS_NOT_MASTERED);
        wrongBook.setNotes(request.getNotes());
        wrongBook.setTags(request.getTags());
        wrongBook.setErrorType(request.getErrorType());
        wrongBook.setSubject(request.getSubject() != null ? request.getSubject() : "未分类");
        wrongBook.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : exercise.getDifficulty());
        wrongBook.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        wrongBook.setReviewCount(0);
        wrongBook.setCreatedAt(LocalDateTime.now());
        wrongBook.setUpdatedAt(LocalDateTime.now());

        wrongBook.setNextReviewAt(LocalDateTime.now().plusDays(1));

        wrongBookMapper.insert(wrongBook);
        log.info("成功添加错题, id: {}", wrongBook.getId());

        return convertToWrongBookDTO(wrongBook);
    }

    @Override
    public WrongBookStatsDTO getWrongBookStats() {
        log.debug("获取错题本统计");
        User user = getCurrentUser();

        WrongBookStatsDTO stats = new WrongBookStatsDTO();

        QueryWrapper<WrongBook> totalQuery = new QueryWrapper<>();
        totalQuery.eq("user_id", user.getId());
        Long totalCount = wrongBookMapper.selectCount(totalQuery);
        stats.setTotalWrongCount(totalCount != null ? totalCount.intValue() : 0);

        QueryWrapper<WrongBook> statusQuery = new QueryWrapper<>();
        statusQuery.eq("user_id", user.getId());

        Long masteredCount = wrongBookMapper.selectCount(statusQuery.clone().eq("master_status", MASTER_STATUS_MASTERED));
        stats.setMasteredCount(masteredCount != null ? masteredCount.intValue() : 0);

        Long masteringCount = wrongBookMapper.selectCount(statusQuery.clone().eq("master_status", MASTER_STATUS_MASTERING));
        stats.setMasteringCount(masteringCount != null ? masteringCount.intValue() : 0);

        Long notMasteredCount = wrongBookMapper.selectCount(statusQuery.clone().eq("master_status", MASTER_STATUS_NOT_MASTERED));
        stats.setNotMasteredCount(notMasteredCount != null ? notMasteredCount.intValue() : 0);

        Long dueTodayCount = wrongBookMapper.selectCount(
            statusQuery.clone()
                .le("next_review_at", LocalDateTime.now().plusDays(1))
                .ge("next_review_at", LocalDateTime.now().minusDays(1))
        );
        stats.setDueTodayCount(dueTodayCount != null ? dueTodayCount.intValue() : 0);

        Long dueThisWeekCount = wrongBookMapper.selectCount(
            statusQuery.clone()
                .le("next_review_at", LocalDateTime.now().plusWeeks(1))
        );
        stats.setDueThisWeekCount(dueThisWeekCount != null ? dueThisWeekCount.intValue() : 0);

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public WrongBookDTO updateWrongBook(Long wrongBookId, AddToWrongBookRequest request) {
        log.info("更新错题信息, wrongBookId: {}, request: {}", wrongBookId, request);
        if (wrongBookId == null || wrongBookId <= 0) {
            throw new BusinessException(400, "错题本ID无效");
        }

        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(404, "错题不存在");
        }

        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        if (StringUtils.hasText(request.getNotes())) {
            wrongBook.setNotes(request.getNotes());
        }
        if (StringUtils.hasText(request.getTags())) {
            wrongBook.setTags(request.getTags());
        }
        if (StringUtils.hasText(request.getErrorType())) {
            wrongBook.setErrorType(request.getErrorType());
        }
        if (StringUtils.hasText(request.getPriority())) {
            wrongBook.setPriority(request.getPriority());
        }
        if (StringUtils.hasText(request.getSubject())) {
            wrongBook.setSubject(request.getSubject());
        }
        if (StringUtils.hasText(request.getDifficulty())) {
            wrongBook.setDifficulty(request.getDifficulty());
        }

        wrongBook.setUpdatedAt(LocalDateTime.now());
        wrongBookMapper.updateById(wrongBook);

        return convertToWrongBookDTO(wrongBook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public WrongBookDTO markAsReviewed(Long wrongBookId) {
        log.info("标记错题为已复习, wrongBookId: {}", wrongBookId);
        if (wrongBookId == null || wrongBookId <= 0) {
            throw new BusinessException(400, "错题本ID无效");
        }

        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(404, "错题不存在");
        }

        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        int newReviewCount = (wrongBook.getReviewCount() != null ? wrongBook.getReviewCount() : 0) + 1;
        wrongBook.setReviewCount(newReviewCount);

        LocalDateTime[] reviewSchedule = {
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(7),
            LocalDateTime.now().plusDays(14),
            LocalDateTime.now().plusDays(30)
        };

        int scheduleIndex = Math.min(newReviewCount - 1, reviewSchedule.length - 1);
        wrongBook.setNextReviewAt(reviewSchedule[scheduleIndex]);
        wrongBook.setLastAttemptAt(LocalDateTime.now());
        wrongBook.setUpdatedAt(LocalDateTime.now());

        wrongBookMapper.updateById(wrongBook);
        return convertToWrongBookDTO(wrongBook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "wrongBooks", allEntries = true)
    public WrongBookDTO redoWrongBook(Long wrongBookId, String userAnswer, boolean isCorrect) {
        log.info("重做错题, wrongBookId: {}, isCorrect: {}", wrongBookId, isCorrect);
        if (wrongBookId == null || wrongBookId <= 0) {
            throw new BusinessException(400, "错题本ID无效");
        }

        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(404, "错题不存在");
        }

        User user = getCurrentUser();
        if (!Objects.equals(wrongBook.getUserId(), user.getId())) {
            throw new BusinessException(403, "无权操作此错题");
        }

        wrongBook.setUserAnswer(userAnswer);
        wrongBook.setLastAttemptAt(LocalDateTime.now());

        if (isCorrect) {
            wrongBook.setCorrectCount(wrongBook.getCorrectCount() + 1);
            if (wrongBook.getCorrectCount() >= MASTER_THRESHOLD) {
                wrongBook.setMasterStatus(MASTER_STATUS_MASTERED);
                wrongBook.setNextReviewAt(null);
            } else {
                wrongBook.setMasterStatus(MASTER_STATUS_MASTERING);
            }
        } else {
            wrongBook.setWrongCount(wrongBook.getWrongCount() + 1);
            wrongBook.setMasterStatus(MASTER_STATUS_NOT_MASTERED);
            wrongBook.setNextReviewAt(LocalDateTime.now().plusDays(1));
        }

        wrongBook.setUpdatedAt(LocalDateTime.now());
        wrongBookMapper.updateById(wrongBook);

        return convertToWrongBookDTO(wrongBook);
    }

    @Override
    @Cacheable(value = "wrongBookExport", key = "'export:' + #format + ':' + #request.hashCode()")
    public byte[] exportWrongBook(String format, WrongBookFilterRequest request) {
        log.info("导出错题本, format: {}", format);
        List<WrongBookDTO> wrongBooks = getWrongBookByFilter(request);

        if ("json".equalsIgnoreCase(format)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.findAndRegisterModules();
                return mapper.writeValueAsBytes(wrongBooks);
            } catch (Exception e) {
                log.error("导出JSON失败", e);
                throw new BusinessException(500, "导出失败");
            }
        } else if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder();
            csv.append("ID,题目内容,用户答案,正确答案,错误次数,正确次数,掌握状态,笔记,标签,创建时间\n");
            for (WrongBookDTO wb : wrongBooks) {
                csv.append(wb.getId()).append(",");
                csv.append(wb.getExercise() != null ? wb.getExercise().getContent() : "").append(",");
                csv.append(wb.getUserAnswer()).append(",");
                csv.append(wb.getExercise() != null ? wb.getExercise().getAnswer() : "").append(",");
                csv.append(wb.getWrongCount()).append(",");
                csv.append(wb.getCorrectCount()).append(",");
                csv.append(wb.getMasterStatus()).append(",");
                csv.append(wb.getNotes()).append(",");
                csv.append(wb.getTags()).append(",");
                csv.append(wb.getCreatedAt()).append("\n");
            }
            return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        throw new BusinessException(400, "不支持的导出格式");
    }
}
