package com.student.platform.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Spring Cache（@Cacheable）使用的 Redis 缓存管理器。
 *
 * 类型信息必须开，但只能按白名单开：@Cacheable 回读时拿不到方法声明的返回类型，
 * 没有类型标记就只能还原成 LinkedHashMap；而无限制的多态类型推断等于允许
 * Redis 里的任意类名被实例化，是反序列化 RCE 面。因此用 BasicPolymorphicTypeValidator
 * 限定为项目自身类型与几个 JDK 容器类型 —— 新增被缓存的第三方类型时要显式加白名单。
 *
 * 原先这里还定义了 recommendCacheManager(6h) 与 userProfileCacheManager(12h)
 * 两个 Bean，但没有任何 cacheManagerRef 引用它们，宣称的 TTL 从未生效，已删除；
 * 每个缓存各自的 TTL 改由 withInitialCacheConfigurations 表达。
 */
@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializationContext.SerializationPair<Object> values =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(cacheObjectMapper()));

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(values)
                .prefixCacheNameWith("student:platform:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        // 题库编辑有 @CacheEvict 主动失效，TTL 只作兜底
                        "exercises", defaults.entryTtl(Duration.ofMinutes(10)),
                        "wrongBookExport", defaults.entryTtl(Duration.ofMinutes(5))))
                .build();
    }

    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.student.platform.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.math.")
                        .allowIfSubTypeIsArray()
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
