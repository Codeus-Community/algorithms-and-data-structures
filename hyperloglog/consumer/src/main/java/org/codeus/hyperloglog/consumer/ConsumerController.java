package org.codeus.hyperloglog.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeus.hyperloglog.common.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consumer")
@Slf4j
@RequiredArgsConstructor
public class ConsumerController {

  private final EventStorageService storageService;

  @PostMapping("/event")
  public ResponseEntity<Void> receiveEvent(@RequestBody Event event) {
    log.debug("📥 Received event: tenant={}, user={}, country={}, device={}",
      event.getTenantId(), event.getUserId(), event.getCountry(), event.getDevice());

    storageService.storeEvent(event);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/storage")
  public ResponseEntity<Void> clearAggregates() {
    storageService.clearStorage();
    log.info("🗑️ Cleared storage");
    return ResponseEntity.ok().build();
  }
}
