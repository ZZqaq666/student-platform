package com.student.platform.service;

import com.student.platform.dto.BookRecommendationDTO;
import com.student.platform.dto.KnowledgeGraphDTO;
import com.student.platform.dto.SseMessage;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface AiService {
    String generateResponseWithContext(String context, String question);
    Flux<SseMessage> generateStreamResponseWithContext(String context, String question);
    String generateResponseFromImage(MultipartFile image, String question);
    Flux<SseMessage> generateStreamResponseFromImage(MultipartFile image, String question);
    BookRecommendationDTO getBookRecommendations(Long bookId);
    KnowledgeGraphDTO generateKnowledgeGraph(Long bookId);
    String generateExercises(String knowledgePoint, String difficulty);
}