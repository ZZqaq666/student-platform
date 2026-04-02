package com.student.platform.service;

import com.student.platform.dto.NoteDTO;
import java.util.List;

/**
 * 笔记服务接口
 */
public interface NoteService {
    /**
     * 获取用户的所有笔记
     * @return 笔记列表
     */
    List<NoteDTO> getAllNotes();
    
    /**
     * 获取指定书籍的笔记
     * @param bookId 书籍ID
     * @return 笔记列表
     */
    List<NoteDTO> getNotesByBook(Long bookId);
    
    /**
     * 获取指定章节的笔记
     * @param chapterId 章节ID
     * @return 笔记列表
     */
    List<NoteDTO> getNotesByChapter(Long chapterId);
    
    /**
     * 创建笔记
     * @param noteDTO 笔记DTO
     * @return 创建的笔记
     */
    NoteDTO createNote(NoteDTO noteDTO);
    
    /**
     * 更新笔记
     * @param noteId 笔记ID
     * @param noteDTO 笔记DTO
     * @return 更新后的笔记
     */
    NoteDTO updateNote(Long noteId, NoteDTO noteDTO);
    
    /**
     * 删除笔记
     * @param noteId 笔记ID
     * @return 是否删除成功
     */
    boolean deleteNote(Long noteId);
    
    /**
     * 获取笔记详情
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    NoteDTO getNoteById(Long noteId);
}
