package com.student.platform.service;

import com.student.platform.dto.BookDTO;
import com.student.platform.dto.UpdateProgressRequest;
import com.student.platform.dto.UserBookDTO;

import java.util.List;

public interface BookshelfService {
    List<BookDTO> getAllBooks();
    BookDTO getBookById(Long id);
    List<BookDTO> searchBooks(String keyword);
    List<BookDTO> getBooksBySubject(String subject);
    List<UserBookDTO> getUserBooks();
    UserBookDTO getUserBook(Long bookId);
    UserBookDTO addBookToShelf(Long bookId);
    void removeBookFromShelf(Long bookId);
    UserBookDTO updateProgress(Long bookId, UpdateProgressRequest request);
    UserBookDTO getRecentBook();
}