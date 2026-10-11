package com.student.platform.qa.service;

/**
 * 学长学姐人设 + 大模型回答生成服务。
 *
 * 答疑专区的学长学姐是预置人设，回答由大模型扮演对应方向的学长生成并落库，
 * 从而让「提问 → 回答 → 追问 → 再回答」形成真实闭环，
 * 而不是只能看到建库时写死的种子回答。
 */
public interface SeniorAiService {

    /** AI 生成状态：未开始 */
    String STATUS_NONE = "none";
    /** AI 生成状态：生成中 */
    String STATUS_GENERATING = "generating";
    /** AI 生成状态：已完成 */
    String STATUS_DONE = "done";
    /** AI 生成状态：生成失败 */
    String STATUS_FAILED = "failed";

    /**
     * 异步为问题生成一条学长回答。
     *
     * @param questionId 问题ID（异步线程不携带登录态，只能传ID）
     */
    void generateAnswerAsync(String questionId);

    /**
     * 异步为追问生成一条学长回复（追加为一条新的回答）。
     *
     * @param questionId 问题ID
     * @param followUpId 追问ID
     */
    void generateFollowUpReplyAsync(String questionId, String followUpId);

    /**
     * 按问题分类匹配负责回答的学长人设ID。
     */
    String matchSeniorId(String category);
}
