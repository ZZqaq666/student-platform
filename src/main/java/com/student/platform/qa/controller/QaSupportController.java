package com.student.platform.qa.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.student.platform.common.result.Result;
import com.student.platform.qa.entity.ExamKeyPoint;
import com.student.platform.qa.entity.ExamSubject;
import com.student.platform.qa.entity.QaBook;
import com.student.platform.qa.entity.QaBookChapter;
import com.student.platform.qa.mapper.ExamKeyPointMapper;
import com.student.platform.qa.mapper.ExamSubjectMapper;
import com.student.platform.qa.mapper.QaBookChapterMapper;
import com.student.platform.qa.mapper.QaBookMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * QA 问答中心支撑接口：书籍 / 章节 / 考试科目 / 考点。
 *
 * 原微服务架构下这些数据由 knowledge/learning 服务提供，单体合并后统一在此暴露，
 * 供前端问答页"选择教材提问"与"考试准备"面板使用。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/qa")
@Tag(name = "QA 支撑接口", description = "书籍、章节、考试科目、考点查询")
public class QaSupportController {

    private final QaBookMapper qaBookMapper;
    private final QaBookChapterMapper qaBookChapterMapper;
    private final ExamSubjectMapper examSubjectMapper;
    private final ExamKeyPointMapper examKeyPointMapper;

    @GetMapping("/books")
    @Operation(summary = "获取可提问的教材列表")
    public Result<List<QaBook>> getBooks() {
        QueryWrapper<QaBook> qw = new QueryWrapper<>();
        qw.eq("status", "ACTIVE")
          .select("id", "title", "author", "publisher", "cover_image", "subject", "description");
        return Result.success(qaBookMapper.selectList(qw));
    }

    @GetMapping("/books/{bookId}/chapters")
    @Operation(summary = "获取教材章节列表")
    public Result<List<QaBookChapter>> getChapters(@PathVariable Long bookId) {
        QueryWrapper<QaBookChapter> qw = new QueryWrapper<>();
        qw.eq("book_id", bookId)
          .orderByAsc("sort_order");
        return Result.success(qaBookChapterMapper.selectList(qw));
    }

    @GetMapping("/exam/subjects")
    @Operation(summary = "获取考试科目列表")
    public Result<List<ExamSubject>> getExamSubjects() {
        QueryWrapper<ExamSubject> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        return Result.success(examSubjectMapper.selectList(qw));
    }

    @GetMapping("/exam/subjects/{subjectId}/key-points")
    @Operation(summary = "获取指定科目的考点列表")
    public Result<List<ExamKeyPoint>> getKeyPoints(@PathVariable String subjectId) {
        QueryWrapper<ExamKeyPoint> qw = new QueryWrapper<>();
        qw.eq("subject_id", subjectId)
          .orderByAsc("order_num");
        return Result.success(examKeyPointMapper.selectList(qw));
    }

    @GetMapping("/exam/subjects/{subjectId}/papers")
    @Operation(summary = "获取指定科目的试卷列表", description = "试卷由 AI 按需生成，此接口返回最近生成的试卷；暂无数据时返回空列表")
    public Result<List<Object>> getExamPapers(@PathVariable String subjectId) {
        // 试卷数据由 AI 动态生成（见 ai-generate 流程），暂无持久化试卷表，返回空列表避免前端报错
        return Result.success(Collections.emptyList());
    }

    @GetMapping("/courses")
    @Operation(summary = "获取课程列表")
    public Result<List<Object>> getCourses() {
        // course 表暂无数据源接入，返回空列表
        return Result.success(Collections.emptyList());
    }
}
