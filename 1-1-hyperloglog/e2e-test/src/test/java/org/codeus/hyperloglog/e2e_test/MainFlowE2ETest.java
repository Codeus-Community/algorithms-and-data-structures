package org.codeus.hyperloglog.e2e_test;

import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.assertj.core.data.Percentage;
import org.awaitility.Awaitility;
import org.codeus.hyperloglog.aggregator.AggregationController;
import org.codeus.hyperloglog.common.Event;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.codeus.hyperloglog.common.Constants.BASE_PREFIX;
import static org.codeus.hyperloglog.common.DateTimeUtils.ZONE_ID;
import static org.codeus.hyperloglog.consumer.EventStorageService.CACHE_KEY_PREFIX;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainFlowE2ETest {

  static final int REDIS_CONT_PORT = 6379;

  @Container
  static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_CONT_PORT);
  static RedisClient redisClient;
  static StatefulRedisConnection<String, String> redisConnection;

  static ConfigurableApplicationContext consumerCtx;
  static ConfigurableApplicationContext aggregatorCtx;
  static ConfigurableApplicationContext producerCtx;

  static int consumerPort;
  static int aggregatorPort;
  static int producerPort;

  static String redisHost;
  static int redisPort;

  @BeforeAll
  static void startApps() {
    redis.start();
    redisHost = redis.getHost();
    redisPort = redis.getMappedPort(REDIS_CONT_PORT);
    redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
    redisConnection = redisClient.connect();

    // 1) Start Consumer
    consumerCtx = SpringApplication.run(
      org.codeus.hyperloglog.consumer.ConsumerApplication.class,
      asArgs(Map.of(
        "server.port", "0",
//        "spring.profiles.active", "test-e2e",
        "spring.data.redis.host", redisHost,
        "spring.data.redis.port", String.valueOf(redisPort)
      ))
    );
    consumerPort = envPort(consumerCtx);

    // 2) Start Aggregator
    aggregatorCtx = SpringApplication.run(
      org.codeus.hyperloglog.aggregator.AggregatorApplication.class,
      asArgs(Map.of(
        "server.port", "0",
//        "spring.profiles.active", "test-e2e",
        "spring.data.redis.host", redisHost,
        "spring.data.redis.port", String.valueOf(redisPort),
        "app.scheduling.enabled", "false"
      ))
    );
    aggregatorPort = envPort(aggregatorCtx);

    // 3) Start Producer (pointing to Consumer)
    producerCtx = SpringApplication.run(
      org.codeus.hyperloglog.producer.ProducerApplication.class,
      asArgs(Map.of(
        "server.port", "0",
//        "spring.profiles.active", "test-e2e",
        "app.consumer.base-url", "http://localhost:" + consumerPort,
        "app.scheduling.enabled", "false"

      ))
    );
    producerPort = envPort(producerCtx);
  }

  @BeforeEach
  void clearCache() {
    redisConnection.sync().flushall();
  }


  @AfterAll
  static void stopApps() {
    if (producerCtx != null) producerCtx.close();
    if (aggregatorCtx != null) aggregatorCtx.close();
    if (consumerCtx != null) consumerCtx.close();
    if (redisClient != null) redisClient.close();
    if (redisConnection != null) redisConnection.close();
    redis.stop();
  }


  @Test
  @Order(1)
  void should_produce_events_and_list_events_per_hour() {
    List<String> uniqueUserIds = Stream.of(1, 1, 2, 3, 4, 5, 2, 2, 4, 6).map(number -> "userId-" + number).toList();
    int expectedUniqueUserCount = new HashSet<>(uniqueUserIds).size();

    //Step 1: Send 10 user IDs to Producer to produce events for them
    Consumer<String> producerRequest = userId ->
      given()
        .baseUri("http://localhost:%d".formatted(producerPort))
        .contentType("application/json")
        .body("""
          { "userId":"%s"}
          """.formatted(userId))
        .when()
        .post("/api/producer/produce")
        .then()
        .statusCode(HttpStatus.OK.value());

    uniqueUserIds.forEach(producerRequest);

    //Step 2: Wait, retrieve and assert the number of unique user IDs that were processed by Aggregator
    Awaitility.await().atMost(ofSeconds(10)).pollDelay(ofMillis(500)).pollInterval(ofMillis(200))
      .untilAsserted(() -> {
        long uniqueUserCount = retrieveUniqueUsers();
        assertThat(uniqueUserCount).isCloseTo(expectedUniqueUserCount, Percentage.withPercentage(5.0));
      });
  }

  @Test
  @Order(6)
  void should_produce_events_and_aggregate_events_per_hour() {
    //TODO: add BeforeEach Redis cleaning and AfterAll Redis connection closing
    List<String> uniqueUserIds = Stream.of(1, 1, 2, 3, 4, 5, 2, 2, 4, 6).map(number -> "userId-" + number).toList();
    int expectedUniqueUserCount = new HashSet<>(uniqueUserIds).size();


    //Step 1: Send 10 user IDs to Producer to produce events for them
    Function<String, Event> producerRequest = userId ->
      given()
        .baseUri("http://localhost:%d".formatted(producerPort))
        .contentType("application/json")
        .body("""
          { "userId":"%s"}
          """.formatted(userId))
        .when()
        .post("/api/producer/produce")
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract().as(Event.class);

    Set<ZonedDateTime> timestamps = uniqueUserIds.stream()
      .map(producerRequest)
      .map(event -> toZonedDateTime(event.getTimestamp()))
      .collect(Collectors.toSet());

    //Step 2: Prepare a request to Aggregator to read how many unique user IDs were processed
    Function<ZonedDateTime, AggregationController.AggregationPayload> aggregatorRequest = timestamp ->
      given()
        .baseUri("http://localhost:%d".formatted(aggregatorPort))
        .when()
        .get("/api/aggregator/hour?at=%s".formatted(timestamp))
        .then()
        .onFailMessage("Cannot reach/get result from %s endpoint. Check your implementation of Aggregator service. It didn't provided expected result".formatted("http://localhost:%d%s".formatted(aggregatorPort, "/api/aggregator/hour")))
        .statusCode(200)
        .extract().as(AggregationController.AggregationPayload.class);

    //Step 3: Wait, retrieve and assert the number of unique user IDs that were processed by Aggregator
    Awaitility.await().atMost(ofSeconds(10)).pollDelay(ofMillis(500)).pollInterval(ofMillis(200))
      .untilAsserted(() -> {
        long uniqueUserCount = timestamps.parallelStream().map(aggregatorRequest).map(AggregationController.AggregationPayload::uniqueUsers).mapToLong(Long::longValue).sum();
        assertThat(uniqueUserCount).isCloseTo(expectedUniqueUserCount, Percentage.withPercentage(5.0));
      });
  }


  // --- helpers ---
  private static String[] asArgs(Map<String, String> props) {
    return props.entrySet().stream()
      .map(e -> "--" + e.getKey() + "=" + e.getValue())
      .toArray(String[]::new);
  }

  private static int envPort(ConfigurableApplicationContext ctx) {
    Environment env = ctx.getEnvironment();
    return Integer.parseInt(env.getProperty("local.server.port"));
  }

  private Long retrieveUniqueUsers() {
    RedisCommands<String, String> sync = redisConnection.sync();

    List<String> keys = sync.keys("*");

    if ((!Objects.equals(BASE_PREFIX, CACHE_KEY_PREFIX)) &&
      containsPrefix(keys, BASE_PREFIX) && containsPrefix(keys, CACHE_KEY_PREFIX)) {
      throw new IllegalStateException("Redis test instance contains both keys for  this is unexpected. " +
        "Check `org.codeus.hyperloglog.consumer.EventStorageService.CACHE_KEY_PREFIX` value " +
        "and ensure that all Redis keys produced by `org.codeus.hyperloglog.consumer.EventStorageService` have the same prefix");
    }

    return containsPrefix(keys, BASE_PREFIX) ? mergeForSet(keys, sync) : mergeForHyperLogLog(keys, sync);
  }

  private boolean containsPrefix(List<String> keys, String targetPrefix) {
    return keys.stream().anyMatch(key -> key.startsWith(targetPrefix));
  }

  private ZonedDateTime toZonedDateTime(long timestamp) {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE_ID)
      .withMinute(0)
      .withSecond(0)
      .withNano(0);
  }

  private Long mergeForSet(List<String> keys, RedisCommands<String, String> client) {
    String testAggregationKey = "test:%s:aggregated".formatted(BASE_PREFIX);
    return client.sunionstore(testAggregationKey, keys.toArray(String[]::new));
  }

  private Long mergeForHyperLogLog(List<String> keys, RedisCommands<String, String> client) {
    String testAggregationKey = "test:%s:aggregated".formatted(CACHE_KEY_PREFIX);
    client.pfmerge(testAggregationKey, keys.toArray(String[]::new));
    return client.pfcount(testAggregationKey);
  }
}
