package com.student.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return Health.up()
                    .withDetail("redis", "available")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
