package com.student.platform.qa.service.impl;

import com.student.platform.qa.service.VectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量服务实现 - 使用内存存储（演示版本）
 * 生产环境建议使用 Milvus、Pinecone 等专业向量数据库
 */
@Slf4j
@Service
public class VectorServiceImpl implements VectorService {

    /**
     * 向量维度
     */
    @Value("${vector.dimensions:384}")
    private int dimensions;

    /**
     * 存储向量数据：sessionId -> List<VectorEntry>
     */
    private final Map<String, List<VectorEntry>> vectorStore = new ConcurrentHashMap<>();

    /**
     * 伪随机数生成器（用于模拟向量）
     */
    private final Random random = new Random(42);

    @PostConstruct
    public void init() {
        log.info("向量服务初始化完成，向量维度：{}", dimensions);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[dimensions];
        }
        
        // 使用文本的哈希值作为种子生成确定性的向量
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            int seed = Arrays.hashCode(hash);
            Random seededRandom = new Random(seed);
            
            float[] vector = new float[dimensions];
            for (int i = 0; i < dimensions; i++) {
                vector[i] = seededRandom.nextFloat() * 2 - 1; // [-1, 1]
            }
            // 归一化
            return normalize(vector);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            return generateRandomVector();
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    @Override
    public void storeVector(String sessionId, String messageId, String text, String role) {
        float[] vector = embed(text);
        VectorEntry entry = new VectorEntry(messageId, text, role, vector);
        
        vectorStore.compute(sessionId, (key, entries) -> {
            if (entries == null) {
                entries = new ArrayList<>();
            }
            // 移除相同messageId的旧条目
            entries.removeIf(e -> e.messageId.equals(messageId));
            entries.add(entry);
            return entries;
        });
        
        log.debug("向量已存储，会话ID: {}, 消息ID: {}", sessionId, messageId);
    }

    @Override
    public List<VectorSearchResult> searchSimilar(String sessionId, String query, int topK) {
        List<VectorEntry> entries = vectorStore.get(sessionId);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        float[] queryVector = embed(query);
        List<ScoredEntry> scoredEntries = new ArrayList<>();

        for (VectorEntry entry : entries) {
            double similarity = cosineSimilarity(queryVector, entry.vector);
            scoredEntries.add(new ScoredEntry(entry, similarity));
        }

        // 按相似度降序排序
        scoredEntries.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 取topK
        List<VectorSearchResult> results = new ArrayList<>();
        int count = Math.min(topK, scoredEntries.size());
        for (int i = 0; i < count; i++) {
            ScoredEntry scored = scoredEntries.get(i);
            results.add(new VectorSearchResult(
                scored.entry.messageId,
                scored.entry.text,
                scored.entry.role,
                scored.similarity
            ));
        }

        log.debug("向量检索完成，会话ID: {}, 查询: {}, 结果数: {}", sessionId, 
            query.length() > 20 ? query.substring(0, 20) + "..." : query, results.size());
        
        return results;
    }

    @Override
    public void deleteVectors(String sessionId) {
        vectorStore.remove(sessionId);
        log.debug("会话向量已删除，会话ID: {}", sessionId);
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 归一化向量
     */
    private float[] normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm == 0) {
            return vector;
        }
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    /**
     * 生成随机向量
     */
    private float[] generateRandomVector() {
        float[] vector = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }
        return normalize(vector);
    }

    /**
     * 向量条目
     */
    private static class VectorEntry {
        final String messageId;
        final String text;
        final String role;
        final float[] vector;

        VectorEntry(String messageId, String text, String role, float[] vector) {
            this.messageId = messageId;
            this.text = text;
            this.role = role;
            this.vector = vector;
        }
    }

    /**
     * 带分数的条目
     */
    private static class ScoredEntry {
        final VectorEntry entry;
        final double similarity;

        ScoredEntry(VectorEntry entry, double similarity) {
            this.entry = entry;
            this.similarity = similarity;
        }
    }
}