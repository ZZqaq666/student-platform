package com.student.platform.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 单体统一 Spring Security 配置。
 *
 * 接口级鉴权由 {@link JwtAuthFilter}（原网关过滤器移植）负责：
 * - HTTP 层统一 permitAll，避免双重拦截
 * - JwtAuthFilter 注册在 SecurityContextHolderFilter 之后（security chain 内部），
 *   校验 JWT 后同时设置 SecurityContext + 注入 X-User-Id / X-User-Name 请求头
 * - 通过 FilterRegistrationBean 禁用其顶层 Servlet 注册，防止重复执行
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 注册到 security chain 内部：在 SecurityContextHolderFilter 之后执行，
                // 保证 JwtAuthFilter 设置的 SecurityContext 不被覆盖
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 禁用 JwtAuthFilter 的顶层 Servlet 自动注册（@Component Filter 默认会被注册），
     * 使其仅在 security chain 中执行一次。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**"
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
