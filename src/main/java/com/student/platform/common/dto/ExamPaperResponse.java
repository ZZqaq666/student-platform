package com.student.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ExamPaperResponse {
    @JsonProperty("examPapers")
    private List<ExamPaper> examPapers;

    public List<ExamPaper> getExamPapers() {
        return examPapers;
    }

    public void setExamPapers(List<ExamPaper> examPapers) {
        this.examPapers = examPapers;
    }
}
