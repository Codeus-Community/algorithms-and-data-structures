package com.codeus.outbox.orderservice.service;

import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.entity.OutboxEventStatus;
import com.codeus.outbox.orderservice.kafka.KafkaPublisher;
import com.codeus.outbox.orderservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        processOutboxEventsByStatus(OutboxEventStatus.NEW);
        processOutboxEventsByStatus(OutboxEventStatus.FAILED);
    }

    private void processOutboxEventsByStatus(OutboxEventStatus status) {
        List<OutboxEvent> events = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(status);

        if (events.isEmpty()) {
            log.debug("\n\nNo outbox events to process for status={}", status);
            return;
        }

        log.info("\n\nProcessing {} outbox events with status={}", events.size(), status);

        for (OutboxEvent event : events) {
            log.info("\n\n➡️ Sending event id={} type={} createdAt={}", event.getId(), event.getEventType(), event.getCreatedAt());

            // Random 20% simulated failure before sending to Kafka
            int random = ThreadLocalRandom.current().nextInt(1, 101); // 1..10
            if (random <= 50) { // 50% chance
                log.error("\n\n❌ Simulated failure: event id={} failed before sending to Kafka", event.getId());
                event.setStatus(OutboxEventStatus.FAILED);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                continue;
            }

            kafkaPublisher.publish(event);
        }
    }
}
