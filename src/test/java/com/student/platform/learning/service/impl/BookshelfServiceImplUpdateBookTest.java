package com.student.platform.learning.service.impl;

import com.student.platform.common.dto.UpdateBookRequest;
import com.student.platform.common.entity.User;
import com.student.platform.common.exception.BusinessException;
import com.student.platform.learning.entity.Book;
import com.student.platform.learning.mapper.BookChapterMapper;
import com.student.platform.learning.mapper.BookMapper;
import com.student.platform.learning.mapper.UserBookMapper;
import com.student.platform.learning.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 共享目录编辑的权限门禁与部分更新语义。
 *
 * book 表没有归属列，任何一次编辑都对所有用户生效，
 * 所以「只有管理员能改」必须由服务端守住，不能只靠前端隐藏按钮。
 */
class BookshelfServiceImplUpdateBookTest {

    private BookMapper bookMapper;
    private UserMapper userMapper;
    private BookshelfServiceImpl service;

    @BeforeEach
    void setUp() {
        bookMapper = mock(BookMapper.class);
        userMapper = mock(UserMapper.class);
        service = new BookshelfServiceImpl(
                bookMapper, mock(UserBookMapper.class), userMapper, mock(BookChapterMapper.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, String role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus("ACTIVE");
        when(userMapper.findByUsername(username)).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private Book existingBook() {
        Book book = new Book();
        book.setId(42L);
        book.setTitle("高等数学");
        book.setAuthor("同济大学");
        book.setDescription("原简介");
        book.setPublisher("高等教育出版社");
        book.setMajor("数学");
        book.setSemester("大一上");
        book.setCourseType("必修");
        book.setStatus("ACTIVE");
        return book;
    }

    @Test
    @DisplayName("普通用户编辑目录返回 403，且不落库")
    void rejectsNonAdmin() {
        loginAs("student01", "STUDENT");

        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("被我改掉的书名");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateBook(42L, request));
        assertEquals(403, ex.getCode());
        verifyNoInteractions(bookMapper);
    }

    @Test
    @DisplayName("未登录返回 401")
    void requiresAuthentication() {
        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("x");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateBook(42L, request));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("管理员可编辑，未提供的字段保持原值")
    void adminUpdatesOnlyProvidedFields() {
        loginAs("admin01", "ADMIN");
        when(bookMapper.selectById(42L)).thenReturn(existingBook());

        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("线性代数");
        // author / description / major / semester / courseType 都不传

        service.updateBook(42L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(Book.class);
        verify(bookMapper).updateById(captor.capture());
        Book saved = captor.getValue();
        assertEquals("线性代数", saved.getTitle());
        assertEquals("同济大学", saved.getAuthor(), "未提交的字段不能被清空");
        assertEquals("原简介", saved.getDescription());
        assertEquals("必修", saved.getCourseType());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("书籍不存在返回 404")
    void rejectsUnknownBook() {
        loginAs("admin01", "ADMIN");
        when(bookMapper.selectById(any())).thenReturn(null);

        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("不存在的书");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateBook(99L, request));
        assertEquals(404, ex.getCode());
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    @DisplayName("非法 ID 直接拒绝，不查库")
    void rejectsInvalidId() {
        loginAs("admin01", "ADMIN");

        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("x");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateBook(0L, request));
        assertEquals(400, ex.getCode());
        verify(bookMapper, never()).selectById(any());
    }
}
