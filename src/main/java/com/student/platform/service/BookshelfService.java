package com.student.platform.service;

import com.student.platform.dto.BookChapterTreeDTO;
import com.student.platform.dto.BookDTO;
import com.student.platform.dto.UpdateProgressRequest;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.entity.BookChapter;

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
    UserBookDTO getRecentBook();
    List<BookChapter> getBookChapters(Long bookId);
    List<BookChapterTreeDTO> getBookChapterTree(Long bookId);
    List<BookChapter> searchChapters(Long bookId, String keyword);
    Map<String, Object> addBooks(Long userId, Map<String, List<BookDTO>> request);
}