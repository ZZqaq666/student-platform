package com.student.platform.learning.service;

import com.student.platform.common.dto.NoteDTO;
import java.util.List;

public interface NoteService {

    List<NoteDTO> getAllNotes(Long userId);

    List<NoteDTO> getNotesByBook(Long userId, Long bookId);

    List<NoteDTO> getNotesByChapter(Long userId, Long chapterId);

    NoteDTO createNote(Long userId, NoteDTO noteDTO);

    NoteDTO updateNote(Long userId, Long noteId, NoteDTO noteDTO);

    boolean deleteNote(Long userId, Long noteId);

    NoteDTO getNoteById(Long userId, Long noteId);
}
