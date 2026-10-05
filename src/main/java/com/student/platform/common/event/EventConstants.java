package com.student.platform.common.event;

public class EventConstants {

    public static final String EXCHANGE_DIRECT = "student.direct";
    public static final String EXCHANGE_TOPIC = "student.topic";
    public static final String EXCHANGE_FANOUT = "student.fanout";

    public static final String QUEUE_USER_CREATED = "student.user.created";
    public static final String QUEUE_QA_ANSWERED = "student.qa.answered";
    public static final String QUEUE_EXERCISE_COMPLETED = "student.exercise.completed";
    public static final String QUEUE_KNOWLEDGE_UPDATED = "student.knowledge.updated";

    public static final String ROUTING_KEY_USER_CREATED = "user.created";
    public static final String ROUTING_KEY_QA_ANSWERED = "qa.answered";
    public static final String ROUTING_KEY_EXERCISE_COMPLETED = "exercise.completed";
    public static final String ROUTING_KEY_KNOWLEDGE_UPDATED = "knowledge.updated";

    private EventConstants() {}
}
