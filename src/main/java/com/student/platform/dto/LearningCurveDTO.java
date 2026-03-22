package com.student.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningCurveDTO {
    
    private List<LearningDataPoint> learningCurve;
    private double currentProgress;
    private String bookTitle;
    private String bookAuthor;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningDataPoint {
        private String date;
        private double progress;
        private int pagesRead;
        private int studyTimeMinutes;
    }
}
