package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.Result;
import com.student.platform.service.CourseRecommendService;
import com.student.platform.service.QaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class QaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QaService qaService;

    @MockBean
    private CourseRecommendService courseRecommendService;

    @Test
    public void testSaveProcessedText() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("question", "测试问题");
        request.put("processedText", "测试回答");
        request.put("subject", "数学");
        request.put("userId", 1L);

        MvcResult result = mockMvc.perform(post("/qa/save-processed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertTrue(response.contains("code"));
    }

    @Test
    public void testSaveProcessedTextWithMissingParams() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("question", "测试问题");
        // 缺少 processedText

        MvcResult result = mockMvc.perform(post("/qa/save-processed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("不能为空") || response.contains("error"));
    }

    @Test
    public void testGetRecommendedCourses() throws Exception {
        List<Map<String, Object>> mockCourses = new ArrayList<>();
        Map<String, Object> course = new HashMap<>();
        course.put("id", 1L);
        course.put("name", "高等数学");
        course.put("desc", "高等数学课程");
        course.put("videoLink", "https://example.com/video");
        course.put("recommendReason", "热门推荐");
        mockCourses.add(course);

        Mockito.when(courseRecommendService.recommendCoursesByQuestionHistory(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(mockCourses);

        MvcResult result = mockMvc.perform(get("/qa/courses/recommend")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertTrue(response.contains("高等数学") || response.contains("data"));
    }

    @Test
    public void testGetHistory() throws Exception {
        Mockito.when(qaService.getHistory(Mockito.anyLong()))
                .thenReturn(new ArrayList<>());

        MvcResult result = mockMvc.perform(get("/qa/history"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    public void testGetCourses() throws Exception {
        List<Map<String, Object>> mockCourses = new ArrayList<>();
        Map<String, Object> course = new HashMap<>();
        course.put("id", 1L);
        course.put("name", "测试课程");
        mockCourses.add(course);

        Mockito.when(qaService.getCourses()).thenReturn(mockCourses);

        MvcResult result = mockMvc.perform(get("/qa/courses"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
    }
}
