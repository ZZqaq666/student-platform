package com.student.platform.service.impl;

import com.student.platform.service.SeniorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SeniorServiceImpl implements SeniorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getSeniors() {
        String sql = "SELECT id, name, grade, major, subject, tags, answer_count as answerCount, rating, certified, online FROM senior";
        List<Map<String, Object>> seniors = jdbcTemplate.queryForList(sql);
        
        // 处理tags字段，转换为数组
        for (Map<String, Object> senior : seniors) {
            String tags = (String) senior.get("tags");
            if (tags != null) {
                senior.put("tags", tags.split(","));
            } else {
                senior.put("tags", new String[0]);
            }
        }
        
        return seniors;
    }

    @Override
    public Map<String, Object> getSeniorById(String id) {
        String sql = "SELECT id, name, grade, major, subject, tags, answer_count as answerCount, rating, certified, online FROM senior WHERE id = ?";
        Map<String, Object> senior = jdbcTemplate.queryForMap(sql, id);
        
        // 处理tags字段
        String tags = (String) senior.get("tags");
        if (tags != null) {
            senior.put("tags", tags.split(","));
        } else {
            senior.put("tags", new String[0]);
        }
        
        return senior;
    }

    @Override
    public List<Map<String, Object>> getQuestions(String seniorId, String filter, String sort, int page, int pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.id, q.title, q.content, q.category, q.author_id as authorId, q.author, q.created_at as createdAt, q.views, q.likes, q.solved, q.accepted_answer_id as acceptedAnswerId, q.images");
        sql.append(" FROM senior_question q");
        
        List<Object> params = new ArrayList<>();
        
        // 如果指定了学长ID，需要通过关联表过滤
        if (seniorId != null && !seniorId.isEmpty()) {
            sql.append(" JOIN senior_question_mentor qm ON q.id = qm.question_id");
            sql.append(" WHERE qm.senior_id = ?");
            params.add(seniorId);
        }
        
        // 应用过滤条件
        if (filter != null) {
            if (!params.isEmpty()) {
                sql.append(" AND");
            } else {
                sql.append(" WHERE");
            }
            
            switch (filter) {
                case "unsolved":
                    sql.append(" q.solved = false");
                    break;
                case "solved":
                    sql.append(" q.solved = true");
                    break;
                case "hot":
                    sql.append(" q.hot_score >= 100");
                    break;
            }
        }
        
        // 应用排序
        sql.append(" ORDER BY");
        if ("likes".equals(sort)) {
            sql.append(" q.likes DESC");
        } else {
            sql.append(" q.created_at DESC");
        }
        
        // 分页
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        
        List<Map<String, Object>> questions = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        
        // 为每个问题加载相关数据
        for (Map<String, Object> question : questions) {
            String questionId = (String) question.get("id");
            
            // 加载学长ID列表
            List<Map<String, Object>> mentors = jdbcTemplate.queryForList(
                "SELECT senior_id FROM senior_question_mentor WHERE question_id = ?", questionId
            );
            List<String> mentorIds = new ArrayList<>();
            for (Map<String, Object> mentor : mentors) {
                mentorIds.add((String) mentor.get("senior_id"));
            }
            question.put("mentorIds", mentorIds);
            
            // 加载回答列表
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(
                "SELECT id, author_id as authorId, author_name as authorName, certified, badge, content, time, likes, liked_by_current_user as likedByCurrentUser FROM senior_answer WHERE question_id = ? ORDER BY created_at DESC", 
                questionId
            );
            question.put("answers", answers);
            
            // 加载追问列表
            List<Map<String, Object>> followUps = jdbcTemplate.queryForList(
                "SELECT id, author_id as authorId, author, time, content FROM senior_follow_up WHERE question_id = ? ORDER BY created_at DESC", 
                questionId
            );
            question.put("followUps", followUps);
            
            // 加载标签
            List<Map<String, Object>> tags = jdbcTemplate.queryForList(
                "SELECT st.name FROM senior_tag st JOIN senior_question_tag qt ON st.id = qt.tag_id WHERE qt.question_id = ?", 
                questionId
            );
            List<String> tagNames = new ArrayList<>();
            for (Map<String, Object> tag : tags) {
                tagNames.add((String) tag.get("name"));
            }
            question.put("tags", tagNames);
        }
        
        return questions;
    }

    @Override
    public Map<String, Object> getQuestionById(String id) {
        // 直接根据ID查询问题
        String sql = "SELECT id, title, content, category, author_id as authorId, author, created_at as createdAt, views, likes, solved, accepted_answer_id as acceptedAnswerId, images FROM senior_question WHERE id = ?";
        Map<String, Object> question = jdbcTemplate.queryForMap(sql, id);
        
        // 加载学长ID列表
        List<Map<String, Object>> mentors = jdbcTemplate.queryForList(
            "SELECT senior_id FROM senior_question_mentor WHERE question_id = ?", id
        );
        List<String> mentorIds = new ArrayList<>();
        for (Map<String, Object> mentor : mentors) {
            mentorIds.add((String) mentor.get("senior_id"));
        }
        question.put("mentorIds", mentorIds);
        
        // 加载回答列表
        List<Map<String, Object>> answers = jdbcTemplate.queryForList(
            "SELECT id, author_id as authorId, author_name as authorName, certified, badge, content, time, likes, liked_by_current_user as likedByCurrentUser FROM senior_answer WHERE question_id = ? ORDER BY created_at DESC", 
            id
        );
        question.put("answers", answers);
        
        // 加载追问列表
        List<Map<String, Object>> followUps = jdbcTemplate.queryForList(
            "SELECT id, author_id as authorId, author, time, content FROM senior_follow_up WHERE question_id = ? ORDER BY created_at DESC", 
            id
        );
        question.put("followUps", followUps);
        
        // 加载标签
        List<Map<String, Object>> tags = jdbcTemplate.queryForList(
            "SELECT st.name FROM senior_tag st JOIN senior_question_tag qt ON st.id = qt.tag_id WHERE qt.question_id = ?", 
            id
        );
        List<String> tagNames = new ArrayList<>();
        for (Map<String, Object> tag : tags) {
            tagNames.add((String) tag.get("name"));
        }
        question.put("tags", tagNames);
        
        return question;
    }

    @Override
    public Map<String, Object> publishQuestion(Map<String, Object> data) {
        String id = "q-" + UUID.randomUUID().toString().substring(0, 8);
        String title = (String) data.get("title");
        String category = (String) data.get("category");
        String content = (String) data.get("content");
        List<String> tags = (List<String>) data.get("tags");
        List<String> mentorIds = (List<String>) data.get("mentorIds");
        
        // 插入问题
        String sql = "INSERT INTO senior_question (id, title, content, category, author_id, author, created_at, created_at_ms) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        jdbcTemplate.update(sql, id, title, content, category, "u-current", "我", System.currentTimeMillis());
        
        // 插入问题-学长关联
        if (mentorIds != null && !mentorIds.isEmpty()) {
            for (String mentorId : mentorIds) {
                jdbcTemplate.update(
                    "INSERT INTO senior_question_mentor (question_id, senior_id) VALUES (?, ?)", 
                    id, mentorId
                );
            }
        }
        
        // 插入标签
        if (tags != null && !tags.isEmpty()) {
            for (String tagName : tags) {
                // 查找或创建标签
                List<Map<String, Object>> tagResult = jdbcTemplate.queryForList(
                    "SELECT id FROM senior_tag WHERE name = ?", tagName
                );
                int tagId;
                if (tagResult.isEmpty()) {
                    // 创建新标签
                    jdbcTemplate.update("INSERT INTO senior_tag (name) VALUES (?)", tagName);
                    tagResult = jdbcTemplate.queryForList("SELECT id FROM senior_tag WHERE name = ?", tagName);
                    tagId = (int) tagResult.get(0).get("id");
                } else {
                    tagId = (int) tagResult.get(0).get("id");
                }
                
                // 插入问题-标签关联
                jdbcTemplate.update(
                    "INSERT INTO senior_question_tag (question_id, tag_id) VALUES (?, ?)", 
                    id, tagId
                );
            }
        }
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("content", content);
        result.put("category", category);
        result.put("mentorIds", mentorIds);
        result.put("tags", tags);
        
        return result;
    }

    @Override
    public void likeAnswer(String answerId) {
        // 切换点赞状态
        Map<String, Object> answer = jdbcTemplate.queryForMap(
            "SELECT liked_by_current_user FROM senior_answer WHERE id = ?", answerId
        );
        boolean liked = (boolean) answer.get("liked_by_current_user");
        
        if (liked) {
            // 取消点赞
            jdbcTemplate.update(
                "UPDATE senior_answer SET likes = likes - 1, liked_by_current_user = false WHERE id = ?", 
                answerId
            );
        } else {
            // 点赞
            jdbcTemplate.update(
                "UPDATE senior_answer SET likes = likes + 1, liked_by_current_user = true WHERE id = ?", 
                answerId
            );
        }
    }

    @Override
    public void acceptAnswer(String questionId, String answerId) {
        // 更新问题状态
        jdbcTemplate.update(
            "UPDATE senior_question SET solved = true, accepted_answer_id = ? WHERE id = ?", 
            answerId, questionId
        );
    }

    @Override
    public void submitFollowUp(String questionId, String content) {
        String id = "f-" + UUID.randomUUID().toString().substring(0, 8);
        
        // 插入追问
        jdbcTemplate.update(
            "INSERT INTO senior_follow_up (id, question_id, author_id, author, time, content) VALUES (?, ?, ?, ?, ?, ?)", 
            id, questionId, "u-current", "我", "刚刚", content
        );
    }
}
