package com.student.platform.learning.service;

import com.student.platform.common.dto.BookChapterTreeDTO;
import com.student.platform.common.dto.BookDTO;
import com.student.platform.common.dto.UpdateBookRequest;
import com.student.platform.common.dto.UpdateProgressRequest;
import com.student.platform.common.dto.UserBookDTO;
import com.student.platform.learning.entity.BookChapter;

import java.util.List;
import java.util.Map;

public interface BookshelfService {

    List<BookDTO> getAllBooks();

    BookDTO getBookById(Long id);

    List<BookDTO> searchBooks(String keyword, String category, String major, String semester, String courseType);

    List<BookDTO> getBooksBySubject(String subject);

    List<UserBookDTO> getUserBooks();

    UserBookDTO getUserBook(Long bookId);

    UserBookDTO addBookToShelf(Long bookId);

    void removeBookFromShelf(Long bookId);

    UserBookDTO updateProgress(Long bookId, UpdateProgressRequest request);

    /**
     * 编辑共享目录中的书籍元信息。仅管理员可调用，
     * book 表无归属列，一次编辑对所有用户生效。
     */
    BookDTO updateBook(Long bookId, UpdateBookRequest request);

    UserBookDTO getRecentBook();

    List<BookChapter> getBookChapters(Long bookId);

    List<BookChapterTreeDTO> getBookChapterTree(Long bookId);

    List<BookChapter> searchChapters(Long bookId, String keyword);

    Map<String, Object> addBooks(Long userId, Map<String, List<BookDTO>> request);
}
