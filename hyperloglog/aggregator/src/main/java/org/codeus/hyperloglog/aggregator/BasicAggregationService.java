package org.codeus.hyperloglog.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.codeus.hyperloglog.common.Constants.BASE_PREFIX;
import static org.codeus.hyperloglog.common.DateTimeUtils.DATE_HOUR_FMT;
import static org.codeus.hyperloglog.common.DateTimeUtils.ZONE_ID;

/**
 * Basic implementation of AggregationService with internal @Scheduled methods to track aggregation behavior in logs.
 * <p>
 * This implementation processes Redis keys like:
 * {storagePrefix}:{tenant}:{countryCode}:{device}:{yyyy-MM-dd-HH}
 * Example: hll:tenant-3:DE:iOS:2025-10-08-20
 * <p>
 * Aggregated data is stored back to Redis with keys:<br>
 * - Hour aggregate key:                             hll:hourly:{tenant}:{yyyy-MM-dd-HH}<br>
 * - Tenant filtered Hour aggregate key:             hll:hourly:{tenant}:{yyyy-MM-dd-HH}<br>
 * - Tenant filtered Day aggregate key:              hll:daily:{tenant}:{yyyy-MM-dd}<br>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicAggregationService implements AggregationService {

  private final RedisTemplate<String, String> redisTemplate;

  public static final String CACHE_KEY_PREFIX = "hll";//def BASE_PREFIX //TODO: change key once you started tasks, e.g. to "hll"
  public static final String AGGREGATION_KEY_PREFIX = "aggr";
  private static final DateTimeFormatter DAY_KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Retrieves the approximate number of unique users per event key for the specified hour.
   * <p>
   * This method queries Redis for keys matching the pattern `{CACHE_KEY_PREFIX}:*:*:*:{hourBucket}`.
   * It calculates the approximate unique user count for each key using HyperLogLog data structures.
   * If no data is found for the specified hour, an empty map is returned.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * ZonedDateTime now = ZonedDateTime.now();
   * Map<String, Long> result = listUniqueUsersPerEventKeyByHour(now);
   * Example output:
   * //{
   * //  "hll:tenant-1:US:Android:2023-10-08-15" -> 150,
   * //  "hll:tenant-2:DE:iOS:2023-10-08-15" -> 200
   * //}
   * </pre>
   *
   * @param now the timestamp used to derive the hour bucket
   * @return a map of event keys to approximate unique user counts for the hour bucket
   */
  @Override
  public Map<String, Long> listUniqueUsersPerEventKeyByHour(ZonedDateTime now) {
    String hourBucket = now.format(DATE_HOUR_FMT);
    String cacheKey = ("%s:*:*:*:%s").formatted(CACHE_KEY_PREFIX, hourBucket);
    Set<String> keys = redisTemplate.keys(cacheKey);

    if (keys.isEmpty()) {
      log.info("No data were collected for {} hour", hourBucket);
      return Collections.emptyMap();
    }

    Map<String, Long> uniqueUsersByEventKey = keys.parallelStream().collect(
      Collectors.toMap(Function.identity(), key -> redisTemplate.opsForHyperLogLog().size(key)));

    uniqueUsersByEventKey.forEach((eventKey, count) -> log.info("📊 {} -> ~{} unique users per hour ({})", eventKey, count, hourBucket));
    return uniqueUsersByEventKey;
  }

  /**
   * Retrieves the approximate number of unique users for the specified hour.
   * <p>
   * This method should aggregate data from Redis keys matching a specific pattern.
   * The aggregated result should be stored in Redis under custom key, different from the retrieval keys.
   * If no data is found, the result is 0.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * ZonedDateTime now = ZonedDateTime.now();
   * long result = getUniqueUsersByHour(now);
   * // Example output: 350
   * </pre>
   *
   * @param now the timestamp used to derive the hour bucket
   * @return the approximate unique user count for the hour bucket
   */
  @Override
  public long getUniqueUsersByHour(ZonedDateTime now) {
    String hourBucket = now.format(DATE_HOUR_FMT);
    String cacheKey = "%s:*:*:*:%s".formatted(CACHE_KEY_PREFIX, hourBucket);

    String hourAggregateKey = "%s:hourly:%s".formatted(AGGREGATION_KEY_PREFIX, hourBucket);

    long uniqueUserPerHour = aggregate(cacheKey, hourAggregateKey);
    log.info("📊 {} -> ~{} unique users per hour", hourAggregateKey, uniqueUserPerHour);
    return uniqueUserPerHour;
  }

  /**
   * Retrieves the approximate number of unique users for the specified tenant and hour.
   * <p>
   * This method should aggregate data from Redis keys matching a specific pattern that includes tenant value.
   * The aggregated result should be stored in Redis under custom key, different from the retrieval keys.
   * If no data is found, the result is 0.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * ZonedDateTime now = ZonedDateTime.now();
   * long result = getUniqueUsersByHour("tenant-1", now);
   * // Example output: 120
   * </pre>
   *
   * @param tenant the tenant identifier
   * @param now    the timestamp used to derive the hour bucket
   * @return the approximate unique user count for the tenant and hour bucket
   */
  @Override
  public long getUniqueUsersByHour(String tenant, ZonedDateTime now) {
    String hourBucket = now.format(DATE_HOUR_FMT);
    String key = "%s:%s:*:*:%s".formatted(CACHE_KEY_PREFIX, tenant, hourBucket);
    String hourAggregateKey = "%s:hourly:%s:%s".formatted(AGGREGATION_KEY_PREFIX, tenant, hourBucket);

    long uniqueUserPerHour = aggregate(key, hourAggregateKey);
    log.info("📊 {} -> ~{} unique users for tenant={} per hour", hourAggregateKey, uniqueUserPerHour, tenant);
    return uniqueUserPerHour;
  }

  /**
   * Retrieves the approximate number of unique users for the specified tenant and day.
   * <p>
   * This method should aggregate data from Redis keys matching a specific pattern that includes tenant value..
   * The aggregated result should be stored in Redis under custom key, different from the retrieval keys.
   * If no data is found, the result is 0.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * ZonedDateTime now = ZonedDateTime.now();
   * long result = getUniqueUsersByDay("tenant-1", now);
   * // Example output: 500
   * </pre>
   *
   * @param tenant the tenant identifier
   * @param now    the timestamp used to derive the day bucket
   * @return the approximate unique user count for the tenant and day bucket
   */
  @Override
  public long getUniqueUsersByDay(String tenant, ZonedDateTime now) {
    String dayBucket = now.format(DAY_KEY_FMT);
    String key = "%s:%s:*:*:%s*".formatted(CACHE_KEY_PREFIX, tenant, dayBucket);
    String dayAggregateKey = "%s:daily:%s:%s".formatted(AGGREGATION_KEY_PREFIX, tenant, dayBucket);

    long uniqueUserPerDay = aggregate(key, dayAggregateKey);
    log.info("📊 {} -> ~{} unique users for tenant={} per day", dayAggregateKey, uniqueUserPerDay, tenant);
    return uniqueUserPerDay;
  }

  /**
   * Aggregates data from Redis keys matching the specified pattern and stores the result in a new key.
   * <p>
   * This method uses Redis HyperLogLog operations to calculate the approximate unique user count
   * for the given keys and stores the aggregated result in the specified aggregate key.
   * If no data is found for the given pattern, the result is 0.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * String retrieveKey = "hll:tenant-1:*:*:2023-10-08-15";
   * String aggregateKey = "aggr:hourly:2023-10-08-15";
   * long result = aggregate(retrieveKey, aggregateKey);
   * // Example output: 300
   * </pre>
   *
   * @param retrieveKey the pattern to match Redis keys for aggregation
   * @param aggregateKey the Redis key where the aggregated result will be stored
   * @return the approximate unique user count for the aggregated keys
   */
  private long aggregate(String retrieveKey, String aggregateKey) {
    Set<String> keys = redisTemplate.keys(retrieveKey);

    if (keys.isEmpty()) {
      log.info("No data found for key={}", retrieveKey);
      return 0L;
    }

    redisTemplate.opsForHyperLogLog().union(aggregateKey, keys.toArray(String[]::new));
    return redisTemplate.opsForHyperLogLog().size(aggregateKey);
  }

  /**
   * Scheduled task to aggregate data by hour.
   * <p>
   * This method runs every 30 seconds and logs the approximate unique user counts
   * for each event key in the current hour bucket.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * // Logs:
   * // Scheduled hour aggregation results 👇:
   * // 📊 hll:tenant-1:US:Android:2023-10-08-15 -> ~150 unique users per hour (2023-10-08-15)
   * // 📊 hll:tenant-2:DE:iOS:2023-10-08-15 -> ~200 unique users per hour (2023-10-08-15)
   * </pre>
   */
  @Scheduled(fixedRate = 30000)
  void aggregateDataByHour() {
    log.info("Scheduled hour aggregation results 👇:");
    listUniqueUsersPerEventKeyByHour(ZonedDateTime.now(ZONE_ID));
  }

  /**
   * Scheduled task to aggregate data by day.
   * <p>
   * This method runs every 35 seconds and logs the approximate unique user count
   * for the default tenant in the current day bucket.
   * </p>
   *
   * <p><b>Example:</b></p>
   * <pre>
   * // Logs:
   * // Scheduled day aggregation done for tenant=default, bucket=2023-10-08, ~500 users
   * </pre>
   */
  @Scheduled(fixedRate = 35000)
  void aggregateDataByDay() {
    ZonedDateTime nowUtc = ZonedDateTime.now(ZONE_ID);
    long count = getUniqueUsersByDay("*", nowUtc);
    log.info("Scheduled day aggregation done for tenant=default, bucket={}, ~{} users",
      nowUtc.format(DAY_KEY_FMT), count);
  }
}
