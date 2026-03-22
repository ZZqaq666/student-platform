package com.student.platform.service;

import com.student.platform.dto.*;

import java.util.List;

public interface ExerciseService {
    List<ExerciseDTO> getExercisesByBook(Long bookId);
    List<ExerciseDTO> getExercisesByKnowledgeNode(Long knowledgeNodeId);
    ExerciseDTO getExerciseById(Long id);
    AnswerRecordDTO submitAnswer(SubmitAnswerRequest request);
    List<AnswerRecordDTO> getAnswerHistory();
    List<WrongBookDTO> getWrongBook();
    ExerciseStatsDTO getExerciseStats();
    WrongBookDTO updateWrongBookNotes(Long wrongBookId, String notes);
    void removeFromWrongBook(Long wrongBookId);
}