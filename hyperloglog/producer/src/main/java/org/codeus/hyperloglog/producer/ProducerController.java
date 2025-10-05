package org.codeus.hyperloglog.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.hyperloglog.common.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

import static org.codeus.hyperloglog.common.DateTimeUtils.parseOrNow;

@RestController
@RequestMapping("/api/producer")
@Slf4j
@RequiredArgsConstructor
public class ProducerController {

  private final EventProducerService producerService;

  @PostMapping("/produce")
  public ResponseEntity<Event> produceEvent(@RequestBody PublishEventRequest request) {
    log.debug("📥 Received a request to produce an event for user{},", request.userId());
    return ResponseEntity.ok(producerService.produceEvent(request.userId()));
  }

  @PostMapping("/produce/many")
  public ResponseEntity<PublishManyEventResponse> produceEvents(@RequestParam(value = "eventCount") int eventCount) {
    log.debug("📥 Received a request to produce {} events", eventCount);
    return ResponseEntity.accepted().body(new PublishManyEventResponse(producerService.produceEvents(eventCount)));
  }

  @GetMapping("/uniqueUsers")
  public ResponseEntity<UniqueUsersResponse> getUniqueUsers(@RequestParam(value = "at", required = false) String atIso) {
    return ResponseEntity.ok(new UniqueUsersResponse(producerService.getUniqueUserIdsForDateTime(parseOrNow(atIso))));
  }

  @GetMapping("/uniqueUsers/all")
  public ResponseEntity<Set<UniqueUsersResponseWithDateTime>> getAllUniqueUsers() {
    Set<UniqueUsersResponseWithDateTime> uniqueUserIdsByDateTime = producerService.getUniqueUserIds().entrySet().stream()
      .map(entry -> new UniqueUsersResponseWithDateTime(entry.getKey(), entry.getValue()))
      .collect(Collectors.toSet());

    return ResponseEntity.ok(uniqueUserIdsByDateTime);
  }

  public record PublishEventRequest(String userId) {}
  public record PublishManyEventResponse(int producedEventsCount) {}

  public record UniqueUsersResponse(int count, Set<String> uniqueUserIds) {
    public UniqueUsersResponse(Set<String> uniqueUserIds) {
      this(uniqueUserIds.size(), uniqueUserIds);
    }
  }

  public record UniqueUsersResponseWithDateTime(String dateTime, int count, Set<String> uniqueUserIds) {
    public UniqueUsersResponseWithDateTime(String dateTime, Set<String> uniqueUserIds) {
      this(dateTime, uniqueUserIds.size(), uniqueUserIds);
    }
  }
}
