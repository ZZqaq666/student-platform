package com.student.platform.learning.service.impl;

import com.student.platform.common.dto.NoteDTO;
import com.student.platform.learning.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    private final Map<Long, NoteDTO> notes = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<NoteDTO> getAllNotes() {
        log.info("获取所有笔记, 总数: {}", notes.size());
        return new ArrayList<>(notes.values());
    }

    @Override
    public List<NoteDTO> getNotesByBook(Long bookId) {
        log.info("获取书籍 {} 的笔记", bookId);
        List<NoteDTO> bookNotes = new ArrayList<>();
        for (NoteDTO note : notes.values()) {
            if (note.getBookId() != null && note.getBookId().equals(bookId)) {
                bookNotes.add(note);
            }
        }
        log.info("找到 {} 条笔记", bookNotes.size());
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
        log.info("找到 {} 条笔记", chapterNotes.size());
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
        log.info("笔记创建成功, ID: {}", id);
        return noteDTO;
    }

    @Override
    public NoteDTO updateNote(Long noteId, NoteDTO noteDTO) {
        log.info("更新笔记: {}", noteId);
        NoteDTO existingNote = notes.get(noteId);
        if (existingNote != null) {
            existingNote.setTitle(noteDTO.getTitle());
            existingNote.setContent(noteDTO.getContent());
            existingNote.setBookId(noteDTO.getBookId());
            existingNote.setChapterId(noteDTO.getChapterId());
            existingNote.setUpdatedAt(LocalDateTime.now());
            notes.put(noteId, existingNote);
            log.info("笔记更新成功: {}", noteId);
            return existingNote;
        }
        log.warn("笔记不存在: {}", noteId);
        return null;
    }

    @Override
    public boolean deleteNote(Long noteId) {
        log.info("删除笔记: {}", noteId);
        boolean removed = notes.remove(noteId) != null;
        if (removed) {
            log.info("笔记删除成功: {}", noteId);
        } else {
            log.warn("笔记不存在，删除失败: {}", noteId);
        }
        return removed;
    }

    @Override
    public NoteDTO getNoteById(Long noteId) {
        log.info("获取笔记详情: {}", noteId);
        return notes.get(noteId);
    }
}
