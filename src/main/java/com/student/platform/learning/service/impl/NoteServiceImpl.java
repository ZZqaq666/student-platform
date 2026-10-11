package com.student.platform.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.common.dto.NoteDTO;
import com.student.platform.learning.entity.Note;
import com.student.platform.learning.mapper.NoteMapper;
import com.student.platform.learning.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学习笔记服务：基于 note 表的真实实现，所有操作强制按当前登录用户隔离
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;

    @Override
    public List<NoteDTO> getAllNotes(Long userId) {
        log.info("获取用户 {} 的所有笔记", userId);
        return noteMapper.selectList(new QueryWrapper<Note>()
                        .eq("user_id", userId)
                        .orderByDesc("updated_at"))
                .stream().map(NoteServiceImpl::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<NoteDTO> getNotesByBook(Long userId, Long bookId) {
        log.info("获取用户 {} 书籍 {} 的笔记", userId, bookId);
        return noteMapper.selectList(new QueryWrapper<Note>()
                        .eq("user_id", userId)
                        .eq("book_id", bookId)
                        .orderByDesc("updated_at"))
                .stream().map(NoteServiceImpl::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<NoteDTO> getNotesByChapter(Long userId, Long chapterId) {
        log.info("获取用户 {} 章节 {} 的笔记", userId, chapterId);
        return noteMapper.selectList(new QueryWrapper<Note>()
                        .eq("user_id", userId)
                        .eq("chapter_id", chapterId)
                        .orderByDesc("updated_at"))
                .stream().map(NoteServiceImpl::toDTO).collect(Collectors.toList());
    }

    @Override
    public NoteDTO createNote(Long userId, NoteDTO noteDTO) {
        log.info("用户 {} 创建笔记: {}", userId, noteDTO.getTitle());
        Note note = new Note();
        note.setUserId(userId);
        note.setBookId(noteDTO.getBookId());
        note.setChapterId(noteDTO.getChapterId());
        note.setTitle(noteDTO.getTitle());
        note.setContent(noteDTO.getContent());
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        noteMapper.insert(note);
        log.info("笔记创建成功, ID: {}", note.getId());
        return toDTO(note);
    }

    @Override
    public NoteDTO updateNote(Long userId, Long noteId, NoteDTO noteDTO) {
        log.info("用户 {} 更新笔记: {}", userId, noteId);
        // 按 id + userId 条件更新，天然防止越权修改他人笔记
        Note existing = noteMapper.selectOne(new QueryWrapper<Note>()
                .eq("id", noteId)
                .eq("user_id", userId));
        if (existing == null) {
            log.warn("笔记不存在或无权操作: userId={}, noteId={}", userId, noteId);
            return null;
        }
        existing.setBookId(noteDTO.getBookId());
        existing.setChapterId(noteDTO.getChapterId());
        existing.setTitle(noteDTO.getTitle());
        existing.setContent(noteDTO.getContent());
        existing.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(existing);
        log.info("笔记更新成功: {}", noteId);
        return toDTO(existing);
    }

    @Override
    public boolean deleteNote(Long userId, Long noteId) {
        log.info("用户 {} 删除笔记: {}", userId, noteId);
        // 按 id + userId 条件删除，天然防止越权删除他人笔记
        int removed = noteMapper.delete(new QueryWrapper<Note>()
                .eq("id", noteId)
                .eq("user_id", userId));
        if (removed > 0) {
            log.info("笔记删除成功: {}", noteId);
            return true;
        }
        log.warn("笔记不存在或无权删除: userId={}, noteId={}", userId, noteId);
        return false;
    }

    @Override
    public NoteDTO getNoteById(Long userId, Long noteId) {
        log.info("用户 {} 获取笔记详情: {}", userId, noteId);
        Note note = noteMapper.selectOne(new QueryWrapper<Note>()
                .eq("id", noteId)
                .eq("user_id", userId));
        return note == null ? null : toDTO(note);
    }

    private static NoteDTO toDTO(Note note) {
        NoteDTO dto = new NoteDTO();
        dto.setId(note.getId());
        dto.setUserId(note.getUserId());
        dto.setBookId(note.getBookId());
        dto.setChapterId(note.getChapterId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        return dto;
    }
}
