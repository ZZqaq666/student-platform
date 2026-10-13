package com.student.platform.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 配置类。
 *
 * 两套序列化器，用途不同，不要混用：
 * - redisTemplate：值不带类型信息。现有调用只存取标量与 List/Map（如
 *   {@code qa:user:history:*} 的 ID 列表、{@code senior:questions:*} 的
 *   {@code List<Map<String,Object>>}），回读为 LinkedHashMap 即可满足。
 * - Spring Cache（@Cacheable）：回读时拿不到方法声明的返回类型，必须靠 JSON 里的
 *   类型标记才能还原成 {@code List<ExerciseDTO>} 这类具体类型，因此这里保留类型信息，
 *   但用白名单校验器限定可实例化的类。
 *
 * 缓存管理器必须显式声明：不声明时 Spring Boot 会退回 JDK 原生序列化，
 * 既要求被缓存对象实现 Serializable（本项目 DTO 都没实现，@Cacheable 一命中就抛异常），
 * 又把 Java 反序列化 gadget 面暴露给 Redis。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializationContext.SerializationPair<Object> values =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(cacheObjectMapper()));
        RedisSerializationContext.SerializationPair<String> keys =
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(keys)
                .serializeValuesWith(values);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        // 练习题随题库编辑变化，由 @CacheEvict 主动失效，TTL 只作兜底
                        "exercises", defaults.entryTtl(Duration.ofMinutes(10)),
                        "wrongBookExport", defaults.entryTtl(Duration.ofMinutes(5))))
                .build();
    }

    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
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
