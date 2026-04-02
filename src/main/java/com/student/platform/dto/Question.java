package com.student.platform.dto;

import java.util.List;

// 题目类
public class Question {
    private String id;
    private String type;
    private String content;
    private String question;
    private List<String> options;
    private Object correctAnswer;
    private String analysis;
    private int score;
    
    // Getter和Setter
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public List<String> getOptions() {
        return options;
    }
    
    public void setOptions(List<String> options) {
        this.options = options;
    }
    
    public Object getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(Object correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
}