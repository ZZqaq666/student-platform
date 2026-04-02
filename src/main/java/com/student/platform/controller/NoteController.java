package com.student.platform.controller;

import com.student.platform.dto.NoteDTO;
import com.student.platform.dto.Result;
import com.student.platform.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记管理控制器
 */
@Tag(name = "笔记管理", description = "笔记的增删改查接口")
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {

    private final NoteService noteService;

    /**
     * 获取用户的所有笔记
     *
     * @return 笔记列表
     */
    @Operation(summary = "获取所有笔记", description = "获取用户的所有笔记")
    @GetMapping
    public Result<List<NoteDTO>> getAllNotes() {
        try {
            List<NoteDTO> notes = noteService.getAllNotes();
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取所有笔记失败: {}", e.getMessage(), e);
            return Result.error("获取所有笔记失败");
        }
    }

    /**
     * 获取指定书籍的笔记
     *
     * @param bookId 书籍ID
     * @return 笔记列表
     */
    @Operation(summary = "获取书籍的笔记", description = "获取指定书籍的笔记")
    @GetMapping("/book/{bookId}")
    public Result<List<NoteDTO>> getNotesByBook(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long bookId) {
        try {
            List<NoteDTO> notes = noteService.getNotesByBook(bookId);
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取书籍笔记失败: {}", e.getMessage(), e);
            return Result.error("获取书籍笔记失败");
        }
    }

    /**
     * 获取指定章节的笔记
     *
     * @param chapterId 章节ID
     * @return 笔记列表
     */
    @Operation(summary = "获取章节的笔记", description = "获取指定章节的笔记")
    @GetMapping("/chapter/{chapterId}")
    public Result<List<NoteDTO>> getNotesByChapter(
            @Parameter(description = "章节ID", required = true) @PathVariable Long chapterId) {
        try {
            List<NoteDTO> notes = noteService.getNotesByChapter(chapterId);
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取章节笔记失败: {}", e.getMessage(), e);
            return Result.error("获取章节笔记失败");
        }
    }

    /**
     * 创建笔记
     *
     * @param noteDTO 笔记DTO
     * @return 创建的笔记
     */
    @Operation(summary = "创建笔记", description = "创建新的笔记")
    @PostMapping
    public Result<NoteDTO> createNote(
            @Parameter(description = "笔记信息", required = true) @RequestBody NoteDTO noteDTO) {
        try {
            NoteDTO createdNote = noteService.createNote(noteDTO);
            return Result.success(createdNote);
        } catch (Exception e) {
            log.error("创建笔记失败: {}", e.getMessage(), e);
            return Result.error("创建笔记失败");
        }
    }

    /**
     * 更新笔记
     *
     * @param noteId 笔记ID
     * @param noteDTO 笔记DTO
     * @return 更新后的笔记
     */
    @Operation(summary = "更新笔记", description = "更新指定的笔记")
    @PutMapping("/{noteId}")
    public Result<NoteDTO> updateNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            @Parameter(description = "笔记信息", required = true) @RequestBody NoteDTO noteDTO) {
        try {
            NoteDTO updatedNote = noteService.updateNote(noteId, noteDTO);
            if (updatedNote != null) {
                return Result.success(updatedNote);
            } else {
                return Result.error("笔记不存在");
            }
        } catch (Exception e) {
            log.error("更新笔记失败: {}", e.getMessage(), e);
            return Result.error("更新笔记失败");
        }
    }

    /**
     * 删除笔记
     *
     * @param noteId 笔记ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除笔记", description = "删除指定的笔记")
    @DeleteMapping("/{noteId}")
    public Result<Boolean> deleteNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        try {
            boolean deleted = noteService.deleteNote(noteId);
            if (deleted) {
                return Result.success(true);
            } else {
                return Result.error("笔记不存在");
            }
        } catch (Exception e) {
            log.error("删除笔记失败: {}", e.getMessage(), e);
            return Result.error("删除笔记失败");
        }
    }

    /**
     * 获取笔记详情
     *
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    @Operation(summary = "获取笔记详情", description = "获取指定笔记的详细信息")
    @GetMapping("/{noteId}")
    public Result<NoteDTO> getNoteById(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        try {
            NoteDTO note = noteService.getNoteById(noteId);
            if (note != null) {
                return Result.success(note);
            } else {
                return Result.error("笔记不存在");
            }
        } catch (Exception e) {
            log.error("获取笔记详情失败: {}", e.getMessage(), e);
            return Result.error("获取笔记详情失败");
        }
    }
}
