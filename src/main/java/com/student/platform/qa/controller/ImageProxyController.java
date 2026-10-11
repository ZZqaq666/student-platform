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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 图片代理控制器
 *
 * 功能说明：
 * - 代理访问外部图片资源，解决418错误和跨域问题
 * - 模拟浏览器请求头，绕过图片服务器的访问限制
 * - 支持各类图片格式（JPG、PNG、WebP等）
 *
 * 安全措施（防 SSRF）：
 * 1. 仅允许 http/https 协议，禁止 file、jar、ftp 等
 * 2. URL 必须在 {@link #ALLOWED_HOST_SUFFIXES} 域名白名单内（教材封面图床）
 * 3. DNS 解析后逐个校验目标 IP，拒绝回环地址、内网私有网段、链路本地（含云元数据 169.254.169.254）
 * 4. 禁止跟随重定向，避免白名单被 302 绕过
 * 5. 连接/读取超时 + 响应体大小上限，防止慢速攻击与大内存占用
 * 6. 响应 Content-Type 必须为 image/*
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
     * 域名白名单：仅放行教材/书籍封面常用的公开图床。
     * 匹配规则为「精确相等」或「以 .xxx 结尾」，因此 doubanio.com.evil.com 这类伪装域名不会命中。
     */
    private static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of(
            "doubanio.com",
            "douban.com",
            "picsum.photos"
    );

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_URL_LENGTH = 2048;

    @Operation(
            summary = "代理获取图片",
            description = "通过服务器代理获取外部图片资源，解决418错误和防盗链问题（仅限白名单图床）"
    )
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(
            @Parameter(description = "图片URL", required = true) @RequestParam String url
    ) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String target = url.trim();
        if (target.length() > MAX_URL_LENGTH) {
            log.warn("图片代理拒绝：URL 超长");
            return ResponseEntity.badRequest().build();
        }

        // 1~3. 协议、域名白名单、目标 IP 合法性校验
        try {
            String rejection = validate(target);
            if (rejection != null) {
                log.warn("图片代理拒绝违规URL: {}, 原因: {}", target, rejection);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.warn("图片代理URL解析失败: {}, {}", target, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        HttpURLConnection conn = null;
        try {
            URL u = new URL(target);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            // 4. 禁止跟随重定向，防止通过 302 跳到内网地址绕过白名单
            conn.setInstanceFollowRedirects(false);
            getHeaders().forEach(conn::setRequestProperty);

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                log.info("图片代理上游返回非成功状态: {}, status={}", target, status);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

            // 6. 只接受图片响应，避免被用来代理任意文本内容
            String contentType = conn.getHeaderField("Content-Type");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                log.warn("图片代理拒绝非图片响应: {}, contentType={}", target, contentType);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }

            long declaredLength = conn.getHeaderFieldLong("Content-Length", -1L);
            if (declaredLength > MAX_IMAGE_BYTES) {
                log.warn("图片代理拒绝超大响应: {}, size={}", target, declaredLength);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            // 5. 流式读取并强制限制大小，防止无 Content-Length 的超大响应撑爆内存
            byte[] body = readLimited(conn.getInputStream());
            if (body == null) {
                log.warn("图片代理响应体超出上限: {}", target);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(body.length);
            return new ResponseEntity<>(body, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("代理请求图片异常: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (Exception e) {
            log.error("代理请求图片内部错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 综合校验，返回 null 表示通过，否则返回拒绝原因。
     */
    private String validate(String target) throws Exception {
        URI uri = URI.create(target);
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            return "协议不合法（仅允许 http/https）";
        }
        if (uri.getRawUserInfo() != null) {
            return "URL 不允许携带用户信息";
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "无法解析主机名";
        }
        host = host.toLowerCase(Locale.ROOT);

        boolean allowed = false;
        for (String suffix : ALLOWED_HOST_SUFFIXES) {
            if (host.equals(suffix) || host.endsWith("." + suffix)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return "域名不在白名单内";
        }

        // DNS 解析后校验真实 IP，防御域名指向内网 / 云元数据服务的情况
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            return "DNS 解析无结果";
        }
        for (InetAddress addr : addresses) {
            String reason = rejectReason(addr);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    /**
     * 判定该地址是否属于禁止访问的地址类型。返回 null 表示允许。
     */
    private String rejectReason(InetAddress addr) {
        if (addr.isAnyLocalAddress()) return "目标为通配地址";
        if (addr.isLoopbackAddress()) return "目标为回环地址";
        if (addr.isLinkLocalAddress()) return "目标为链路本地地址";
        if (addr.isSiteLocalAddress()) return "目标为内网私有地址";
        if (addr.isMulticastAddress()) return "目标为组播地址";
        String ip = addr.getHostAddress();
        if (ip.startsWith("169.254.")) return "目标为链路本地/元数据地址";
        if (ip.startsWith("0.")) return "目标为保留网段";
        if (ip.startsWith("100.64.")) return "目标为运营商级NAT保留网段";
        if (ip.startsWith("192.0.0.") || ip.startsWith("192.0.2.")
                || ip.startsWith("198.18.") || ip.startsWith("198.51.100.")
                || ip.startsWith("203.0.113.")) return "目标为文档保留网段";
        return null;
    }

    /**
     * 带上限的流式读取，超出 {@link #MAX_IMAGE_BYTES} 时返回 null。
     */
    private byte[] readLimited(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        try (InputStream stream = in; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int total = 0;
            int n;
            while ((n = stream.read(chunk)) != -1) {
                total += n;
                if (total > MAX_IMAGE_BYTES) {
                    return null;
                }
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
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
        return Map.of(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Accept", "image/webp,image/apng,image/*,*/*;q=0.8",
                "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"
        );
    }
}
