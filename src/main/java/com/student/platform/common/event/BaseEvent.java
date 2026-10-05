package com.student.platform.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BaseEvent implements Serializable {

    private String id;
    private String source;
    private LocalDateTime timestamp;

    public BaseEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public BaseEvent(String id, String source) {
        this.id = id;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
