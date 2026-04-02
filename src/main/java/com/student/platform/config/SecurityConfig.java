package com.student.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.student.platform.config.JwtAuthenticationFilter;

/**
 * 安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/**", "/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/qa/history", "/qa/history/**", "/qa/courses", "/qa/courses/recommendations", "/qa/courses/recommend", "/qa/exam/qa/history", "/qa/exam/qa/history/**", "/qa/exam/qa/save").permitAll()
                .requestMatchers("/api/qa/history", "/api/qa/history/**", "/api/qa/courses", "/api/qa/courses/recommendations", "/api/qa/courses/recommend", "/api/qa/exam/qa/history", "/api/qa/exam/qa/history/**", "/api/qa/exam/qa/save").permitAll()
                .requestMatchers("/books/**").permitAll()
                .requestMatchers("/api/books/**").permitAll()
                .requestMatchers("/bookshelf/**").permitAll()
                .requestMatchers("/api/bookshelf/**").permitAll()
                .requestMatchers("/api/image-proxy/**").permitAll()
                .requestMatchers("/senior/**").permitAll()
                .requestMatchers("/oss/signature/**").authenticated()
                // 管理员接口需要ADMIN角色（开发环境临时放宽为已认证用户即可访问）
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
