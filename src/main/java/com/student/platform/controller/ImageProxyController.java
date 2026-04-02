package com.student.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片代理控制器
 * 用于处理外部图片资源的访问，解决418错误问题
 */
@Tag(name = "图片代理", description = "处理外部图片资源的访问")
@RestController
@RequestMapping("/api/image-proxy")
@Slf4j
public class ImageProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 代理获取图片
     * @param url 图片URL
     * @return 图片响应
     */
    @Operation(
            summary = "代理获取图片",
            description = "通过服务器代理获取外部图片资源，解决418错误问题"
    )
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(
            @Parameter(description = "图片URL", required = true) @RequestParam String url
    ) {
        try {
            log.info("代理请求图片: {}", url);

            // 创建包含浏览器请求头的请求
            org.springframework.http.HttpEntity<?> requestEntity = new org.springframework.http.HttpEntity<>(getHeaders());

            // 发送请求获取图片
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            // 获取响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(response.getHeaders().getContentType());
            headers.setContentLength(response.getHeaders().getContentLength());

            log.info("图片代理成功: {}", url);
            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);

        } catch (HttpClientErrorException e) {
            log.error("代理请求图片失败: {}, 状态码: {}", url, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            log.error("代理请求图片异常: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 获取模拟浏览器的请求头
     * @return 请求头
     */
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Referer", "https://book.douban.com/");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "image");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        return headers;
    }
}
