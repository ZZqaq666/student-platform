package com.student.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.dto.BookChapterTreeDTO;
import com.student.platform.dto.BookDTO;
import com.student.platform.dto.UpdateProgressRequest;
import com.student.platform.dto.UserBookDTO;
import com.student.platform.entity.Book;
import com.student.platform.entity.BookChapter;
import com.student.platform.entity.User;
import com.student.platform.entity.UserBook;
import com.student.platform.exception.BusinessException;
import com.student.platform.mapper.BookMapper;
import com.student.platform.mapper.BookChapterMapper;
import com.student.platform.mapper.UserBookMapper;
import com.student.platform.mapper.UserMapper;
import com.student.platform.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 书架服务实现类
 * <p>
 * 提供用户书架管理功能，包括书籍查询、添加、删除、进度更新等
 * 使用Spring Cache进行缓存优化
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
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

    /**
     * 获取所有可用书籍列表
     *
     * @return 书籍DTO列表
     */
    @Override
    // 暂时移除缓存，避免Redis连接问题
    // @Cacheable(value = "books", key = "'all'")
    public List<BookDTO> getAllBooks() {
        log.debug("查询所有书籍列表");
        try {
            QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", STATUS_ACTIVE);
            List<Book> books = bookMapper.selectList(queryWrapper);

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

    /**
     * 根据ID获取书籍详情
     *
     * @param id 书籍ID
     * @return 书籍DTO
     * @throws BusinessException 如果书籍不存在
     */
    @Override
    // 暂时移除缓存，避免Redis连接问题
    // @Cacheable(value = "books", key = "#id")
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
     * 搜索书籍
     *
     * @param keyword    关键词
     * @param major      专业
     * @param semester   学期
     * @param courseType 课程类型
     * @return 符合条件的书籍列表
     */
    @Override
    public List<BookDTO> searchBooks(String keyword, String category, String major, String semester, String courseType) {
        log.debug("搜索书籍, keyword: {}, category: {}, major: {}, semester: {}, courseType: {}",
                keyword, category, major, semester, courseType);

        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", STATUS_ACTIVE);

        // 关键词搜索(标题、作者、专业、学科)
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

        // 精确筛选条件
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

    /**
     * 根据学科获取书籍
     *
     * @param subject 学科名称
     * @return 书籍列表
     */
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

    /**
     * 获取当前用户的所有书籍
     *
     * @return 用户书籍列表
     * @throws BusinessException 如果用户不存在
     */
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

    /**
     * 获取用户特定书籍详情
     *
     * @param bookId 书籍ID
     * @return 用户书籍DTO
     * @throws BusinessException 如果书籍不存在或不在用户书架中
     */
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

    /**
     * 添加书籍到用户书架
     *
     * @param bookId 书籍ID
     * @return 用户书籍DTO
     * @throws BusinessException 如果书籍不存在或已在书架中
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 暂时移除缓存，避免Redis连接问题
    // @CacheEvict(value = "userBooks", allEntries = true)
    public UserBookDTO addBookToShelf(Long bookId) {
        log.info("添加书籍到书架, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        User user = getCurrentUser();

        // 检查书籍是否存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        // 检查是否已在书架中
        if (userBookMapper.findByUserIdAndBookId(user.getId(), bookId) != null) {
            throw new BusinessException(409, "此书已在书架中");
        }

        // 创建用户书籍记录
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

    /**
     * 从用户书架移除书籍
     *
     * @param bookId 书籍ID
     * @throws BusinessException 如果书籍不在书架中
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 暂时移除缓存，避免Redis连接问题
    // @CacheEvict(value = "userBooks", allEntries = true)
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

    /**
     * 更新阅读进度
     *
     * @param bookId  书籍ID
     * @param request 进度更新请求
     * @return 更新后的用户书籍DTO
     * @throws BusinessException 如果参数无效或书籍不在书架中
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 暂时移除缓存，避免Redis连接问题
    // @CacheEvict(value = "userBooks", allEntries = true)
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

        // 验证并更新进度
        if (request.getProgress() != null) {
            BigDecimal progress = request.getProgress();
            if (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(400, "进度值必须在0-100之间");
            }
            userBook.setProgress(progress);
        }

        // 验证并更新阅读页数
        if (request.getLastReadPage() != null) {
            if (request.getLastReadPage() < 0) {
                throw new BusinessException(400, "阅读页数不能为负数");
            }
            userBook.setLastReadPage(request.getLastReadPage());
        }

        // 更新最后阅读章节ID
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

    /**
     * 获取最近阅读的书籍
     *
     * @return 最近阅读的书籍，如果没有则返回模拟数据
     */
    @Override
    public UserBookDTO getRecentBook() {
        log.debug("获取最近阅读的书籍");
        User user = getCurrentUserOrNull();
        if (user != null) {
            List<UserBook> userBooks = userBookMapper.findByUserId(user.getId());
            if (!CollectionUtils.isEmpty(userBooks)) {
                // 查找最近阅读的书籍(按最后阅读时间排序)
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
        
        // 返回模拟数据
        return getMockRecentBook();
    }
    
    /**
     * 获取模拟的最近阅读书籍数据
     *
     * @return 模拟的最近阅读书籍
     */
    private UserBookDTO getMockRecentBook() {
        log.debug("返回模拟的最近阅读书籍数据");
        
        // 创建模拟书籍
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
        
        // 创建模拟用户书籍记录
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
        
        // 转换为DTO
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

    // ==================== 私有辅助方法 ====================

    /**
     * 获取当前登录用户
     *
     * @return 用户实体
     * @throws BusinessException 如果用户未登录或不存在
     */
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

    /**
     * 获取当前登录用户(可能返回null)
     *
     * @return 用户实体或null
     */
    private User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (BusinessException e) {
            return null;
        }
    }

    /**
     * 将Book实体转换为BookDTO
     *
     * @param book 书籍实体
     * @return 书籍DTO
     */
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

    /**
     * 将UserBook实体转换为UserBookDTO
     *
     * @param userBook 用户书籍实体
     * @return 用户书籍DTO
     */
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

    /**
     * 检查字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 检查字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 获取书籍的章节列表
     *
     * @param bookId 书籍ID
     * @return 章节列表
     */
    @Override
    public List<BookChapter> getBookChapters(Long bookId) {
        log.debug("获取书籍章节列表, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        // 检查书籍是否存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        return bookChapterMapper.selectByBookId(bookId);
    }

    /**
     * 获取书籍的章节树结构
     *
     * @param bookId 书籍ID
     * @return 章节树结构列表
     */
    @Override
    public List<BookChapterTreeDTO> getBookChapterTree(Long bookId) {
        log.debug("获取书籍章节树结构, bookId: {}", bookId);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        // 检查书籍是否存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }

        List<BookChapter> chapters = bookChapterMapper.selectByBookId(bookId);
        return buildChapterTree(chapters);
    }

    /**
     * 构建章节树结构
     *
     * @param chapters 章节列表
     * @return 章节树结构
     */
    private List<BookChapterTreeDTO> buildChapterTree(List<BookChapter> chapters) {
        // 构建章节映射，方便查找父章节
        Map<Long, BookChapterTreeDTO> chapterMap = new HashMap<>();
        List<BookChapterTreeDTO> rootChapters = new ArrayList<>();

        // 首先创建所有章节的DTO对象
        for (BookChapter chapter : chapters) {
            BookChapterTreeDTO chapterDTO = new BookChapterTreeDTO(chapter);
            chapterDTO.setChildren(new ArrayList<>());
            chapterMap.put(chapter.getId(), chapterDTO);
        }

        // 构建树结构
        for (BookChapter chapter : chapters) {
            BookChapterTreeDTO chapterDTO = chapterMap.get(chapter.getId());
            if (chapter.getParentId() == null) {
                // 根章节
                rootChapters.add(chapterDTO);
            } else {
                // 子章节，添加到父章节的children中
                BookChapterTreeDTO parentDTO = chapterMap.get(chapter.getParentId());
                if (parentDTO != null) {
                    parentDTO.getChildren().add(chapterDTO);
                }
            }
        }

        // 按sortOrder排序
        sortChapters(rootChapters);

        return rootChapters;
    }

    /**
     * 递归排序章节
     *
     * @param chapters 章节列表
     */
    private void sortChapters(List<BookChapterTreeDTO> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        // 按sortOrder排序
        chapters.sort(Comparator.comparing(BookChapterTreeDTO::getSortOrder));

        // 递归排序子章节
        for (BookChapterTreeDTO chapter : chapters) {
            if (chapter.getChildren() != null && !chapter.getChildren().isEmpty()) {
                sortChapters(chapter.getChildren());
            }
        }
    }

    /**
     * 搜索章节
     *
     * @param bookId  书籍ID
     * @param keyword 搜索关键词
     * @return 符合条件的章节列表
     */
    @Override
    public List<BookChapter> searchChapters(Long bookId, String keyword) {
        log.debug("搜索章节, bookId: {}, keyword: {}", bookId, keyword);
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(400, "书籍ID无效");
        }

        // 检查书籍是否存在
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
        
        // 验证请求数据
        if (request == null || !request.containsKey("books")) {
            throw new BusinessException(400, "请求数据无效");
        }
        
        List<BookDTO> books = request.get("books");
        if (books == null || books.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的教材");
        }
        
        // 验证userId
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }
        
        // 根据userId获取用户
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
                
                // 检查书籍是否存在
                Book book = bookMapper.selectById(bookDTO.getId());
                if (book == null) {
                    failCount++;
                    failMessages.add("书籍不存在: " + bookDTO.getTitle());
                    continue;
                }
                
                // 检查是否已在书架中
                if (userBookMapper.findByUserIdAndBookId(user.getId(), bookDTO.getId()) != null) {
                    failCount++;
                    failMessages.add("书籍已在书架中: " + bookDTO.getTitle());
                    continue;
                }
                
                // 创建用户书籍记录
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
