package com.student.platform.qa.service;

import java.util.List;
import java.util.Map;

/**
 * 向量服务接口 - 用于文本向量化和向量检索
 */
public interface VectorService {

    /**
     * 将文本转换为向量
     * @param text 文本内容
     * @return 向量表示（float数组）
     */
    float[] embed(String text);

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 存储会话消息向量
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @param text 文本内容
     * @param role 角色（user/assistant）
     */
    void storeVector(String sessionId, String messageId, String text, String role);

    /**
     * 检索与查询最相关的历史消息
     * @param sessionId 会话ID
     * @param query 查询文本
     * @param topK 返回的最相关消息数量
     * @return 相关消息列表，按相关性排序
     */
    List<VectorSearchResult> searchSimilar(String sessionId, String query, int topK);

    /**
     * 删除会话的所有向量
     * @param sessionId 会话ID
     */
    void deleteVectors(String sessionId);

    /**
     * 搜索结果记录
     */
    record VectorSearchResult(
        String messageId,
        String content,
        String role,
        double similarity
    ) {}
}