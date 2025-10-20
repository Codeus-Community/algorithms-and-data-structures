package com.codeus.inbox.postorderservice.service;

import com.codeus.inbox.postorderservice.entity.InboxEvent;
import com.codeus.inbox.postorderservice.entity.InboxEventStatus;
import com.codeus.inbox.postorderservice.repository.InboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostOrderService {

    private final InboxEventRepository inboxEventRepository;

    public long count() {
        return inboxEventRepository.count();
    }

    public void process(InboxEvent event) {
        int random = ThreadLocalRandom.current().nextInt(1, 11);
        if (random <= 2) { // 20% chance of failure
            throw new RuntimeException("Simulated random failure for testing");
        }
        // some logic...
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processInbox() {
        List<InboxEvent> events = inboxEventRepository.findTop20ByStatusOrderByReceivedAtAsc(InboxEventStatus.FAILED);

        if (events.isEmpty()) {
            log.debug("\n\nNo inbox events to process for FAILED status");
            return;
        }

        log.info("\n\nProcessing {} inbox events with FAILED status", events.size());

        for (InboxEvent event : events) {
            try {
                log.info("\n\nReprocessing inbox event id={} type={}", event.getId(), event.getEventType());
                process(event);
                event.setStatus(InboxEventStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                inboxEventRepository.save(event);
                log.info("\n\nSuccessfully reprocessed inbox event id={}", event.getId());
            } catch (Exception e) {
                log.error("\n\nError while reprocessing inbox event id={} - {}", event.getId(), e.getMessage(), e);
                event.setStatus(InboxEventStatus.FAILED);
                inboxEventRepository.save(event);
                log.warn("\n\nInbox event id={} marked as FAILED again", event.getId());
            }
        }
    }
}
