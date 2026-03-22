package com.student.platform.controller;

import com.student.platform.dto.BookDTO;
import com.student.platform.dto.Result;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.service.BookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "书籍管理", description = "书籍相关接口")
public class BookController {

    private final BookshelfService bookshelfService;

    @GetMapping
    @Operation(summary = "获取所有书籍", description = "获取系统中所有可用的书籍")
    public Result<List<BookDTO>> getAllBooks() {
        return Result.success(bookshelfService.getAllBooks());
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近学习的书籍", description = "获取当前用户最近学习的书籍")
    public Result<UserBookDTO> getRecentBook() {
        UserBookDTO recentBook = bookshelfService.getRecentBook();
        return Result.success(recentBook);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索书籍", description = "根据关键词搜索书籍")
    public Result<List<BookDTO>> searchBooks(@RequestParam String keyword) {
        return Result.success(bookshelfService.searchBooks(keyword));
    }
}
