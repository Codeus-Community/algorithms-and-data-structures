package org.codeus.hyperloglog.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.hyperloglog.common.Event;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.codeus.hyperloglog.common.Constants.BASE_PREFIX;
import static org.codeus.hyperloglog.common.DateTimeUtils.DATE_HOUR_FMT;

/**
 * Service for storing events in Redis using its internal structures for memory-efficient unique user counting.
 * Additionally, it manages the storage of consumed events .
 * This service provides methods to store events grouped by key and clear stored data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventStorageService {

  private final RedisTemplate<String, String> redisTemplate;
  private final AtomicInteger eventCounter = new AtomicInteger(0);

  public static final String CACHE_KEY_PREFIX = BASE_PREFIX; //TODO: change key once you started tasks, e.g. to "hll"

  /**
   * Generates a Redis cache key for the given event.
   *
   * @param event the event for which the cache key is generated
   * @return the generated cache key
   */
  protected String cacheKey(Event event) {
    return String.format("%s:%s:%s:%s:%s",
      CACHE_KEY_PREFIX,
      event.getTenantId(),
      event.getCountry(),
      event.getDevice(),
      DATE_HOUR_FMT.format(Instant.ofEpochMilli(event.getTimestamp()))
    );
  }

  /**
   * Stores an event in Redis using HyperLogLog for unique user counting.
   * Logs the number of stored events every 1000 events.
   *
   * @param event the event to be stored
   */
  //TODO: adjust method to work with HyperLogLog data structure
  //      Don't forget to change cache key prefix to avoid key collisions in Redis (see and change CACHE_KEY_PREFIX).
  public void storeEvent(Event event) {
    String cacheKey = cacheKey(event);
    redisTemplate.opsForSet().add(cacheKey, event.getUserId());

    int count = eventCounter.incrementAndGet();
    if (count % 1000 == 0) {
      log.info("💾 Stored {} events using HyperLogLog (Memory: minimal)", count);
    }
  }

  /**
   * Clears all stored events from Redis and resets the event counter.
   */
  public void clearStorage() {
    Set<String> keys = redisTemplate.keys("%s:*".formatted(CACHE_KEY_PREFIX));
    if (!keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
    eventCounter.set(0);
  }
}