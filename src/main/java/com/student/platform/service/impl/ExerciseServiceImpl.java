package com.student.platform.service.impl;

import com.student.platform.dto.*;
import com.student.platform.entity.*;
import com.student.platform.mapper.*;
import com.student.platform.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseMapper exerciseMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongBookMapper wrongBookMapper;
    private final UserMapper userMapper;

    public List<ExerciseDTO> getExercisesByBook(Long bookId) {
        List<Exercise> exercises = exerciseMapper.findByBookId(bookId);
        return exercises.stream().map(this::convertToExerciseDTO).collect(java.util.stream.Collectors.toList());
    }

    public List<ExerciseDTO> getExercisesByKnowledgeNode(Long knowledgeNodeId) {
        List<Exercise> exercises = exerciseMapper.findByKnowledgeNodeId(knowledgeNodeId);
        return exercises.stream().map(this::convertToExerciseDTO).collect(java.util.stream.Collectors.toList());
    }

    public ExerciseDTO getExerciseById(Long id) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw new RuntimeException("题目不存在");
        }
        return convertToExerciseDTO(exercise);
    }

    @Transactional
    public AnswerRecordDTO submitAnswer(SubmitAnswerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        Exercise exercise = exerciseMapper.selectById(request.getExerciseId());
        if (exercise == null) {
            throw new RuntimeException("题目不存在");
        }

        boolean isCorrect = checkAnswer(exercise, request.getUserAnswer());
        int scoreObtained = isCorrect ? exercise.getScore() : 0;

        // 计算尝试次数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AnswerRecord> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        queryWrapper.eq("exercise_id", exercise.getId());
        long count = answerRecordMapper.selectCount(queryWrapper);
        int attemptCount = (int) count + 1;

        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setUserId(user.getId());
        answerRecord.setExerciseId(exercise.getId());
        answerRecord.setUserAnswer(request.getUserAnswer());
        answerRecord.setIsCorrect(isCorrect);
        answerRecord.setScoreObtained(scoreObtained);
        answerRecord.setTimeSpent(request.getTimeSpent());
        answerRecord.setAttemptCount(attemptCount);

        answerRecordMapper.insert(answerRecord);

        updateWrongBook(user, exercise, answerRecord, isCorrect);

        return convertToAnswerRecordDTO(answerRecord);
    }

    public List<AnswerRecordDTO> getAnswerHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<AnswerRecord> records = answerRecordMapper.findByUserId(user.getId());
        return records.stream().map(this::convertToAnswerRecordDTO).collect(java.util.stream.Collectors.toList());
    }

    public List<WrongBookDTO> getWrongBook() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<WrongBook> wrongBooks = wrongBookMapper.findByUserId(user.getId());
        return wrongBooks.stream().map(this::convertToWrongBookDTO).collect(java.util.stream.Collectors.toList());
    }

    public ExerciseStatsDTO getExerciseStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 计算总题目数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Exercise> exerciseQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        exerciseQuery.eq("status", "ACTIVE");
        long totalExercises = exerciseMapper.selectCount(exerciseQuery);

        // 计算正确和错误次数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AnswerRecord> correctQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        correctQuery.eq("user_id", user.getId());
        correctQuery.eq("is_correct", true);
        long correctCount = answerRecordMapper.selectCount(correctQuery);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AnswerRecord> wrongQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrongQuery.eq("user_id", user.getId());
        wrongQuery.eq("is_correct", false);
        long wrongCount = answerRecordMapper.selectCount(wrongQuery);

        long totalAttempts = correctCount + wrongCount;
        double accuracyRate = totalAttempts > 0 ? (double) correctCount / totalAttempts * 100 : 0;

        // 计算分数
        List<AnswerRecord> allRecords = answerRecordMapper.findByUserId(user.getId());
        int totalScore = 0;
        int obtainedScore = 0;
        for (AnswerRecord record : allRecords) {
            Exercise ex = exerciseMapper.selectById(record.getExerciseId());
            if (ex != null) {
                totalScore += ex.getScore();
            }
            obtainedScore += record.getScoreObtained();
        }

        return new ExerciseStatsDTO(
                totalExercises,
                totalAttempts,
                correctCount,
                wrongCount,
                accuracyRate,
                totalScore,
                obtainedScore
        );
    }

    @Transactional
    public WrongBookDTO updateWrongBookNotes(Long wrongBookId, String notes) {
        WrongBook wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new RuntimeException("错题不存在");
        }
        wrongBook.setNotes(notes);
        wrongBookMapper.updateById(wrongBook);
        return convertToWrongBookDTO(wrongBook);
    }

    @Transactional
    public void removeFromWrongBook(Long wrongBookId) {
        wrongBookMapper.deleteById(wrongBookId);
    }

    private boolean checkAnswer(Exercise exercise, String userAnswer) {
        if ("SINGLE_CHOICE".equals(exercise.getQuestionType()) ||
                "MULTIPLE_CHOICE".equals(exercise.getQuestionType())) {
            return exercise.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
        } else {
            return exercise.getAnswer().trim().equals(userAnswer.trim());
        }
    }

    private void updateWrongBook(User user, Exercise exercise, AnswerRecord answerRecord, boolean isCorrect) {
        WrongBook existingWrongBook = wrongBookMapper.findByUserIdAndExerciseId(user.getId(), exercise.getId());

        if (existingWrongBook != null) {
            WrongBook wrongBook = existingWrongBook;
            if (isCorrect) {
                wrongBook.setCorrectCount(wrongBook.getCorrectCount() + 1);
                if (wrongBook.getCorrectCount() >= 3) {
                    wrongBook.setMasterStatus("MASTERED");
                } else if (wrongBook.getCorrectCount() >= 1) {
                    wrongBook.setMasterStatus("MASTERING");
                }
            } else {
                wrongBook.setWrongCount(wrongBook.getWrongCount() + 1);
                wrongBook.setMasterStatus("NOT_MASTERED");
            }
            wrongBook.setLastAttemptAt(java.time.LocalDateTime.now());
            wrongBook.setAnswerRecordId(answerRecord.getId());
            wrongBookMapper.updateById(wrongBook);
        } else if (!isCorrect) {
            WrongBook wrongBook = new WrongBook();
            wrongBook.setUserId(user.getId());
            wrongBook.setExerciseId(exercise.getId());
            wrongBook.setAnswerRecordId(answerRecord.getId());
            wrongBook.setWrongCount(1);
            wrongBook.setCorrectCount(0);
            wrongBook.setLastAttemptAt(java.time.LocalDateTime.now());
            wrongBook.setMasterStatus("NOT_MASTERED");
            wrongBookMapper.insert(wrongBook);
        }
    }

    private ExerciseDTO convertToExerciseDTO(Exercise exercise) {
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
        Exercise exercise = wrongBook.getExerciseId() != null ? exerciseMapper.selectById(wrongBook.getExerciseId()) : null;
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