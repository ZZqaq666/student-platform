package com.student.platform.service.impl;

import com.student.platform.service.AiService;
import com.student.platform.dto.QaRequest;
import com.student.platform.dto.QaResponse;
import com.student.platform.entity.Book;
import com.student.platform.entity.Course;
import com.student.platform.entity.ExamHistory;
import com.student.platform.entity.ExamKeyPoint;
import com.student.platform.entity.ExamSubject;
import com.student.platform.entity.KnowledgeNode;
import com.student.platform.entity.QaHistory;
import com.student.platform.entity.User;
import com.student.platform.mapper.BookMapper;
import com.student.platform.mapper.CourseMapper;
import com.student.platform.mapper.ExamHistoryMapper;
import com.student.platform.mapper.ExamKeyPointMapper;
import com.student.platform.mapper.ExamSubjectMapper;
import com.student.platform.mapper.KnowledgeNodeMapper;
import com.student.platform.mapper.QaHistoryMapper;
import com.student.platform.mapper.UserMapper;
import com.student.platform.service.QaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class QaServiceImpl implements QaService {

    private final AiService aiService;
    private final QaHistoryMapper qaHistoryMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final CourseMapper courseMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ExamSubjectMapper examSubjectMapper;
    private final ExamKeyPointMapper examKeyPointMapper;
    private final ExamHistoryMapper examHistoryMapper;

    public QaResponse askQuestion(QaRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("用户 {} 提问: {}", username, request.getQuestion());
            
            User user = userMapper.findByUsername(username);
            if (user == null) {
                log.error("用户 {} 不存在", username);
                throw new RuntimeException("用户不存在");
            }

            String context = buildSystemPrompt(request);
            String answer = aiService.generateResponseWithContext(context, request.getQuestion());
            log.info("AI 生成回答成功");

            QaHistory history = new QaHistory();
            history.setUserId(user.getId());
            history.setQuestion(request.getQuestion());
            history.setAnswer(answer);
            
            if (request.getBookId() != null) {
                history.setBookId(request.getBookId());
            }
            if (request.getKnowledgeNodeId() != null) {
                history.setKnowledgeNodeId(request.getKnowledgeNodeId());
            }
            history.setQuestionType("PROFESSIONAL");
            history.setContext(request.getSubject());

            qaHistoryMapper.insert(history);
            log.info("问答历史保存成功，ID: {}", history.getId());

            return convertToResponse(history);
        } catch (Exception e) {
            log.error("提问处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("提问处理失败，请稍后重试");
        }
    }

    public List<QaResponse> getHistory() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("用户 {} 查询历史记录", username);
            
            User user = userMapper.findByUsername(username);
            if (user == null) {
                log.error("用户 {} 不存在", username);
                throw new RuntimeException("用户不存在");
            }

            List<QaHistory> histories = qaHistoryMapper.findByUserId(user.getId());
            log.info("获取到 {} 条历史记录", histories.size());
            return histories.stream().map(this::convertToResponse).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取历史记录失败，请稍后重试");
        }
    }

    public void deleteHistory(Long id) {
        try {
            log.info("删除历史记录，ID: {}", id);
            qaHistoryMapper.deleteById(id);
            log.info("历史记录删除成功，ID: {}", id);
        } catch (Exception e) {
            log.error("删除历史记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除历史记录失败，请稍后重试");
        }
    }

    private String buildSystemPrompt(QaRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的学科问答助手，请根据用户的问题提供详细、准确的回答。");

        if (request.getSubject() != null) {
            prompt.append("\n学科：").append(request.getSubject());
        }

        return prompt.toString();
    }

    private QaResponse convertToResponse(QaHistory history) {
        return new QaResponse(
                history.getId(),
                history.getQuestion(),
                history.getAnswer(),
                history.getContext(),
                history.getBookId(),
                history.getKnowledgeNodeId(),
                history.getCreatedAt()
        );
    }

    public List<Map<String, Object>> getExamSubjects() {
        try {
            log.info("获取考试科目");
            List<ExamSubject> examSubjects = examSubjectMapper.selectList(null);
            List<Map<String, Object>> subjects = examSubjects.stream()
                .map(subject -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", subject.getId());
                    map.put("name", subject.getName());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 个考试科目", subjects.size());
            return subjects;
        } catch (Exception e) {
            log.error("获取考试科目失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取考试科目失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getKeyPoints(String subjectId) {
        try {
            log.info("获取知识点，科目ID: {}", subjectId);
            List<ExamKeyPoint> examKeyPoints = examKeyPointMapper.selectBySubjectId(subjectId);
            
            Map<String, Object> subjectKeyPoints = new HashMap<>();
            
            // 获取科目名称
            ExamSubject subject = examSubjectMapper.selectById(subjectId);
            String subjectName = subject != null ? subject.getName() : "未知科目";
            
            List<String> keyPointNames = examKeyPoints.stream()
                .map(ExamKeyPoint::getName)
                .collect(Collectors.toList());
            
            subjectKeyPoints.put("subject", subjectName);
            subjectKeyPoints.put("keyPoints", keyPointNames);
            
            List<Map<String, Object>> keyPoints = new ArrayList<>();
            keyPoints.add(subjectKeyPoints);
            
            log.info("获取到 {} 个知识点", keyPointNames.size());
            return keyPoints;
        } catch (Exception e) {
            log.error("获取知识点失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取知识点失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getExamHistory() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("用户 {} 查询考试历史", username);
            
            User user = userMapper.findByUsername(username);
            if (user == null) {
                log.error("用户 {} 不存在", username);
                throw new RuntimeException("用户不存在");
            }
            
            List<ExamHistory> histories = examHistoryMapper.selectByUserId(user.getId());
            List<Map<String, Object>> examHistory = histories.stream()
                .map(history -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", history.getId());
                    map.put("subject", history.getSubject());
                    map.put("score", history.getScore());
                    map.put("date", history.getExamDate());
                    map.put("duration", history.getDuration());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 条考试历史记录", examHistory.size());
            return examHistory;
        } catch (Exception e) {
            log.error("获取考试历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取考试历史失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getBooks() {
        try {
            log.info("获取书籍列表");
            List<Book> books = bookMapper.selectList(null);
            if (books == null) {
                log.info("书籍列表为空");
                return new ArrayList<>();
            }
            List<Map<String, Object>> bookList = books.stream()
                .map(book -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", book.getId());
                    map.put("name", book.getTitle());
                    map.put("author", book.getAuthor());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("获取到 {} 本书籍", bookList.size());
            return bookList;
        } catch (Exception e) {
            log.error("获取书籍列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取书籍列表失败，请稍后重试");
        }
    }

    public List<Map<String, Object>> getCourses() {
        try {
            log.info("获取课程列表");
            List<Course> courses = courseMapper.selectList(null);
            List<Map<String, Object>> courseList = new ArrayList<>();
            
            if (courses != null) {
                courseList = courses.stream()
                    .map(course -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", course.getId());
                        map.put("name", course.getName());
                        map.put("desc", course.getDescription());
                        map.put("code", course.getCode());
                        map.put("videoLink", course.getVideoLink());
                        return map;
                    })
                    .collect(Collectors.toList());
            }
            
            // 如果数据库中没有课程数据，返回默认数据
            if (courseList.isEmpty()) {
                Map<String, Object> defaultCourse = new HashMap<>();
                defaultCourse.put("id", 1);
                defaultCourse.put("name", "网课推荐");
                defaultCourse.put("desc", "综合推荐 — 待");
                defaultCourse.put("code", "#36CFCC");
                defaultCourse.put("videoLink", "https://example.com/video1");
                courseList.add(defaultCourse);
            }
            
            log.info("获取到 {} 个课程", courseList.size());
            return courseList;
        } catch (Exception e) {
            log.error("获取课程列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取课程列表失败，请稍后重试");
        }
    }
}