package com.park.eventcenter.common.idempotency;

import java.time.Duration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis-backed idempotency store, the production adapter. */
@Component
@Profile("!local")
public class RedisIdempotencyStore implements IdempotencyStore {

  private final StringRedisTemplate redisTemplate;

  public RedisIdempotencyStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean markIfAbsent(String key, Duration ttl) {
    Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
    return Boolean.TRUE.equals(created);
  }

  @Override
  public boolean exists(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  @Override
  public void mark(String key, Duration ttl) {
    redisTemplate.opsForValue().set(key, "1", ttl);
  }
}
