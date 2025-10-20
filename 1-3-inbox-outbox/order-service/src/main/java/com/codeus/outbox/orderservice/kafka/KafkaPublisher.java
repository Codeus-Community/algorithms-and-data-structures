package com.codeus.outbox.orderservice.kafka;

import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.entity.OutboxEventStatus;
import com.codeus.outbox.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxRepository;

    public void publish(OutboxEvent event) {
        int random = ThreadLocalRandom.current().nextInt(1, 101); // 1–100
        boolean shouldDuplicate = random <= 70;

        log.info("\n\n➡️ Publishing event id={} type={} to Kafka topic=order-created", event.getId(), event.getEventType());

        kafkaTemplate.send("order-created", event.getId().toString(), event.getPayload()).whenComplete((result, ex) -> {
            if (ex == null) {
                event.setStatus(OutboxEventStatus.SENT);
                event.setProcessedAt(Instant.now());
                log.info("\n\n✅ Successfully sent event id={} offset={} partition={}", event.getId(), result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            } else {
                event.setStatus(OutboxEventStatus.FAILED);
                log.error("\n\n❌ Failed to send event id={} to Kafka: {}", event.getId(), ex.getMessage(), ex);
            }

            try {
                outboxRepository.save(event);
                log.debug("\n\n💾 Updated status={} for event id={}", event.getStatus(), event.getId());
            } catch (Exception saveEx) {
                log.error("\n\n⚠️ Failed to persist status update for event id={} reason={}", event.getId(), saveEx.getMessage(), saveEx);
            }
        });

        if (shouldDuplicate) {
            kafkaTemplate.send("order-created", event.getId().toString(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.warn("\n\n⚠️ Duplicate send simulated for event id={} offset={} partition={}",
                                    event.getId(),
                                    result.getRecordMetadata().offset(),
                                    result.getRecordMetadata().partition());
                        } else {
                            log.error("\n\n❌ Failed to send duplicate event {} to Kafka", event.getId(), ex);
                        }
                    });
        }
    }
}
