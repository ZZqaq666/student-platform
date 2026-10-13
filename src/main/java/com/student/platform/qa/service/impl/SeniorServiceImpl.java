package com.student.platform.qa.service.impl;

import com.student.platform.common.exception.BusinessException;
import com.student.platform.qa.service.SeniorAiService;
import com.student.platform.qa.service.SeniorService;
import com.student.platform.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 学长学姐答疑服务。
 *
 * 与旧实现的关键差异（旧实现是一套「假闭环」）：
 * 1. 发布问题 / 提交追问不再把作者硬编码为 'u-current' / '我'，改为绑定真实登录用户；
 * 2. 发布后异步调用大模型生成学长回答，追问也会触发同一位学长继续回复；
 * 3. 点赞写入 senior_answer_like，按用户维度判断是否点过赞，不再是全局标志位；
 * 4. 采纳回答校验提问人身份，并回写学长答疑次数与评分；
 * 5. 列表支持学科过滤，并返回真实分页元信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeniorServiceImpl implements SeniorService {

    /** 学长「在线」判定窗口：最近 N 小时内有答疑活动 */
    private static final int ONLINE_WINDOW_HOURS = 168;

    private static final DateTimeFormatter FOLLOW_UP_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final RedisUtil redisUtil;
    private final SeniorAiService seniorAiService;

    @Override
    public Map<String, Object> getStats() {
        Integer seniorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior WHERE updated_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)",
                Integer.class, ONLINE_WINDOW_HOURS);
        Integer totalQuestions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_question", Integer.class);
        Integer solvedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_question WHERE solved = 1", Integer.class);
        Integer totalAnswers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_answer", Integer.class);

        int safeTotal = totalQuestions == null ? 0 : totalQuestions;
        int safeSolved = solvedCount == null ? 0 : solvedCount;
        int solvedRate = safeTotal > 0 ? (int) Math.round(safeSolved * 100.0 / safeTotal) : 0;

        Double avgRating = jdbcTemplate.queryForObject(
                "SELECT AVG(rating) FROM senior WHERE rating > 0", Double.class);

        Map<String, Object> stats = new HashMap<>();
        stats.put("seniorCount", seniorCount == null ? 0 : seniorCount);
        stats.put("totalQuestions", safeTotal);
        stats.put("totalAnswers", totalAnswers == null ? 0 : totalAnswers);
        stats.put("solvedRate", solvedRate);
        stats.put("avgRating", avgRating == null ? 0.0 : Math.round(avgRating * 10) / 10.0);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getSeniors() {
        String sql = "SELECT id, name, grade, major, subject, tags,"
                + " answer_count AS answerCount, rating, certified,"
                + " (updated_at >= DATE_SUB(NOW(), INTERVAL " + ONLINE_WINDOW_HOURS + " HOUR)) AS online,"
                + " DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS lastActiveAt"
                + " FROM senior ORDER BY answer_count DESC";
        List<Map<String, Object>> seniors = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> senior : seniors) {
            senior.put("tags", splitTags(senior.get("tags")));
        }
        return seniors;
    }

    @Override
    public Map<String, Object> getSeniorById(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, grade, major, subject, tags,"
                        + " answer_count AS answerCount, rating, certified,"
                        + " (updated_at >= DATE_SUB(NOW(), INTERVAL " + ONLINE_WINDOW_HOURS + " HOUR)) AS online,"
                        + " DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS lastActiveAt"
                        + " FROM senior WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "学长学姐不存在");
        }
        Map<String, Object> senior = rows.get(0);
        senior.put("tags", splitTags(senior.get("tags")));
        return senior;
    }

    @Override
    public Map<String, Object> getQuestions(String seniorId, String category, String filter, String sort,
                                            int page, int pageSize, Long currentUserId) {
        String cacheKey = buildQuestionsCacheKey(seniorId, category, filter, sort, page, pageSize);

        List<Map<String, Object>> questions = readCache(cacheKey);
        if (questions == null) {
            questions = queryQuestions(seniorId, category, filter, sort, page, pageSize);
            writeCache(cacheKey, questions);
        } else {
            questions = copyQuestions(questions);
        }

        int total = queryQuestionsCount(seniorId, category, filter);
        markLikedAnswers(questions, currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", questions);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> getQuestionById(String id, Long currentUserId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, content, category, author_id AS authorId, author,"
                        + " DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS createdAt, views, likes, solved,"
                        + " accepted_answer_id AS acceptedAnswerId, images,"
                        + " ai_status AS aiStatus, ai_senior_id AS aiSeniorId"
                        + " FROM senior_question WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "问题不存在");
        }

        Map<String, Object> question = rows.get(0);
        jdbcTemplate.update("UPDATE senior_question SET views = views + 1 WHERE id = ?", id);
        question.put("views", toInt(question.get("views")) + 1);

        question.put("mentorIds", loadMentorIds(id));
        question.put("answers", loadAnswers(id));
        question.put("followUps", loadFollowUps(id));
        question.put("tags", loadTagNames(id));
        markLikedAnswers(List.of(question), currentUserId);
        return question;
    }

    @Override
    public Map<String, Object> publishQuestion(Map<String, Object> data, Long userId, String displayName) {
        if (data == null) {
            throw new BusinessException(400, "请求数据不能为空");
        }
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        String title = requireText(data.get("title"), "问题标题不能为空");
        String content = requireText(data.get("content"), "问题内容不能为空");
        String category = requireText(data.get("category"), "问题分类不能为空");
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        if (content.length() > 4000) {
            content = content.substring(0, 4000);
        }

        String authorName = displayName == null || displayName.isBlank() ? "同学" : displayName;
        String questionId = "q-" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.update(
                "INSERT INTO senior_question (id, title, content, category, author_id, author,"
                        + " created_at, created_at_ms, ai_status, ai_senior_id)"
                        + " VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, 'generating', ?)",
                questionId, title, content, category, String.valueOf(userId), authorName,
                System.currentTimeMillis(), seniorAiService.matchSeniorId(category)
        );

        saveTags(questionId, data.get("tags"));

        evictQuestionsCache();
        // 异步生成学长回答：不阻塞发帖响应
        seniorAiService.generateAnswerAsync(questionId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", questionId);
        result.put("title", title);
        result.put("content", content);
        result.put("category", category);
        result.put("authorId", String.valueOf(userId));
        result.put("author", authorName);
        result.put("aiStatus", SeniorAiService.STATUS_GENERATING);
        return result;
    }

    @Override
    public Map<String, Object> likeAnswer(String answerId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_answer WHERE id = ?", Integer.class, answerId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "回答不存在");
        }

        Integer liked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_answer_like WHERE answer_id = ? AND user_id = ?",
                Integer.class, answerId, userId);
        boolean alreadyLiked = liked != null && liked > 0;

        if (alreadyLiked) {
            jdbcTemplate.update(
                    "DELETE FROM senior_answer_like WHERE answer_id = ? AND user_id = ?", answerId, userId);
            jdbcTemplate.update(
                    "UPDATE senior_answer SET likes = GREATEST(likes - 1, 0) WHERE id = ?", answerId);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO senior_answer_like (id, answer_id, user_id) VALUES (?, ?, ?)"
                            + " ON DUPLICATE KEY UPDATE id = id",
                    "l-" + UUID.randomUUID().toString().substring(0, 8), answerId, userId);
            jdbcTemplate.update("UPDATE senior_answer SET likes = likes + 1 WHERE id = ?", answerId);
        }

        evictQuestionsCache();

        Integer likes = jdbcTemplate.queryForObject(
                "SELECT likes FROM senior_answer WHERE id = ?", Integer.class, answerId);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", !alreadyLiked);
        result.put("likes", likes == null ? 0 : likes);
        return result;
    }

    @Override
    public void acceptAnswer(String questionId, String answerId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, author_id AS authorId, solved FROM senior_question WHERE id = ?", questionId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "问题不存在");
        }

        String authorId = String.valueOf(rows.get(0).get("authorId"));
        if (!String.valueOf(userId).equals(authorId)) {
            throw new BusinessException(403, "只有提问者本人可以采纳回答");
        }
        if (toInt(rows.get(0).get("solved")) == 1) {
            throw new BusinessException(400, "该问题已采纳过回答");
        }

        Integer belongs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_answer WHERE id = ? AND question_id = ?",
                Integer.class, answerId, questionId);
        if (belongs == null || belongs == 0) {
            throw new BusinessException(400, "该回答不属于这个问题");
        }

        jdbcTemplate.update(
                "UPDATE senior_question SET solved = 1, accepted_answer_id = ? WHERE id = ?",
                answerId, questionId);

        // 采纳后回写学长答疑数据，让「评分 / 答疑次数」随真实反馈变化
        String seniorId = jdbcTemplate.query(
                "SELECT senior_id FROM senior_answer WHERE id = ?",
                resultSet -> resultSet.next() ? resultSet.getString("senior_id") : null,
                answerId);
        if (seniorId != null && !seniorId.isBlank()) {
            jdbcTemplate.update(
                    "UPDATE senior SET answer_count = answer_count + 1,"
                            + " rating = LEAST(rating + 0.1, 5.0), updated_at = NOW() WHERE id = ?",
                    seniorId);
        }

        evictQuestionsCache();
    }

    @Override
    public Map<String, Object> submitFollowUp(String questionId, String content, Long userId, String displayName) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "追问内容不能为空");
        }

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM senior_question WHERE id = ?", Integer.class, questionId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "问题不存在");
        }

        String authorName = displayName == null || displayName.isBlank() ? "同学" : displayName;
        String followUpId = "f-" + UUID.randomUUID().toString().substring(0, 8);

        jdbcTemplate.update(
                "INSERT INTO senior_follow_up (id, question_id, author_id, author, time, content, ai_status)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 'generating')",
                followUpId, questionId, String.valueOf(userId), authorName,
                LocalDateTime.now().format(FOLLOW_UP_TIME_FORMATTER), content.trim()
        );

        evictQuestionsCache();
        // 同一位学长继续回复这条追问
        seniorAiService.generateFollowUpReplyAsync(questionId, followUpId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", followUpId);
        result.put("questionId", questionId);
        result.put("content", content.trim());
        result.put("author", authorName);
        result.put("aiStatus", SeniorAiService.STATUS_GENERATING);
        return result;
    }

    @Override
    public Map<String, Object> regenerateAnswer(String questionId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, author_id AS authorId FROM senior_question WHERE id = ?", questionId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "问题不存在");
        }
        if (!String.valueOf(userId).equals(String.valueOf(rows.get(0).get("authorId")))) {
            throw new BusinessException(403, "只有提问者本人可以重新生成回答");
        }

        jdbcTemplate.update(
                "UPDATE senior_question SET ai_status = 'generating' WHERE id = ?", questionId);
        jdbcTemplate.update(
                "UPDATE senior_follow_up SET ai_status = 'none' WHERE question_id = ? AND ai_status = 'failed'",
                questionId);

        evictQuestionsCache();
        seniorAiService.generateAnswerAsync(questionId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", questionId);
        result.put("aiStatus", SeniorAiService.STATUS_GENERATING);
        return result;
    }

    // ------------------------------------------------------------------ 查询辅助

    private List<Map<String, Object>> queryQuestions(String seniorId, String category, String filter,
                                                     String sort, int page, int pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.id, q.title, q.content, q.category, q.author_id AS authorId, q.author,"
                + " DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i') AS createdAt, q.views, q.likes,"
                + " q.solved, q.accepted_answer_id AS acceptedAnswerId, q.images,"
                + " q.answer_count AS answerCount,"
                + " q.ai_status AS aiStatus, q.ai_senior_id AS aiSeniorId,"
                + " s.name AS aiSeniorName"
                + " FROM senior_question q"
                + " LEFT JOIN senior s ON q.ai_senior_id = s.id");

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (seniorId != null && !seniorId.isBlank()) {
            sql.append(" JOIN senior_question_mentor qm ON q.id = qm.question_id");
            conditions.add("qm.senior_id = ?");
            params.add(seniorId);
        }
        if (category != null && !category.isBlank() && !"all".equals(category)) {
            conditions.add("q.category = ?");
            params.add(category);
        }
        appendFilterCondition(filter, conditions);

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        if ("likes".equals(sort)) {
            sql.append(" ORDER BY q.likes DESC, q.created_at DESC");
        } else {
            sql.append(" ORDER BY q.created_at DESC");
        }
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(Math.max(0, (page - 1) * pageSize));

        List<Map<String, Object>> questions = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        for (Map<String, Object> question : questions) {
            String questionId = String.valueOf(question.get("id"));
            question.put("mentorIds", loadMentorIds(questionId));
            question.put("answers", loadAnswers(questionId));
            question.put("followUps", loadFollowUps(questionId));
            question.put("tags", loadTagNames(questionId));
        }
        return questions;
    }

    private int queryQuestionsCount(String seniorId, String category, String filter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM senior_question q");
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (seniorId != null && !seniorId.isBlank()) {
            sql.append(" JOIN senior_question_mentor qm ON q.id = qm.question_id");
            conditions.add("qm.senior_id = ?");
            params.add(seniorId);
        }
        if (category != null && !category.isBlank() && !"all".equals(category)) {
            conditions.add("q.category = ?");
            params.add(category);
        }
        appendFilterCondition(filter, conditions);

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        Integer total = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return total == null ? 0 : total;
    }

    private void appendFilterCondition(String filter, List<String> conditions) {
        if (filter == null || "all".equals(filter)) {
            return;
        }
        switch (filter) {
            case "unsolved" -> conditions.add("q.solved = 0");
            case "solved" -> conditions.add("q.solved = 1");
            case "hot" -> conditions.add("q.likes >= 5");
            default -> { /* 未知过滤条件忽略 */ }
        }
    }

    private List<Map<String, Object>> loadAnswers(String questionId) {
        return jdbcTemplate.queryForList(
                "SELECT a.id, a.author_id AS authorId, a.author_name AS authorName, a.certified,"
                        + " a.badge, a.content, a.time, a.likes, a.senior_id AS seniorId, a.source,"
                        + " CASE WHEN a.id = q.accepted_answer_id THEN 1 ELSE 0 END AS accepted,"
                        + " 0 AS likedByMe"
                        + " FROM senior_answer a JOIN senior_question q ON a.question_id = q.id"
                        + " WHERE a.question_id = ?"
                        + " ORDER BY a.created_at ASC",
                questionId);
    }

    private List<String> loadMentorIds(String questionId) {
        List<Map<String, Object>> mentors = jdbcTemplate.queryForList(
                "SELECT senior_id FROM senior_question_mentor WHERE question_id = ?", questionId);
        List<String> mentorIds = new ArrayList<>();
        for (Map<String, Object> mentor : mentors) {
            mentorIds.add(String.valueOf(mentor.get("senior_id")));
        }
        return mentorIds;
    }

    private List<Map<String, Object>> loadFollowUps(String questionId) {
        return jdbcTemplate.queryForList(
                "SELECT id, author_id AS authorId, author, time, content,"
                        + " ai_status AS aiStatus FROM senior_follow_up"
                        + " WHERE question_id = ? ORDER BY created_at ASC",
                questionId);
    }

    private List<String> loadTagNames(String questionId) {
        List<Map<String, Object>> tags = jdbcTemplate.queryForList(
                "SELECT st.name FROM senior_tag st JOIN senior_question_tag qt ON st.id = qt.tag_id"
                        + " WHERE qt.question_id = ?",
                questionId);
        List<String> names = new ArrayList<>();
        for (Map<String, Object> tag : tags) {
            names.add(String.valueOf(tag.get("name")));
        }
        return names;
    }

    /**
     * 按当前登录用户标记「我是否点过赞」。
     * 缓存中不含该字段（否则会跨用户污染），每次返回前单独补一次查询。
     */
    private void markLikedAnswers(List<Map<String, Object>> questions, Long currentUserId) {
        Set<String> likedAnswerIds = new HashSet<>();
        if (currentUserId != null) {
            likedAnswerIds.addAll(jdbcTemplate.queryForList(
                    "SELECT answer_id FROM senior_answer_like WHERE user_id = ?",
                    String.class, currentUserId));
        }

        for (Map<String, Object> question : questions) {
            Object answers = question.get("answers");
            if (!(answers instanceof List<?> answerList)) {
                continue;
            }
            for (Object item : answerList) {
                if (item instanceof Map<?, ?> answerMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> answer = (Map<String, Object>) answerMap;
                    answer.put("likedByMe", likedAnswerIds.contains(String.valueOf(answer.get("id"))));
                }
            }
        }
    }

    private void saveTags(String questionId, Object rawTags) {
        if (!(rawTags instanceof List<?> tags) || tags.isEmpty()) {
            return;
        }
        for (Object item : tags) {
            if (item == null) {
                continue;
            }
            String tagName = String.valueOf(item).trim();
            if (tagName.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> tagResult = jdbcTemplate.queryForList(
                    "SELECT id FROM senior_tag WHERE name = ?", tagName);
            int tagId;
            if (tagResult.isEmpty()) {
                jdbcTemplate.update("INSERT INTO senior_tag (name) VALUES (?)", tagName);
                Integer generated = jdbcTemplate.queryForObject(
                        "SELECT id FROM senior_tag WHERE name = ?", Integer.class, tagName);
                tagId = generated == null ? 0 : generated;
            } else {
                tagId = toInt(tagResult.get(0).get("id"));
            }
            jdbcTemplate.update(
                    "INSERT IGNORE INTO senior_question_tag (question_id, tag_id) VALUES (?, ?)",
                    questionId, tagId);
        }
    }

    // ------------------------------------------------------------------ 缓存辅助

    private String buildQuestionsCacheKey(String seniorId, String category, String filter, String sort,
                                          int page, int pageSize) {
        return "senior:questions:" + (seniorId == null ? "all" : seniorId)
                + ":" + (category == null ? "all" : category)
                + ":" + (filter == null ? "all" : filter)
                + ":" + (sort == null ? "latest" : sort)
                + ":" + page + ":" + pageSize;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readCache(String cacheKey) {
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        } catch (Exception e) {
            log.warn("读取答疑列表缓存失败：{}", e.getMessage());
        }
        return null;
    }

    private void writeCache(String cacheKey, List<Map<String, Object>> questions) {
        try {
            redisUtil.set(cacheKey, questions, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入答疑列表缓存失败：{}", e.getMessage());
        }
    }

    /**
     * 缓存命中的列表对象是共享引用，直接改动会污染缓存，
     * 这里做一层拷贝（含 answers 子列表），保证 likedByMe 只作用于本次响应。
     */
    private List<Map<String, Object>> copyQuestions(List<Map<String, Object>> cached) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> question : cached) {
            Map<String, Object> questionCopy = new HashMap<>(question);
            Object answers = questionCopy.get("answers");
            if (answers instanceof List<?> answerList) {
                List<Map<String, Object>> answerCopy = new ArrayList<>();
                for (Object item : answerList) {
                    if (item instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) map;
                        answerCopy.add(new HashMap<>(typed));
                    }
                }
                questionCopy.put("answers", answerCopy);
            }
            copy.add(questionCopy);
        }
        return copy;
    }

    private void evictQuestionsCache() {
        try {
            redisUtil.delete(redisUtil.keys("senior:questions:*"));
        } catch (Exception e) {
            log.warn("清除答疑列表缓存失败：{}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ 工具方法

    private String requireText(Object value, String message) {
        String text = value == null ? null : String.valueOf(value).trim();
        if (text == null || text.isEmpty()) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String[] splitTags(Object value) {
        if (value == null) {
            return new String[0];
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return new String[0];
        }
        return text.split(",");
    }
}
