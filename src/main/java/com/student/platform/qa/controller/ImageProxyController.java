package com.student.platform.qa.controller;

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
 *
 * 功能说明：
 * - 代理访问外部图片资源，解决418错误和跨域问题
 * - 模拟浏览器请求头，绕过图片服务器的访问限制
 * - 支持各类图片格式（JPG、PNG、WebP等）
 *
 * 使用场景：
 * - 教材封面图片加载
 * - 外部资源图片展示
 * - 绕过防盗链限制
 * - 解决跨域图片加载问题
 *
 * 安全注意事项：
 * - 需要对URL进行白名单校验，防止SSRF攻击
 * - 限制可访问的域名范围
 * - 对请求频率进行限制
 *
 * @author student-platform
 * @since 1.0.0
 */
@Tag(name = "图片代理", description = "处理外部图片资源的访问")
@RestController
@RequestMapping("/image-proxy")
@Slf4j
public class ImageProxyController {

    /**
     * RestTemplate实例，用于发送HTTP请求
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 代理获取图片
     *
     * 通过服务器代理获取外部图片资源，模拟浏览器请求头绕过访问限制
     *
     * @param url 图片URL
     * @return 图片二进制数据和响应头
     */
    @Operation(
            summary = "代理获取图片",
            description = "通过服务器代理获取外部图片资源，解决418错误和防盗链问题"
    )
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(
            @Parameter(description = "图片URL", required = true) @RequestParam String url
    ) {
        try {
            log.info("代理请求图片: {}", url);

            // 创建包含模拟浏览器请求头的请求实体
            org.springframework.http.HttpEntity<?> requestEntity = new org.springframework.http.HttpEntity<>(getHeaders());

            // 发送GET请求获取图片数据
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            // 构建响应头，保留原图片的Content-Type和Content-Length
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
     *
     * 模拟Chrome浏览器发送请求，绕过服务器的访问限制
     *
     * @return 请求头Map
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
