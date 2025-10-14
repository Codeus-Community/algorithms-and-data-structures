package org.codeus.hyperloglog.producer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.hyperloglog.common.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.codeus.hyperloglog.common.DateTimeUtils.DATE_HOUR_FMT;

/**
 * Service responsible for producing and sending events to consumers.
 * It generates random events, sends them to consumer endpoints, and tracks unique users.
 * The service also simulates virtual time for event timestamps.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventProducerService {

  private final RestTemplate restTemplate = new RestTemplate();
  private final Random random = new Random();
  private final AtomicInteger eventCounter = new AtomicInteger(0);
  @Getter
  private final Map<String, Set<String>> uniqueUserIds = new HashMap<>();

  private static final String[] TENANTS = {"tenant-1", "tenant-2", "tenant-3"};
  private static final String[] COUNTRIES = {"US", "UK", "DE", "FR"};
  private static final String[] DEVICES = {"iOS", "Android", "Web"};
  private static final String EVENT_ENDPOINT = "/api/consumer/event";

  @Value("${app.consumer.base-url:http://localhost:8081,http://localhost:8082}")
  private String[] consumers;

  // Virtual time that increments by 1 hour every 10 seconds
  private long virtualTimeOffset = 0;
  private long lastRealTime = System.currentTimeMillis();

  /**
   * A scheduled job that produces a batch of events and sends them to consumers.
   * Preserves and provides generated user IDs per hour basis.
   * Logs the total number of events produced and the number of unique users every 1000 events.
   */
  @Scheduled(fixedRate = 100)
  public void produceEvents() {
    try {
      // Generate 10 events per batch
      for (int i = 0; i < 10; i++) {
        produceEvent();
      }

      if (eventCounter.get() % 1000 == 0) {
        log.info("✅ Produced {} events total", eventCounter.get());
        log.info("🦄 Produced events for {} unique users", uniqueUserIds.size());
      }
    } catch (Exception e) {
      log.error("❌ Error producing events: {}", e.getMessage());
    }
  }

  /**
   * Produces a specified number of events in batches and sends them to consumers.
   *
   * @param eventCount the total number of events to produce
   * @return the total number of events produced (rounded up to the nearest batch size)
   */
  public int produceEvents(int eventCount) {
    int eventInBatch = 10;
    int batches = (int) Math.ceil((double) eventCount / eventInBatch);
    int totalEventCount = batches * eventInBatch;
    log.info("Starting producing {} events", totalEventCount);

    for (int k = 0; k < batches; k++) {
      produceEvents();
    }

    return totalEventCount;
  }

  /**
   * Produces a single event with a randomly generated user ID and sends it to a consumer.
   */
  public void produceEvent() {
    produceEvent(null);
  }

  /**
   * Produces a single event with a specified or randomly generated user ID,
   * sends it to a consumer, and tracks the event.
   *
   * @param userId the user ID to associate with the event; if null, a random user ID is generated
   * @return the produced event
   * @throws RuntimeException if the consumer returns an unexpected HTTP status
   */
  public Event produceEvent(String userId) {
    String consumerUrl = consumers[random.nextInt(consumers.length)] + EVENT_ENDPOINT;
    Event event = generateRandomEvent(userId);

    ResponseEntity<Void> response = restTemplate.postForEntity(consumerUrl, event, Void.class);

    if (response.getStatusCode().equals(HttpStatus.OK)) {
      log.debug("📨 Produced event: tenant={}, user={}, country={}, device={}",
        event.getTenantId(), event.getUserId(), event.getCountry(), event.getDevice());
      eventCounter.incrementAndGet();
      storeEventUser(event);
    } else {
      throw new RuntimeException("Got unexpected status (%s) from consumer=%s"
        .formatted(response.getStatusCode().toString(), consumerUrl));
    }

    return event;
  }

  /**
   * Retrieves the set of unique user IDs for a specific date and time.
   *
   * @param zonedDateTime the date and time for which to retrieve unique user IDs
   * @return a set of unique user IDs for the specified date and time
   */
  public Set<String> getUniqueUserIdsForDateTime(ZonedDateTime zonedDateTime) {
    return uniqueUserIds.computeIfAbsent(zonedDateTime.format(DATE_HOUR_FMT), ignored -> Collections.emptySet());
  }

  /**
   * Stores the user ID of the given event in the unique user tracking map.
   *
   * @param event the event whose user ID is to be stored
   */
  private void storeEventUser(Event event) {
    uniqueUserIds.compute(
      DATE_HOUR_FMT.format(Instant.ofEpochMilli(event.getTimestamp())),
      (key, uniqueUserIds) -> {
        if(uniqueUserIds == null) {
          uniqueUserIds = new HashSet<>();
        }
        uniqueUserIds.add(event.getUserId());
        return uniqueUserIds;
      }
    );

  }

  /**
   * Generates a random event with a specified or randomly generated user ID.
   * The event includes a tenant ID, country, device, and a virtual timestamp.
   *
   * @param customUserId the user ID to associate with the event; if null, a random user ID is generated
   * @return the generated event
   */
  private Event generateRandomEvent(String customUserId) {
    String tenantId = TENANTS[random.nextInt(TENANTS.length)];
    String userId = customUserId != null ? customUserId : "user-" + random.nextInt(10000); // 10k unique users
    String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
    String device = DEVICES[random.nextInt(DEVICES.length)];

    // Calculate virtual timestamp that advances 1 hour every 10 seconds
    long currentRealTime = System.currentTimeMillis();
    long elapsedRealTime = currentRealTime - lastRealTime;

    // Every 10 seconds of real time = 1 hour of virtual time
    if (elapsedRealTime > 10000) {
      virtualTimeOffset += 3600000; // Add 1 hour in milliseconds
      lastRealTime = currentRealTime; // Update last check time
      log.info("⏰ Virtual time advanced by 1 hour (event time {})", DATE_HOUR_FMT.format(Instant.ofEpochMilli(currentRealTime + virtualTimeOffset)));
    }

    long timestamp = currentRealTime + virtualTimeOffset;

    return new Event(tenantId, userId, country, device, timestamp);
  }

}
