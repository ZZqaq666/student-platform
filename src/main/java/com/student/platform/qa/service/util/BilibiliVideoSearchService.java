package com.student.platform.qa.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * B站视频真实搜索服务
 *
 * 功能说明：
 * - 服务端真实调用 B站公开搜索接口，返回真实存在的视频（真实 BV 号、标题、UP主、播放量）
 * - 用于课程/视频推荐场景，确保推荐给用户的 videoLink 是真实可访问的链接，
 *   杜绝大模型凭"记忆"编造不相关链接的问题（联网搜索由本服务承担，AI 只做候选精选）
 *
 * @author student-platform
 */
@Service
@Slf4j
public class BilibiliVideoSearchService {

    private static final String SEARCH_URL = "https://api.bilibili.com/x/web-interface/search/all/v2";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 搜索单个关键词下的真实视频
     *
     * @param keyword 搜索关键词
     * @param limit   最多返回条数
     * @return 视频候选列表（title/bvid/url/author/play/duration/description/keyword）
     */
    public List<Map<String, Object>> searchVideos(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            String body = HttpRequest.get(SEARCH_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.bilibili.com/")
                    .form("keyword", keyword.trim())
                    .form("order", "totalrank")
                    .timeout(8000)
                    .execute()
                    .body();

            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                log.warn("B站搜索接口返回异常，关键词: {}，code: {}，msg: {}",
                        keyword, root.path("code").asInt(), root.path("message").asText());
                return Collections.emptyList();
            }

            List<Map<String, Object>> videos = new ArrayList<>();
            for (JsonNode block : root.path("data").path("result")) {
                if (!"video".equals(block.path("result_type").asText())) {
                    continue;
                }
                for (JsonNode v : block.path("data")) {
                    if (videos.size() >= limit) {
                        break;
                    }
                    String bvid = v.path("bvid").asText();
                    if (bvid == null || bvid.isEmpty()) {
                        continue;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", cleanHtml(v.path("title").asText()));
                    item.put("bvid", bvid);
                    item.put("url", "https://www.bilibili.com/video/" + bvid);
                    item.put("author", v.path("author").asText());
                    item.put("play", v.path("play").asLong(0));
                    item.put("duration", v.path("duration").asText(""));
                    item.put("description", abbreviate(cleanHtml(v.path("description").asText("")), 120));
                    item.put("keyword", keyword.trim());
                    videos.add(item);
                }
                if (videos.size() >= limit) {
                    break;
                }
            }
            log.info("B站真实搜索完成，关键词: {}，命中视频数: {}", keyword, videos.size());
            return videos;
        } catch (Exception e) {
            log.error("B站视频搜索失败，关键词: {}，原因: {}", keyword, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 按多个关键词搜索并汇总去重（按 bvid 去重，保持关键词顺序）
     *
     * @param keywords   关键词列表
     * @param perKeyword 每个关键词最多取多少条
     * @return 去重后的候选列表
     */
    public List<Map<String, Object>> searchByKeywords(List<String> keywords, int perKeyword) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> merged = new ArrayList<>();
        Set<String> seenBvids = new HashSet<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            for (Map<String, Object> video : searchVideos(keyword, perKeyword)) {
                String bvid = String.valueOf(video.get("bvid"));
                if (seenBvids.add(bvid)) {
                    merged.add(video);
                }
            }
        }
        return merged;
    }

    /**
     * 生成关键词的B站搜索页链接（作为兜底：永远真实有效且主题相关）
     */
    public String searchPageUrl(String keyword) {
        String safeKeyword = keyword == null ? "大学课程" : keyword.trim();
        return "https://search.bilibili.com/all?keyword=" + URLEncoder.encode(safeKeyword, StandardCharsets.UTF_8);
    }

    /**
     * 判断链接是否为候选集合中的真实视频链接
     */
    public boolean isRealCandidateLink(String url, List<Map<String, Object>> candidates) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        for (Map<String, Object> candidate : candidates) {
            if (url.equals(candidate.get("url"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理搜索结果标题中的高亮 HTML 标签
     */
    private String cleanHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("<em[^>]*>", "").replace("</em>", "").trim();
    }

    /**
     * 截断文本
     */
    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
