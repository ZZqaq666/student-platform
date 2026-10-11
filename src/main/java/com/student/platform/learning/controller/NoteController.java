package com.student.platform.learning.controller;

import com.student.platform.common.dto.NoteDTO;
import com.student.platform.common.result.Result;
import com.student.platform.learning.service.NoteService;
import com.student.platform.user.config.UserIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "笔记管理", description = "笔记的增删改查接口（按当前登录用户隔离）")
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "获取所有笔记", description = "获取当前用户的所有笔记")
    @GetMapping
    public Result<List<NoteDTO>> getAllNotes(HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            List<NoteDTO> notes = noteService.getAllNotes(userId);
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取所有笔记失败: {}", e.getMessage(), e);
            return Result.error("获取所有笔记失败");
        }
    }

    @Operation(summary = "获取书籍的笔记", description = "获取当前用户在指定书籍下的笔记")
    @GetMapping("/book/{bookId}")
    public Result<List<NoteDTO>> getNotesByBook(
            @Parameter(description = "书籍ID", required = true) @PathVariable Long bookId,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            List<NoteDTO> notes = noteService.getNotesByBook(userId, bookId);
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取书籍笔记失败: {}", e.getMessage(), e);
            return Result.error("获取书籍笔记失败");
        }
    }

    @Operation(summary = "获取章节的笔记", description = "获取当前用户在指定章节下的笔记")
    @GetMapping("/chapter/{chapterId}")
    public Result<List<NoteDTO>> getNotesByChapter(
            @Parameter(description = "章节ID", required = true) @PathVariable Long chapterId,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            List<NoteDTO> notes = noteService.getNotesByChapter(userId, chapterId);
            return Result.success(notes);
        } catch (Exception e) {
            log.error("获取章节笔记失败: {}", e.getMessage(), e);
            return Result.error("获取章节笔记失败");
        }
    }

    @Operation(summary = "创建笔记", description = "创建新的笔记（归属当前用户）")
    @PostMapping
    public Result<NoteDTO> createNote(
            @Parameter(description = "笔记信息", required = true) @RequestBody NoteDTO noteDTO,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            NoteDTO createdNote = noteService.createNote(userId, noteDTO);
            return Result.success(createdNote);
        } catch (Exception e) {
            log.error("创建笔记失败: {}", e.getMessage(), e);
            return Result.error("创建笔记失败");
        }
    }

    @Operation(summary = "更新笔记", description = "更新指定的笔记（仅限本人笔记）")
    @PutMapping("/{noteId}")
    public Result<NoteDTO> updateNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            @Parameter(description = "笔记信息", required = true) @RequestBody NoteDTO noteDTO,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            NoteDTO updatedNote = noteService.updateNote(userId, noteId, noteDTO);
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

    @Operation(summary = "删除笔记", description = "删除指定的笔记（仅限本人笔记）")
    @DeleteMapping("/{noteId}")
    public Result<Boolean> deleteNote(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            boolean deleted = noteService.deleteNote(userId, noteId);
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

    @Operation(summary = "获取笔记详情", description = "获取指定笔记的详细信息（仅限本人笔记）")
    @GetMapping("/{noteId}")
    public Result<NoteDTO> getNoteById(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            HttpServletRequest request) {
        try {
            Long userId = currentUserId(request);
            NoteDTO note = noteService.getNoteById(userId, noteId);
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

    /**
     * 从 UserIdInterceptor 注入的请求属性中取当前登录用户ID
     */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(UserIdInterceptor.USER_ID_ATTRIBUTE);
        if (userId instanceof Number num) {
            return num.longValue();
        }
        throw new IllegalStateException("未获取到当前登录用户");
    }
}
