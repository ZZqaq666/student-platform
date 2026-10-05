package com.student.platform.common.event;

public class ExerciseCompletedEvent extends BaseEvent {

    private Long userId;
    private Long exerciseId;
    private String subject;
    private Integer score;

    public ExerciseCompletedEvent() {}

    public ExerciseCompletedEvent(Long userId, Long exerciseId, String subject, Integer score) {
        super(String.valueOf(exerciseId), "exercise-service");
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.subject = subject;
        this.score = score;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
