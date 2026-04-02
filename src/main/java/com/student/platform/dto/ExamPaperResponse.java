package com.student.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// 试卷包装类
public class ExamPaperResponse {
    @JsonProperty("examPapers")
    private List<ExamPaper> examPapers;
    
    // Getter和Setter
    public List<ExamPaper> getExamPapers() {
        return examPapers;
    }
    
    public void setExamPapers(List<ExamPaper> examPapers) {
        this.examPapers = examPapers;
    }
}