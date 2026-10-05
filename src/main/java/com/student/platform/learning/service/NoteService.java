package com.student.platform.learning.service;

import com.student.platform.common.dto.NoteDTO;
import java.util.List;

public interface NoteService {

    List<NoteDTO> getAllNotes();

    List<NoteDTO> getNotesByBook(Long bookId);

    List<NoteDTO> getNotesByChapter(Long chapterId);

    NoteDTO createNote(NoteDTO noteDTO);

    NoteDTO updateNote(Long noteId, NoteDTO noteDTO);

    boolean deleteNote(Long noteId);

    NoteDTO getNoteById(Long noteId);
}
