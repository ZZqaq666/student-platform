package com.student.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.dto.BookDTO;
import com.student.platform.dto.UpdateProgressRequest;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.entity.Book;
import com.student.platform.entity.User;
import com.student.platform.entity.UserBook;
import com.student.platform.mapper.BookMapper;
import com.student.platform.mapper.UserBookMapper;
import com.student.platform.mapper.UserMapper;
import com.student.platform.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookshelfServiceImpl implements BookshelfService {

    private final BookMapper bookMapper;
    private final UserBookMapper userBookMapper;
    private final UserMapper userMapper;

    @Cacheable(value = "books", key = "'all'")
    public List<BookDTO> getAllBooks() {
        // 使用 MyBatis-Plus 的条件查询
        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "ACTIVE");
        List<Book> books = bookMapper.selectList(queryWrapper);
        return books.stream().map(this::convertToBookDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "books", key = "#id")
    public BookDTO getBookById(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new RuntimeException("书籍不存在");
        }
        return convertToBookDTO(book);
    }

    public List<BookDTO> searchBooks(String keyword) {
        List<Book> books = bookMapper.searchByTitle(keyword);
        return books.stream().map(this::convertToBookDTO).collect(Collectors.toList());
    }

    public List<BookDTO> getBooksBySubject(String subject) {
        List<Book> books = bookMapper.findBySubject(subject);
        return books.stream().map(this::convertToBookDTO).collect(Collectors.toList());
    }

    public List<UserBookDTO> getUserBooks() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<UserBook> userBooks = userBookMapper.findByUserId(user.getId());
        return userBooks.stream().map(this::convertToUserBookDTO).collect(Collectors.toList());
    }

    public UserBookDTO getUserBook(Long bookId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);
        if (userBook == null) {
            throw new RuntimeException("用户书架中没有此书");
        }
        return convertToUserBookDTO(userBook);
    }

    @Transactional
    @CacheEvict(value = "userBooks", key = "'user:' + authentication.name")
    public UserBookDTO addBookToShelf(Long bookId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new RuntimeException("书籍不存在");
        }

        if (userBookMapper.findByUserIdAndBookId(user.getId(), bookId) != null) {
            throw new RuntimeException("此书已在书架中");
        }

        UserBook userBook = new UserBook();
        userBook.setUserId(user.getId());
        userBook.setBookId(bookId);
        userBook.setProgress(java.math.BigDecimal.ZERO);
        userBook.setLastReadPage(0);
        userBook.setStatus("ACTIVE");

        userBookMapper.insert(userBook);
        return convertToUserBookDTO(userBook);
    }

    @Transactional
    @CacheEvict(value = "userBooks", key = "'user:' + authentication.name")
    public void removeBookFromShelf(Long bookId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);
        if (userBook == null) {
            throw new RuntimeException("用户书架中没有此书");
        }

        userBookMapper.deleteById(userBook.getId());
    }

    @Transactional
    @CacheEvict(value = "userBooks", key = "'user:' + authentication.name")
    public UserBookDTO updateProgress(Long bookId, UpdateProgressRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);
        if (userBook == null) {
            throw new RuntimeException("用户书架中没有此书");
        }

        if (request.getProgress() != null) {
            userBook.setProgress(request.getProgress());
        }
        if (request.getLastReadPage() != null) {
            userBook.setLastReadPage(request.getLastReadPage());
        }
        userBook.setLastReadAt(java.time.LocalDateTime.now());

        userBookMapper.updateById(userBook);
        return convertToUserBookDTO(userBook);
    }

    private BookDTO convertToBookDTO(Book book) {
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getIsbn(),
                book.getCoverImage(),
                book.getDescription(),
                book.getSubject(),
                book.getGrade(),
                book.getVersion(),
                book.getStatus(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    private UserBookDTO convertToUserBookDTO(UserBook userBook) {
        Book book = userBook.getBookId() != null ? bookMapper.selectById(userBook.getBookId()) : null;
        return new UserBookDTO(
                userBook.getId(),
                book != null ? convertToBookDTO(book) : null,
                userBook.getProgress(),
                userBook.getLastReadPage(),
                userBook.getLastReadAt(),
                userBook.getStatus(),
                userBook.getCreatedAt(),
                userBook.getUpdatedAt()
        );
    }

    @Override
    public UserBookDTO getRecentBook() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String username = authentication.getName();
        if (username == null || "anonymousUser".equals(username)) {
            return null;
        }
        
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }

        List<UserBook> userBooks = userBookMapper.findByUserId(user.getId());
        if (userBooks == null || userBooks.isEmpty()) {
            return null;
        }

        UserBook recentBook = userBooks.stream()
                .filter(ub -> ub.getLastReadAt() != null)
                .max((ub1, ub2) -> ub1.getLastReadAt().compareTo(ub2.getLastReadAt()))
                .orElse(null);

        if (recentBook == null) {
            return null;
        }

        return convertToUserBookDTO(recentBook);
    }
}