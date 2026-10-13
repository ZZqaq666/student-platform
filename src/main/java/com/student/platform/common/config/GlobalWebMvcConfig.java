package com.student.platform.common.config;

import com.student.platform.user.config.UserIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 单体全局 MVC 配置（合并自原各服务的 WebMvcConfig）：
 * - CORS 按显式来源白名单放行（前端开发走 Vite 代理，本身同源）
 * - 注册 UserIdInterceptor，从 X-User-Id 头解析 userId 到请求属性
 */
@Configuration
public class GlobalWebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserIdInterceptor())
                .addPathPatterns("/**");
    }
}
