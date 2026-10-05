package com.student.platform.exercise.service;

import com.student.platform.common.dto.*;

import java.util.List;

public interface ExerciseService {

    List<ExerciseDTO> getExercisesByBook(Long bookId);

    List<ExerciseDTO> getExercisesByKnowledgeNode(Long knowledgeNodeId);

    ExerciseDTO getExerciseById(Long id);

    AnswerRecordDTO submitAnswer(SubmitAnswerRequest request);

    List<AnswerRecordDTO> getAnswerHistory();

    List<WrongBookDTO> getWrongBook();

    List<WrongBookDTO> getWrongBookByFilter(WrongBookFilterRequest request);

    WrongBookDTO addToWrongBook(AddToWrongBookRequest request);

    ExerciseStatsDTO getExerciseStats();

    WrongBookStatsDTO getWrongBookStats();

    WrongBookDTO updateWrongBookNotes(Long wrongBookId, String notes);

    WrongBookDTO updateWrongBook(Long wrongBookId, AddToWrongBookRequest request);

    void removeFromWrongBook(Long wrongBookId);

    WrongBookDTO markAsReviewed(Long wrongBookId);

    WrongBookDTO redoWrongBook(Long wrongBookId, String userAnswer, boolean isCorrect);

    byte[] exportWrongBook(String format, WrongBookFilterRequest request);
}
