package com.student.platform.service.impl;

import com.student.platform.entity.Course;
import com.student.platform.entity.QaHistory;
import com.student.platform.mapper.CourseMapper;
import com.student.platform.mapper.QaHistoryMapper;
import com.student.platform.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseRecommendServiceImplTest {

    @Mock
    private QaHistoryMapper qaHistoryMapper;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private AiService aiService;

    @InjectMocks
    private CourseRecommendServiceImpl courseRecommendService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecommendCoursesByQuestionHistory() {
        Long userId = 1L;
        Integer limit = 5;

        List<QaHistory> histories = new ArrayList<>();
        QaHistory history = new QaHistory();
        history.setId(1L);
        history.setUserId(userId);
        history.setQuestion("什么是微积分？");
        history.setCreatedAt(LocalDateTime.now());
        histories.add(history);

        when(qaHistoryMapper.findByUserId(userId)).thenReturn(histories);
        when(aiService.generateResponseWithContext(anyString(), anyString())).thenReturn("微积分,数学,积分");
        
        List<Course> courses = new ArrayList<>();
        Course course = new Course();
        course.setId(1L);
        course.setName("高等数学");
        course.setDescription("包含微积分内容");
        course.setVideoLink("https://example.com/video");
        courses.add(course);
        
        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByQuestionHistory(userId, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("高等数学", result.get(0).get("name"));
    }

    @Test
    void testRecommendCoursesByQuestionHistoryWithNoHistory() {
        Long userId = 1L;
        Integer limit = 5;

        when(qaHistoryMapper.findByUserId(userId)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByQuestionHistory(userId, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("热门推荐", result.get(0).get("recommendReason"));
    }

    @Test
    void testRecommendCoursesByKnowledge() {
        List<String> knowledgePoints = Arrays.asList("微积分", "线性代数");
        Integer limit = 5;

        List<Course> courses = new ArrayList<>();
        Course course = new Course();
        course.setId(1L);
        course.setName("高等数学");
        course.setDescription("包含微积分和线性代数");
        course.setVideoLink("https://example.com/video");
        courses.add(course);

        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByKnowledge(knowledgePoints, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testRecommendCoursesBySubject() {
        String subject = "数学";
        Integer limit = 5;

        List<Course> courses = new ArrayList<>();
        Course course = new Course();
        course.setId(1L);
        course.setName("高等数学");
        course.setDescription("数学基础课程");
        course.setVideoLink("https://example.com/video");
        courses.add(course);

        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesBySubject(subject, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testDefaultRecommendations() {
        Long userId = null;
        Integer limit = 3;

        when(qaHistoryMapper.findByUserId(any())).thenReturn(null);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByQuestionHistory(userId, limit);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("热门推荐", result.get(0).get("recommendReason"));
        
        // 验证默认推荐使用国内平台链接
        for (Map<String, Object> course : result) {
            String videoLink = (String) course.get("videoLink");
            assertNotNull(videoLink);
            assertTrue(videoLink.contains("bilibili.com"), "默认推荐应使用B站链接");
        }
    }
    
    @Test
    void testDomesticPlatformLink() {
        List<String> knowledgePoints = Arrays.asList("微积分");
        Integer limit = 1;

        List<Course> courses = new ArrayList<>();
        Course course = new Course();
        course.setId(1L);
        course.setName("高等数学");
        course.setDescription("包含微积分内容");
        course.setVideoLink("https://www.bilibili.com/video/BV1dx411S7WX");
        courses.add(course);

        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByKnowledge(knowledgePoints, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        String videoLink = (String) result.get(0).get("videoLink");
        assertNotNull(videoLink);
        assertTrue(videoLink.contains("bilibili.com"), "国内平台链接应被保留");
        assertEquals("B站", result.get(0).get("platform"), "平台信息应被正确添加");
    }
    
    @Test
    void testForeignPlatformLink() {
        List<String> knowledgePoints = Arrays.asList("微积分");
        Integer limit = 1;

        List<Course> courses = new ArrayList<>();
        Course course = new Course();
        course.setId(1L);
        course.setName("高等数学");
        course.setDescription("包含微积分内容");
        course.setVideoLink("https://www.youtube.com/watch?v=WUvTyaaNkzM");
        courses.add(course);

        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByKnowledge(knowledgePoints, limit);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        String videoLink = (String) result.get(0).get("videoLink");
        assertNotNull(videoLink);
        assertTrue(videoLink.contains("bilibili.com"), "外网链接应被替换为国内平台链接");
        assertEquals("B站", result.get(0).get("platform"), "平台信息应被正确添加");
    }
    
    @Test
    void testPlatformInfoAddition() {
        List<String> knowledgePoints = Arrays.asList("微积分");
        Integer limit = 3;

        List<Course> courses = new ArrayList<>();
        
        // B站课程
        Course bilibiliCourse = new Course();
        bilibiliCourse.setId(1L);
        bilibiliCourse.setName("高等数学");
        bilibiliCourse.setDescription("包含微积分内容");
        bilibiliCourse.setVideoLink("https://www.bilibili.com/video/BV1dx411S7WX");
        courses.add(bilibiliCourse);
        
        // 优酷课程
        Course youkuCourse = new Course();
        youkuCourse.setId(2L);
        youkuCourse.setName("线性代数");
        youkuCourse.setDescription("矩阵、行列式等内容，与微积分相关");
        youkuCourse.setVideoLink("https://www.youku.com/video/XNTk4MjA1NjY4MA==");
        courses.add(youkuCourse);
        
        // 爱奇艺课程
        Course iqiyiCourse = new Course();
        iqiyiCourse.setId(3L);
        iqiyiCourse.setName("概率论");
        iqiyiCourse.setDescription("概率分布、统计推断等内容，需要微积分基础");
        iqiyiCourse.setVideoLink("https://www.iqiyi.com/v_19rqy7q7l8.html");
        courses.add(iqiyiCourse);

        when(courseMapper.selectList(any())).thenReturn(courses);

        List<Map<String, Object>> result = courseRecommendService.recommendCoursesByKnowledge(knowledgePoints, limit);

        assertNotNull(result);
        assertEquals(3, result.size());
        
        // 验证平台信息
        assertEquals("B站", result.get(0).get("platform"));
        assertEquals("优酷", result.get(1).get("platform"));
        assertEquals("爱奇艺", result.get(2).get("platform"));
    }
}
