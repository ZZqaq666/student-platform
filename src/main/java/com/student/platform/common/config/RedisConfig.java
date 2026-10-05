package com.student.platform.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 *
 * 功能说明：
 * - 配置RedisTemplate，用于Redis数据操作
 * - 配置序列化方式，支持Java对象的序列化和反序列化
 * - 启用Spring缓存注解支持
 *
 * 序列化配置：
 * - Key：使用String序列化
 * - Value：使用JSON序列化（Jackson）
 * - 支持Java 8日期时间类型
 * - 忽略未知字段，增强容错性
 *
 * 使用场景：
 * - 用户会话管理
 * - AI回答缓存
 * - 学习进度缓存
 * - 热点数据缓存
 *
 * @author student-platform
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 创建RedisTemplate Bean
     *
     * 配置说明：
     * - 设置连接工厂
     * - 配置Key序列化器（String）
     * - 配置Value序列化器（JSON）
     * - 支持Java 8 DateTime类型
     * - 忽略未知字段，增强容错性
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate<String, Object> Redis操作模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // 设置Redis连接工厂
        template.setConnectionFactory(connectionFactory);

        // 配置JSON序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        // 配置ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 设置所有属性可见
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用默认类型信息（用于多态类型序列化）
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // 注册Java 8日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 禁用日期时间戳格式，使用ISO-8601格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知字段，增强容错性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 设置ObjectMapper到序列化器
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 配置String序列化器（用于Key）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置Key序列化器
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // 设置Value序列化器
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 初始化模板
        template.afterPropertiesSet();
        
        return template;
    }
}
