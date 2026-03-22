package com.student.platform.service;

import com.student.platform.dto.SseMessage;
import reactor.core.publisher.Flux;

public interface AiService {
    String generateResponseWithContext(String context, String question);
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question);
}