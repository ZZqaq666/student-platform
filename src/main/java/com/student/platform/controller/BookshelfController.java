package com.student.platform.controller;

import com.student.platform.dto.BookDTO;
import com.student.platform.dto.Result;
import com.student.platform.dto.UpdateProgressRequest;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.service.BookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookshelf")
@RequiredArgsConstructor
@Tag(name = "个人书架", description = "个人书架相关接口")
public class BookshelfController {

    private final BookshelfService bookshelfService;

    @GetMapping("/books")
    @Operation(summary = "获取所有书籍", description = "获取系统中所有可用的书籍")
    public Result<List<BookDTO>> getAllBooks(@RequestParam(required = false) Long userId) {
        return Result.success(bookshelfService.getAllBooks());
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "获取书籍详情", description = "根据ID获取书籍详情")
    public Result<BookDTO> getBookById(@PathVariable Long id) {
        return Result.success(bookshelfService.getBookById(id));
    }

    @GetMapping("/books/search")
    @Operation(summary = "搜索书籍", description = "根据关键词搜索书籍")
    public Result<List<BookDTO>> searchBooks(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String courseType) {
        return Result.success(bookshelfService.searchBooks(keyword, category, major, semester, courseType));
    }

    @GetMapping("/books/subject/{subject}")
    @Operation(summary = "按学科获取书籍", description = "根据学科获取书籍列表")
    public Result<List<BookDTO>> getBooksBySubject(@PathVariable String subject) {
        return Result.success(bookshelfService.getBooksBySubject(subject));
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的书架", description = "获取当前用户的书架")
    public Result<List<UserBookDTO>> getUserBooks() {
        return Result.success(bookshelfService.getUserBooks());
    }

    @GetMapping("/my/{bookId}")
    @Operation(summary = "获取我的书架中的书籍", description = "获取当前用户书架中指定书籍的信息")
    public Result<UserBookDTO> getUserBook(@PathVariable Long bookId) {
        return Result.success(bookshelfService.getUserBook(bookId));
    }

    @PostMapping("/my/{bookId}")
    @Operation(summary = "添加书籍到书架", description = "将指定书籍添加到当前用户的书架")
    public Result<UserBookDTO> addBookToShelf(@PathVariable Long bookId) {
        return Result.success(bookshelfService.addBookToShelf(bookId));
    }

    @DeleteMapping("/my/{bookId}")
    @Operation(summary = "从书架移除书籍", description = "从当前用户的书架中移除指定书籍")
    public Result<Void> removeBookFromShelf(@PathVariable Long bookId) {
        bookshelfService.removeBookFromShelf(bookId);
        return Result.success();
    }

    @PutMapping("/my/{bookId}/progress")
    @Operation(summary = "更新学习进度", description = "更新当前用户对指定书籍的学习进度")
    public Result<UserBookDTO> updateProgress(@PathVariable Long bookId, @RequestBody UpdateProgressRequest request) {
        return Result.success(bookshelfService.updateProgress(bookId, request));
    }
}
