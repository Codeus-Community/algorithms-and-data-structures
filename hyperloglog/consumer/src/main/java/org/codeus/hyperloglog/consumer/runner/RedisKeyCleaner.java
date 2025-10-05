package org.codeus.hyperloglog.consumer.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Profile("consumer1")
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisKeyCleaner implements ApplicationRunner {

  private final RedisTemplate<String, String> redisTemplate;

  private void clearAllRedisKeys() {
    try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
      connection.serverCommands().flushAll();
      log.info("All keys cleared from Redis.");
    } catch (Exception e) {
      log.error("Error clearing Redis keys: {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    clearAllRedisKeys();
  }
}
