package org.codeus.hyperloglog.aggregator;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

import static org.codeus.hyperloglog.common.DateTimeUtils.parseOrNow;

@RestController
@RequestMapping("/api/aggregator")
@RequiredArgsConstructor
public class AggregationController {

  private final AggregationService aggregationService;

  @GetMapping("/hour/list")
  public ResponseEntity<?> getHourAggregationListing(@RequestParam(value = "at", required = false) String atIso) {
    ZonedDateTime now = parseOrNow(atIso);
    return ResponseEntity.ok(aggregationService.listUniqueUsersPerEventKeyByHour(now));
  }

  @GetMapping("/hour")
  public ResponseEntity<?> getHourAggregation(@RequestParam(value = "at", required = false) String atIso) {
    ZonedDateTime now = parseOrNow(atIso);
    long count = aggregationService.getUniqueUsersByHour(now);

    return ResponseEntity.ok(new AggregationPayload(now, "hour", count));
  }

  @GetMapping("/{tenant}/hour")
  public ResponseEntity<?> getHourAggregationByTenant(
    @PathVariable("tenant") String tenant,
    @RequestParam(value = "at", required = false) String atIso // optional ISO-8601 timestamp
  ) {
    ZonedDateTime now = parseOrNow(atIso);
    long count = aggregationService.getUniqueUsersByHour(tenant, now);

    return ResponseEntity.ok(new TenantAggregationPayload(tenant, now, "hour", count));
  }

  @GetMapping("/{tenant}/day")
  public ResponseEntity<?> getDayAggregationByTenant(
    @PathVariable("tenant") String tenant,
    @RequestParam(value = "at", required = false) String atIso
  ) {
    ZonedDateTime now = parseOrNow(atIso);
    long count = aggregationService.getUniqueUsersByDay(tenant, now);

    return ResponseEntity.ok(new TenantAggregationPayload(tenant, now, "day", count));
  }

  public record AggregationPayload(ZonedDateTime bucket, String scope, long uniqueUsers) {
  }

  public record TenantAggregationPayload(String tenant, ZonedDateTime bucket, String scope, long uniqueUsers) {
  }
}
