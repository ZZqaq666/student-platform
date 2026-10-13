package com.student.platform.qa.service.impl;

import com.student.platform.qa.service.AiService;
import com.student.platform.qa.service.SeniorAiService;
import com.student.platform.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 学长学姐人设 + 大模型回答生成实现。
 *
 * 设计要点：
 * 1. 异步执行，发布问题接口不阻塞等待大模型；
 * 2. @Async 线程不携带 RequestContext / SecurityContext，因此所有方法只接收业务ID，
 *    绝不在异步线程内解析登录态；
 * 3. 每个问题固定一位负责学长（写入 senior_question.ai_senior_id），
 *    追问时由同一位学长继续回复，形成连贯的多轮对话；
 * 4. 大模型失败时把状态置为 failed，前端可引导重试，不留下「永远转圈」的假象。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeniorAiServiceImpl implements SeniorAiService {

    /** 问题分类 → 学长人设ID（与 senior.subject 对齐） */
    private static final Map<String, String> CATEGORY_SENIOR = Map.of(
            "cs", "s-1",
            "computer", "s-101",
            "software", "s-103",
            "electronics", "s-102",
            "data", "s-104",
            "math", "s-2",
            "physics", "s-3",
            "english", "s-4"
    );

    /** 大模型不可用时的兜底文案，出现这些文案视为生成失败 */
    private static final List<String> FAILURE_TEXTS = List.of(
            "AI服务暂时不可用",
            "AI响应生成异常",
            "AI响应超时",
            "请提供有效的问题"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final AiService aiService;
    private final RedisUtil redisUtil;

    @Override
    public String matchSeniorId(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase();
        String mapped = CATEGORY_SENIOR.get(normalized);
        if (mapped != null) {
            return mapped;
        }

        List<String> bySubject = jdbcTemplate.queryForList(
                "SELECT id FROM senior WHERE subject = ? ORDER BY answer_count DESC LIMIT 1",
                String.class, normalized
        );
        if (!bySubject.isEmpty()) {
            return bySubject.get(0);
        }

        List<String> fallback = jdbcTemplate.queryForList(
                "SELECT id FROM senior WHERE certified = 1 ORDER BY answer_count DESC LIMIT 1",
                String.class
        );
        return fallback.isEmpty() ? null : fallback.get(0);
    }

    @Async("seniorAiExecutor")
    @Override
    public void generateAnswerAsync(String questionId) {
        try {
            Map<String, Object> question = loadQuestion(questionId);
            if (question == null) {
                log.warn("问题不存在，跳过AI回答生成：{}", questionId);
                return;
            }

            String category = str(question.get("category"));
            String seniorId = resolveSeniorId(questionId, question, category);
            if (seniorId == null) {
                log.error("没有可用的学长人设，问题 {} 无法生成回答", questionId);
                markQuestionStatus(questionId, STATUS_FAILED);
                return;
            }

            markQuestionStatus(questionId, STATUS_GENERATING);
            assignSenior(questionId, seniorId);

            Map<String, Object> senior = loadSenior(seniorId);
            if (senior == null) {
                markQuestionStatus(questionId, STATUS_FAILED);
                return;
            }

            String answerText = aiService.generateResponseWithContext(
                    buildPersonaContext(senior),
                    buildQuestionText(question, List.of(), null)
            );

            if (isFailureText(answerText)) {
                log.error("大模型未返回有效回答，问题 {} 生成失败：{}", questionId, answerText);
                markQuestionStatus(questionId, STATUS_FAILED);
                return;
            }

            saveAnswer(questionId, senior, answerText);
            markQuestionStatus(questionId, STATUS_DONE);
            evictQuestionCache();
            log.info("问题 {} 已由学长 {} 生成回答", questionId, senior.get("name"));
        } catch (Exception e) {
            log.error("生成学长回答失败，问题 {}：{}", questionId, e.getMessage(), e);
            try {
                markQuestionStatus(questionId, STATUS_FAILED);
            } catch (Exception ignored) {
                // 状态回写失败不影响主流程
            }
            evictQuestionCache();
        }
    }

    @Async("seniorAiExecutor")
    @Override
    public void generateFollowUpReplyAsync(String questionId, String followUpId) {
        try {
            Map<String, Object> question = loadQuestion(questionId);
            if (question == null) {
                log.warn("问题不存在，跳过追问回复生成：{}", questionId);
                return;
            }

            Map<String, Object> followUp = jdbcTemplate.queryForMap(
                    "SELECT id, content FROM senior_follow_up WHERE id = ?", followUpId
            );

            String category = str(question.get("category"));
            String seniorId = resolveSeniorId(questionId, question, category);
            if (seniorId == null) {
                markFollowUpStatus(followUpId, STATUS_FAILED);
                return;
            }

            markFollowUpStatus(followUpId, STATUS_GENERATING);

            Map<String, Object> senior = loadSenior(seniorId);
            if (senior == null) {
                markFollowUpStatus(followUpId, STATUS_FAILED);
                return;
            }

            List<Map<String, Object>> answers = jdbcTemplate.queryForList(
                    "SELECT id, author_name AS authorName, content FROM senior_answer"
                            + " WHERE question_id = ? ORDER BY created_at ASC",
                    questionId
            );

            String answerText = aiService.generateResponseWithContext(
                    buildPersonaContext(senior),
                    buildQuestionText(question, answers, str(followUp.get("content")))
            );

            if (isFailureText(answerText)) {
                log.error("大模型未返回有效追问回复，问题 {}：{}", questionId, answerText);
                markFollowUpStatus(followUpId, STATUS_FAILED);
                return;
            }

            saveAnswer(questionId, senior, answerText);
            markFollowUpStatus(followUpId, STATUS_DONE);
            markQuestionStatus(questionId, STATUS_DONE);
            evictQuestionCache();
            log.info("追问 {} 已由学长 {} 生成回复", followUpId, senior.get("name"));
        } catch (Exception e) {
            log.error("生成追问回复失败，追问 {}：{}", followUpId, e.getMessage(), e);
            try {
                markFollowUpStatus(followUpId, STATUS_FAILED);
            } catch (Exception ignored) {
                // 状态回写失败不影响主流程
            }
            evictQuestionCache();
        }
    }

    private Map<String, Object> loadQuestion(String questionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, content, category, ai_senior_id AS aiSeniorId FROM senior_question WHERE id = ?",
                questionId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 加载学长人设。
     * 注意：senior 表没有 badge 列（badge 属于 senior_answer），
     * 人设标签从 tags 里取第一个，见 {@link #firstTag(String)}。
     */
    private Map<String, Object> loadSenior(String seniorId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, grade, major, subject, tags FROM senior WHERE id = ?", seniorId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String resolveSeniorId(String questionId, Map<String, Object> question, String category) {
        String existing = str(question.get("aiSeniorId"));
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return matchSeniorId(category);
    }

    private void assignSenior(String questionId, String seniorId) {
        jdbcTemplate.update("UPDATE senior_question SET ai_senior_id = ? WHERE id = ?", seniorId, questionId);
    }

    private void markQuestionStatus(String questionId, String status) {
        jdbcTemplate.update("UPDATE senior_question SET ai_status = ? WHERE id = ?", status, questionId);
    }

    private void markFollowUpStatus(String followUpId, String status) {
        jdbcTemplate.update("UPDATE senior_follow_up SET ai_status = ? WHERE id = ?", status, followUpId);
    }

    private void saveAnswer(String questionId, Map<String, Object> senior, String content) {
        String answerId = "a-ai-" + UUID.randomUUID().toString().substring(0, 8);
        String seniorId = str(senior.get("id"));
        String badge = str(senior.get("badge"));
        if (badge == null || badge.isBlank()) {
            badge = firstTag(str(senior.get("tags")));
        }

        jdbcTemplate.update(
                "INSERT INTO senior_answer (id, question_id, author_id, author_name, certified, badge,"
                        + " content, time, likes, liked_by_current_user, senior_id, source)"
                        + " VALUES (?, ?, ?, ?, 1, ?, ?, ?, 0, 0, ?, 'ai')",
                answerId, questionId, seniorId, str(senior.get("name")),
                badge, content, LocalDateTime.now().format(TIME_FORMATTER), seniorId
        );

        jdbcTemplate.update(
                "UPDATE senior_question SET answer_count = (SELECT COUNT(*) FROM senior_answer WHERE question_id = ?)"
                        + " WHERE id = ?",
                questionId, questionId
        );
        jdbcTemplate.update(
                "UPDATE senior SET answer_count = answer_count + 1, updated_at = NOW() WHERE id = ?",
                seniorId
        );
    }

    /**
     * 构造学长人设上下文：让大模型以对应方向学长的身份作答。
     */
    private String buildPersonaContext(Map<String, Object> senior) {
        String name = str(senior.get("name"));
        String grade = str(senior.get("grade"));
        String major = str(senior.get("major"));
        String tags = str(senior.get("tags"));

        return "你现在是「" + name + "」，一位" + nullToEmpty(grade) + nullToEmpty(major)
                + "专业的学长/学姐，正在学校的「学长学姐答疑专区」给学弟学妹解答问题。\n"
                + "你擅长的领域：" + (tags == null || tags.isBlank() ? "本专业基础课与学习方法" : tags) + "\n\n"
                + "回答要求：\n"
                + "1. 用中文作答，语气像热心学长学姐：亲切、直接、不说套话\n"
                + "2. 先给结论，再展开具体步骤或思路，给出可执行的建议\n"
                + "3. 合理使用 Markdown（分点、加粗、代码块、公式），篇幅 300-500 字\n"
                + "4. 遇到概念性问题，用例子或类比帮助理解\n"
                + "5. 绝对不要提及你是人工智能、语言模型或 AI，也不要说「作为一个AI」\n"
                + "6. 不要复述问题，直接开始回答";
    }

    /**
     * 构造送给大模型的问题文本。带上学科与标题，既保证语义完整，
     * 也让缓存键（AiServiceImpl 以问题文本为缓存键）按学科区分，避免串味。
     */
    private String buildQuestionText(Map<String, Object> question,
                                     List<Map<String, Object>> answers,
                                     String followUpContent) {
        StringBuilder builder = new StringBuilder();
        builder.append("【学科分类】").append(nullToEmpty(str(question.get("category")))).append('\n');
        builder.append("【问题标题】").append(nullToEmpty(str(question.get("title")))).append('\n');
        builder.append("【问题正文】").append(nullToEmpty(str(question.get("content")))).append('\n');

        if (answers != null && !answers.isEmpty()) {
            builder.append("\n【已有的回答】\n");
            List<Map<String, Object>> limited = answers.size() > 6
                    ? new ArrayList<>(answers.subList(answers.size() - 6, answers.size()))
                    : answers;
            for (Map<String, Object> answer : limited) {
                builder.append('-').append(nullToEmpty(str(answer.get("authorName")))).append("：")
                        .append(truncate(nullToEmpty(str(answer.get("content"))), 400)).append('\n');
            }
        }

        if (followUpContent != null && !followUpContent.isBlank()) {
            builder.append("\n【学弟学妹的追问】").append(followUpContent)
                    .append("\n\n请针对这条追问作答，承接上面已有的回答，不要重复已经讲过的内容。");
        }

        return builder.toString();
    }

    private boolean isFailureText(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return FAILURE_TEXTS.stream().anyMatch(text::contains);
    }

    private void evictQuestionCache() {
        try {
            redisUtil.delete(redisUtil.keys("senior:questions:*"));
        } catch (Exception e) {
            log.warn("清除答疑列表缓存失败：{}", e.getMessage());
        }
    }

    private String firstTag(String tags) {
        if (tags == null || tags.isBlank()) {
            return "学长学姐";
        }
        return tags.split(",")[0];
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
