package com.student.platform.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.common.dto.BookChapterTreeDTO;
import com.student.platform.common.dto.BookDTO;
import com.student.platform.common.dto.UpdateBookRequest;
import com.student.platform.common.dto.UpdateProgressRequest;
import com.student.platform.common.dto.UserBookDTO;
import com.student.platform.common.exception.BusinessException;
import com.student.platform.learning.entity.Book;
import com.student.platform.learning.entity.BookChapter;
import com.student.platform.common.entity.User;
import com.student.platform.learning.entity.UserBook;
import com.student.platform.learning.mapper.BookMapper;
import com.student.platform.learning.mapper.BookChapterMapper;
import com.student.platform.learning.mapper.UserBookMapper;
import com.student.platform.learning.mapper.UserMapper;
import com.student.platform.learning.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookshelfServiceImpl implements BookshelfService {

    private final BookMapper bookMapper;
    private final UserBookMapper userBookMapper;
    private final UserMapper userMapper;
    private final BookChapterMapper bookChapterMapper;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String ROLE_ADMIN = "ADMIN";
    /** 目录单次返回上限：全量 selectList 没有 LIMIT，数据量增长会直接打满内存 */
    private static final int BOOK_LIST_MAX_SIZE = 2000;

    @Override
    public List<BookDTO> getAllBooks() {
        log.debug("查询所有书籍列表");
        try {
            QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_ACTIVE);
            // 按 id 升序保证截断结果稳定：否则每次查询可能返回不同子集，
            // 叠加上前端 5 分钟缓存会让"书忽隐忽现"极难排查
            queryWrapper.orderByAsc("id");
            queryWrapper.last("LIMIT " + BOOK_LIST_MAX_SIZE);
            List<Book> books = bookMapper.selectList(queryWrapper);

            if (books.size() >= BOOK_LIST_MAX_SIZE) {
                log.warn("书籍目录已达单次返回上限 {}，其余书籍需通过搜索获取", BOOK_LIST_MAX_SIZE);
            }

            if (CollectionUtils.isEmpty(books)) {
                return Collections.emptyList();
            }

            return books.stream()
                    .map(this::convertToBookDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取书籍列表失败", e);
            throw new BusinessException("获取书籍列表失败，请稍后重试");
        }
    }

    @Override
    public BookDTO getBookById(Long id) {
        log.debug("查询书籍详情, id: {}", id);
        if (id == null || id <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }
        return convertToBookDTO(book);
    }

    /**
     * 编辑共享目录。book 表没有归属列，改动对所有用户可见，
     * 因此只允许管理员调用；普通用户想改自己看到的书，属于另一件事
     * （需要在 user_book 上做每用户覆盖）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookDTO updateBook(Long bookId, UpdateBookRequest request) {
        User current = getCurrentUser();
        if (!ROLE_ADMIN.equals(current.getRole())) {
            throw new BusinessException(403, "仅管理员可编辑书籍目录");
        }

        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        // 只覆盖请求里显式给出的字段：null 视为「本次未修改」，
        // 否则空表单会把已有作者/简介抹成 null。
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getMajor() != null) {
            book.setMajor(request.getMajor());
        }
        if (request.getSemester() != null) {
            book.setSemester(request.getSemester());
        }
        if (request.getCourseType() != null) {
            book.setCourseType(request.getCourseType());
        }
        book.setUpdatedAt(LocalDateTime.now());

        bookMapper.updateById(book);
        log.info("管理员更新书籍目录, bookId: {}, operator: {}", bookId, current.getUsername());
        return convertToBookDTO(book);
    }

    @Override
    public List<BookDTO> searchBooks(String keyword, String category, String major, String semester, String courseType) {
        log.debug("搜索书籍, keyword: {}, category: {}, major: {}, semester: {}, courseType: {}",
                keyword, category, major, semester, courseType);

        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", STATUS_ACTIVE);

        if (isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("author", keyword)
                    .or()
                    .like("major", keyword)
                    .or()
                    .like("subject", keyword)
            );
        }

        if (isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }
        if (isNotBlank(major)) {
            queryWrapper.eq("major", major);
        }
        if (isNotBlank(semester)) {
            queryWrapper.eq("semester", semester);
        }
        if (isNotBlank(courseType)) {
            queryWrapper.eq("course_type", courseType);
        }

        List<Book> books = bookMapper.selectList(queryWrapper);
        return books.stream()
                .map(this::convertToBookDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDTO> getBooksBySubject(String subject) {
        log.debug("根据学科查询书籍, subject: {}", subject);
        if (isBlank(subject)) {
            throw new BusinessException(400, "学科名称不能为空");
        }

        List<Book> books = bookMapper.findBySubject(subject);
        if (CollectionUtils.isEmpty(books)) {
            return Collections.emptyList();
        }

        return books.stream()
                .map(this::convertToBookDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBookDTO> getUserBooks() {
        log.debug("获取当前用户的书籍列表");
        User user = getCurrentUser();

        List<UserBook> userBooks = userBookMapper.findByUserId(user.getId());
        if (CollectionUtils.isEmpty(userBooks)) {
            return Collections.emptyList();
        }

        return userBooks.stream()
                .map(this::convertToUserBookDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserBookDTO getUserBook(Long bookId) {
        log.debug("获取用户书籍详情, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        User user = getCurrentUser();
        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);

        if (userBook == null) {
            throw new BusinessException(404, "用户书架中没有此书");
        }

        return convertToUserBookDTO(userBook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBookDTO addBookToShelf(Long bookId) {
        log.info("添加书籍到书架, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        User user = getCurrentUser();

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        if (userBookMapper.findByUserIdAndBookId(user.getId(), bookId) != null) {
            throw new BusinessException(409, "此书已在书架中");
        }

        UserBook userBook = new UserBook();
        userBook.setUserId(user.getId());
        userBook.setBookId(bookId);
        userBook.setProgress(BigDecimal.ZERO);
        userBook.setLastReadPage(0);
        userBook.setLastReadChapterId(null);
        userBook.setStatus(STATUS_ACTIVE);
        userBook.setCreatedAt(LocalDateTime.now());
        userBook.setUpdatedAt(LocalDateTime.now());

        userBookMapper.insert(userBook);
        log.info("成功添加书籍到书架, userId: {}, bookId: {}", user.getId(), bookId);

        return convertToUserBookDTO(userBook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBookFromShelf(Long bookId) {
        log.info("从书架移除书籍, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        User user = getCurrentUser();
        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);

        if (userBook == null) {
            throw new BusinessException(404, "用户书架中没有此书");
        }

        userBookMapper.deleteById(userBook.getId());
        log.info("成功从书架移除书籍, userId: {}, bookId: {}", user.getId(), bookId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBookDTO updateProgress(Long bookId, UpdateProgressRequest request) {
        log.info("更新阅读进度, bookId: {}, request: {}", bookId, request);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }
        if (request == null) {
            throw new BusinessException(400, "更新请求不能为空");
        }

        User user = getCurrentUser();
        UserBook userBook = userBookMapper.findByUserIdAndBookId(user.getId(), bookId);

        if (userBook == null) {
            throw new BusinessException(404, "用户书架中没有此书");
        }

        if (request.getProgress() != null) {
            BigDecimal progress = request.getProgress();
            if (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(400, "进度值必须在0-100之间");
            }
            userBook.setProgress(progress);
        }

        if (request.getLastReadPage() != null) {
            if (request.getLastReadPage() < 0) {
                throw new BusinessException(400, "阅读页数不能为负数");
            }
            userBook.setLastReadPage(request.getLastReadPage());
        }

        if (request.getLastReadChapterId() != null) {
            userBook.setLastReadChapterId(request.getLastReadChapterId());
        }

        userBook.setLastReadAt(LocalDateTime.now());
        userBook.setUpdatedAt(LocalDateTime.now());

        userBookMapper.updateById(userBook);
        log.info("成功更新阅读进度, userId: {}, bookId: {}, progress: {}",
                user.getId(), bookId, userBook.getProgress());

        return convertToUserBookDTO(userBook);
    }

    @Override
    public UserBookDTO getRecentBook() {
        log.debug("获取最近阅读的书籍");
        User user = getCurrentUserOrNull();
        if (user != null) {
            List<UserBook> userBooks = userBookMapper.findByUserId(user.getId());
            if (!CollectionUtils.isEmpty(userBooks)) {
                UserBookDTO recentBook = userBooks.stream()
                        .filter(ub -> ub.getLastReadAt() != null)
                        .max(Comparator.comparing(UserBook::getLastReadAt))
                        .map(this::convertToUserBookDTO)
                        .orElse(null);
                if (recentBook != null) {
                    return recentBook;
                }
            }
        }

        return getMockRecentBook();
    }

    private UserBookDTO getMockRecentBook() {
        log.debug("返回模拟的最近阅读书籍数据");

        Book mockBook = new Book();
        mockBook.setId(205L);
        mockBook.setTitle("高等数学（上册）");
        mockBook.setAuthor("同济大学数学系");
        mockBook.setPublisher("高等教育出版社");
        mockBook.setCoverImage("https://img9.doubanio.com/view/subject/l/public/s29969522.jpg");
        mockBook.setDescription("本书是高等院校理工科各专业学生的数学基础课程教材，内容包括函数与极限、导数与微分、微分中值定理与导数的应用、不定积分与定积分等。");
        mockBook.setSubject("数学");
        mockBook.setMajor("理工科");
        mockBook.setSemester("大一");
        mockBook.setCourseType("必修课");
        mockBook.setStatus(STATUS_ACTIVE);
        mockBook.setCreatedAt(LocalDateTime.now().minusMonths(6));
        mockBook.setUpdatedAt(LocalDateTime.now().minusMonths(1));

        UserBook mockUserBook = new UserBook();
        mockUserBook.setId(1L);
        mockUserBook.setBookId(205L);
        mockUserBook.setProgress(new BigDecimal(35));
        mockUserBook.setLastReadPage(120);
        mockUserBook.setLastReadChapterId(2L);
        mockUserBook.setLastReadAt(LocalDateTime.now().minusDays(2));
        mockUserBook.setStatus(STATUS_ACTIVE);
        mockUserBook.setCreatedAt(LocalDateTime.now().minusWeeks(2));
        mockUserBook.setUpdatedAt(LocalDateTime.now().minusDays(2));

        UserBookDTO mockUserBookDTO = new UserBookDTO();
        mockUserBookDTO.setId(mockUserBook.getId());
        mockUserBookDTO.setBook(convertToBookDTO(mockBook));
        mockUserBookDTO.setProgress(mockUserBook.getProgress());
        mockUserBookDTO.setLastReadPage(mockUserBook.getLastReadPage());
        mockUserBookDTO.setLastReadChapterId(mockUserBook.getLastReadChapterId());
        mockUserBookDTO.setLastReadAt(mockUserBook.getLastReadAt());
        mockUserBookDTO.setStatus(mockUserBook.getStatus());
        mockUserBookDTO.setCreatedAt(mockUserBook.getCreatedAt());
        mockUserBookDTO.setUpdatedAt(mockUserBook.getUpdatedAt());

        return mockUserBookDTO;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "用户未登录");
        }

        String username = authentication.getName();
        if (isBlank(username) || "anonymousUser".equals(username)) {
            throw new BusinessException(401, "用户未登录");
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        return user;
    }

    private User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (BusinessException e) {
            return null;
        }
    }

    private BookDTO convertToBookDTO(Book book) {
        if (book == null) {
            return null;
        }

        String createdAt = book.getCreatedAt() != null ? book.getCreatedAt().toString() : null;
        String updatedAt = book.getUpdatedAt() != null ? book.getUpdatedAt().toString() : null;

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
                book.getCategory(),
                book.getCourseType(),
                book.getMajor(),
                book.getSemester(),
                book.getUniversityLevel(),
                createdAt,
                updatedAt
        );
    }

    private UserBookDTO convertToUserBookDTO(UserBook userBook) {
        if (userBook == null) {
            return null;
        }

        Book book = null;
        if (userBook.getBookId() != null) {
            book = bookMapper.selectById(userBook.getBookId());
        }

        return new UserBookDTO(
                userBook.getId(),
                book != null ? convertToBookDTO(book) : null,
                userBook.getProgress(),
                userBook.getLastReadPage(),
                userBook.getLastReadChapterId(),
                userBook.getLastReadAt(),
                userBook.getStatus(),
                userBook.getCreatedAt(),
                userBook.getUpdatedAt()
        );
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    public List<BookChapter> getBookChapters(Long bookId) {
        log.debug("获取书籍章节列表, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        return bookChapterMapper.selectByBookId(bookId);
    }

    @Override
    public List<BookChapterTreeDTO> getBookChapterTree(Long bookId) {
        log.debug("获取书籍章节树结构, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        List<BookChapter> chapters = bookChapterMapper.selectByBookId(bookId);
        return buildChapterTree(chapters);
    }

    private List<BookChapterTreeDTO> buildChapterTree(List<BookChapter> chapters) {
        Map<Long, BookChapterTreeDTO> chapterMap = new HashMap<>();
        List<BookChapterTreeDTO> rootChapters = new ArrayList<>();

        for (BookChapter chapter : chapters) {
            BookChapterTreeDTO chapterDTO = new BookChapterTreeDTO();
            chapterDTO.setId(chapter.getId());
            chapterDTO.setBookId(chapter.getBookId());
            chapterDTO.setTitle(chapter.getTitle());
            chapterDTO.setSortOrder(chapter.getSortOrder());
            chapterDTO.setContent(chapter.getContent());
            chapterDTO.setParentId(chapter.getParentId());
            chapterDTO.setStatus(chapter.getStatus());
            chapterDTO.setChildren(new ArrayList<>());
            chapterMap.put(chapter.getId(), chapterDTO);
        }

        for (BookChapter chapter : chapters) {
            BookChapterTreeDTO chapterDTO = chapterMap.get(chapter.getId());
            if (chapter.getParentId() == null) {
                rootChapters.add(chapterDTO);
            } else {
                BookChapterTreeDTO parentDTO = chapterMap.get(chapter.getParentId());
                if (parentDTO != null) {
                    parentDTO.getChildren().add(chapterDTO);
                }
            }
        }

        sortChapters(rootChapters);

        return rootChapters;
    }

    private void sortChapters(List<BookChapterTreeDTO> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        chapters.sort(Comparator.comparing(BookChapterTreeDTO::getSortOrder));

        for (BookChapterTreeDTO chapter : chapters) {
            if (chapter.getChildren() != null && !chapter.getChildren().isEmpty()) {
                sortChapters(chapter.getChildren());
            }
        }
    }

    @Override
    public List<BookChapter> searchChapters(Long bookId, String keyword) {
        log.debug("搜索章节, bookId: {}, keyword: {}", bookId, keyword);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        return bookChapterMapper.searchChapters(bookId, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addBooks(Long userId, Map<String, List<BookDTO>> request) {
        log.info("批量添加教材, userId: {}, request: {}", userId, request);

        if (request == null || !request.containsKey("books")) {
            throw new BusinessException(400, "请求数据无效");
        }

        List<BookDTO> books = request.get("books");
        if (books == null || books.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的教材");
        }

        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failMessages = new ArrayList<>();

        for (BookDTO bookDTO : books) {
            try {
                if (bookDTO == null || bookDTO.getId() == null) {
                    failCount++;
                    failMessages.add("书籍数据无效");
                    continue;
                }

                Book book = bookMapper.selectById(bookDTO.getId());
                if (book == null) {
                    failCount++;
                    failMessages.add("书籍不存在: " + bookDTO.getTitle());
                    continue;
                }

                if (userBookMapper.findByUserIdAndBookId(user.getId(), bookDTO.getId()) != null) {
                    failCount++;
                    failMessages.add("书籍已在书架中: " + bookDTO.getTitle());
                    continue;
                }

                UserBook userBook = new UserBook();
                userBook.setUserId(user.getId());
                userBook.setBookId(bookDTO.getId());
                userBook.setProgress(BigDecimal.ZERO);
                userBook.setLastReadPage(0);
                userBook.setLastReadChapterId(null);
                userBook.setStatus(STATUS_ACTIVE);
                userBook.setCreatedAt(LocalDateTime.now());
                userBook.setUpdatedAt(LocalDateTime.now());

                userBookMapper.insert(userBook);
                successCount++;
                log.info("成功添加书籍到书架, userId: {}, bookId: {}", user.getId(), bookDTO.getId());
            } catch (Exception e) {
                failCount++;
                String bookTitle = bookDTO != null ? bookDTO.getTitle() : "未知书籍";
                Long bookId = bookDTO != null ? bookDTO.getId() : null;
                failMessages.add("添加失败: " + bookTitle + " - " + e.getMessage());
                log.error("添加书籍失败, bookId: {}", bookId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failMessages", failMessages);

        return result;
    }
}
