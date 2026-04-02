package com.student.platform.service.impl;

import com.student.platform.dto.CourseRecommendationDTO;
import com.student.platform.entity.QaHistory;
import com.student.platform.mapper.QaHistoryMapper;
import com.student.platform.service.AiService;
import com.student.platform.service.CourseRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private final QaHistoryMapper qaHistoryMapper;
    private final AiService aiService;

    @Autowired
    public CourseRecommendationServiceImpl(QaHistoryMapper qaHistoryMapper, AiService aiService) {
        this.qaHistoryMapper = qaHistoryMapper;
        this.aiService = aiService;
    }

    @Override
    public List<CourseRecommendationDTO> getCourseRecommendations(Long userId) {
        try {
            // 步骤1：检索最近5个QA历史记录
            List<QaHistory> recentQaHistory = qaHistoryMapper.findRecentByUserId(userId, 5);
            
            if (recentQaHistory.isEmpty()) {
                log.warn("未找到用户的质量保证历史：{}", userId);
                return new ArrayList<>();
            }
            
            // 步骤2：将问题格式化为提示词
            String prompt = formatPrompt(recentQaHistory);
            
            // 步骤3：致电AI服务生成课程推荐
            String aiResponse = aiService.generateResponseWithContext("", prompt);
            
            // 步骤4：将AI的响应处理成预期的DTO格式
            return processAiResponse(aiResponse);
        } catch (Exception e) {
            log.error("生成课程推荐时出错：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String formatPrompt(List<QaHistory> qaHistoryList) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Based on the following recent questions from a student, recommend high-quality online courses that would help them learn the relevant topics. ");
        promptBuilder.append("For each recommended course, provide:");
        promptBuilder.append("\n- Title: The course title");
        promptBuilder.append("\n- URL: A direct link to the course");
        promptBuilder.append("\n- Description: A brief description of the course");
        promptBuilder.append("\n- Platform: The platform where the course is offered (e.g., Coursera, edX, Udemy)");
        promptBuilder.append("\n- Level: The difficulty level (e.g., Beginner, Intermediate, Advanced)");
        promptBuilder.append("\n- Rating: The course rating (0-5 stars)");
        promptBuilder.append("\n- Review Count: The number of reviews");
        promptBuilder.append("\n- Image URL: A URL to the course image");
        promptBuilder.append("\n- Category: The subject category");
        promptBuilder.append("\n\nRecent questions:");
        
        for (QaHistory qaHistory : qaHistoryList) {
            promptBuilder.append("\n- ").append(qaHistory.getQuestion());
        }
        
        return promptBuilder.toString();
    }

    private List<CourseRecommendationDTO> processAiResponse(String aiResponse) {
        List<CourseRecommendationDTO> recommendations = new ArrayList<>();
        
        // Split the response into individual course recommendations
        // This is a simple parsing approach - in a real application, you might need more sophisticated parsing
        String[] courseBlocks = aiResponse.split("\\n\\n");
        
        for (String block : courseBlocks) {
            if (block.contains("Title:") && block.contains("URL:")) {
                CourseRecommendationDTO dto = parseCourseBlock(block);
                if (dto != null) {
                    recommendations.add(dto);
                }
            }
        }
        
        return recommendations;
    }

    private CourseRecommendationDTO parseCourseBlock(String block) {
        CourseRecommendationDTO dto = new CourseRecommendationDTO();
        
        try {
            dto.setTitle(extractValue(block, "Title: (.*?)\\n"));
            dto.setUrl(extractValue(block, "URL: (.*?)\\n"));
            dto.setDescription(extractValue(block, "Description: (.*?)\\n"));
            dto.setPlatform(extractValue(block, "Platform: (.*?)\\n"));
            dto.setLevel(extractValue(block, "Level: (.*?)\\n"));
            
            String ratingStr = extractValue(block, "Rating: (.*?)\\n");
            if (ratingStr != null) {
                // Handle different rating formats (e.g., "4.5 stars", "4.5/5")
                ratingStr = ratingStr.replaceAll("[^0-9.]", "");
                dto.setRating(Double.parseDouble(ratingStr));
            }
            
            String reviewCountStr = extractValue(block, "Review Count: (.*?)\\n");
            if (reviewCountStr != null) {
                // Handle different review count formats (e.g., "1,234 reviews", "1234")
                reviewCountStr = reviewCountStr.replaceAll("[^0-9]", "");
                dto.setReviewCount(Integer.parseInt(reviewCountStr));
            }
            
            dto.setImageUrl(extractValue(block, "Image URL: (.*?)\\n"));
            dto.setCategory(extractValue(block, "Category: (.*?)\\n"));
            
            // Validate that we have at least the title and URL
            if (dto.getTitle() != null && dto.getUrl() != null) {
                return dto;
            }
        } catch (Exception e) {
            log.warn("Error parsing course block: {}", e.getMessage());
        }
        
        return null;
    }

    private String extractValue(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}