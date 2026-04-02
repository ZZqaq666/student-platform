package com.student.platform.service.impl;

import com.student.platform.dto.NoteDTO;
import com.student.platform.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 笔记服务实现
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    
    // 内存存储笔记
    private final Map<Long, NoteDTO> notes = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public List<NoteDTO> getAllNotes() {
        log.info("获取所有笔记");
        return new ArrayList<>(notes.values());
    }
    
    @Override
    public List<NoteDTO> getNotesByBook(Long bookId) {
        log.info("获取书籍 {} 的笔记", bookId);
        List<NoteDTO> bookNotes = new ArrayList<>();
        for (NoteDTO note : notes.values()) {
            if (note.getBookId().equals(bookId)) {
                bookNotes.add(note);
            }
        }
        return bookNotes;
    }
    
    @Override
    public List<NoteDTO> getNotesByChapter(Long chapterId) {
        log.info("获取章节 {} 的笔记", chapterId);
        List<NoteDTO> chapterNotes = new ArrayList<>();
        for (NoteDTO note : notes.values()) {
            if (note.getChapterId() != null && note.getChapterId().equals(chapterId)) {
                chapterNotes.add(note);
            }
        }
        return chapterNotes;
    }
    
    @Override
    public NoteDTO createNote(NoteDTO noteDTO) {
        log.info("创建笔记: {}", noteDTO.getTitle());
        Long id = idGenerator.getAndIncrement();
        noteDTO.setId(id);
        noteDTO.setCreatedAt(LocalDateTime.now());
        noteDTO.setUpdatedAt(LocalDateTime.now());
        notes.put(id, noteDTO);
        return noteDTO;
    }
    
    @Override
    public NoteDTO updateNote(Long noteId, NoteDTO noteDTO) {
        log.info("更新笔记: {}", noteId);
        NoteDTO existingNote = notes.get(noteId);
        if (existingNote != null) {
            existingNote.setTitle(noteDTO.getTitle());
            existingNote.setContent(noteDTO.getContent());
            existingNote.setUpdatedAt(LocalDateTime.now());
            notes.put(noteId, existingNote);
            return existingNote;
        }
        return null;
    }
    
    @Override
    public boolean deleteNote(Long noteId) {
        log.info("删除笔记: {}", noteId);
        return notes.remove(noteId) != null;
    }
    
    @Override
    public NoteDTO getNoteById(Long noteId) {
        log.info("获取笔记详情: {}", noteId);
        return notes.get(noteId);
    }
}
