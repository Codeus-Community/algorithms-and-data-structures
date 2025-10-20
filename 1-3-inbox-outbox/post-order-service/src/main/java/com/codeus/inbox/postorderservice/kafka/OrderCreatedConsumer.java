package com.codeus.inbox.postorderservice.kafka;

import com.codeus.inbox.postorderservice.entity.InboxEvent;
import com.codeus.inbox.postorderservice.entity.InboxEventStatus;
import com.codeus.inbox.postorderservice.repository.InboxEventRepository;
import com.codeus.inbox.postorderservice.service.PostOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InboxEventRepository inboxRepository;
    private final PostOrderService postOrderService;

    @KafkaListener(topics = "order-created", groupId = "post-order-service")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        UUID eventId = UUID.fromString(record.key());
        log.info("\n\nReceived Kafka message with key={} and value={}", eventId, record.value());

        Optional<InboxEvent> existing = inboxRepository.findById(eventId);
        // Check for duplicate event
        if (existing.isPresent()) {
            log.warn("\n\nSkipping duplicate event with id={}", eventId);
            return;
        }

        InboxEvent event = new InboxEvent();
        event.setId(eventId);
        event.setEventType("order-created");
        event.setPayload(record.value());
        event.setReceivedAt(Instant.now());
        event.setStatus(InboxEventStatus.RECEIVED);

        try {
            event = inboxRepository.save(event);
            log.info("\n\nSaved InboxEvent id={} with status={}", event.getId(), event.getStatus());

            // Randomly simulate error with 20% probability
            int random = ThreadLocalRandom.current().nextInt(1, 11);
            if (random <= 2) { // 20% chance of failure
                throw new RuntimeException("Simulated random failure for testing");
            }

            // Process business logic
            postOrderService.process(event);
            log.info("\n\nSuccessfully processed event id={} type={}", event.getId(), event.getEventType());

            event.setStatus(InboxEventStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            inboxRepository.save(event);
            log.info("\n\nEvent id={} marked as PROCESSED", event.getId());
        } catch (Exception e) {
            log.error("\n\nError while processing event id={} type={} - {}", eventId, event.getEventType(), e.getMessage(), e);
            event.setStatus(InboxEventStatus.FAILED);
            inboxRepository.save(event);
            log.warn("\n\nEvent id={} marked as FAILED", event.getId());
        }
    }
}
