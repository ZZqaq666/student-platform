package com.student.platform.dto;

import java.util.List;

// 试卷类
public class ExamPaper {
    private String id;
    private String year;
    private String paperName;
    private String subjectId;
    private String subjectName;
    private int duration;
    private int totalScore;
    private int questionCount;
    private int difficulty;
    private String recommendReason;
    private String source;
    private List<Question> questions;
    
    // Getter和Setter
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public String getPaperName() {
        return paperName;
    }
    
    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }
    
    public String getSubjectId() {
        return subjectId;
    }
    
    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
    
    public int getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getRecommendReason() {
        return recommendReason;
    }
    
    public void setRecommendReason(String recommendReason) {
        this.recommendReason = recommendReason;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public List<Question> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}