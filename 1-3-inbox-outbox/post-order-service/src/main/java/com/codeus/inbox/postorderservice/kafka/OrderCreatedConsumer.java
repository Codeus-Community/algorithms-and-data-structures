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

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InboxEventRepository inboxRepository;
    private final PostOrderService postOrderService;

    @KafkaListener(topics = "order-created", groupId = "post-order-service")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        //log.info("\n\nReceived Kafka message with key={} and value={}", eventId, record.value());

        // todo: some verification logic

        InboxEvent event = new InboxEvent();
        // todo: populate InboxEvent

        try {
            // Process business logic
            postOrderService.process(event);
            log.info("\n\nSuccessfully processed event id={}", event.getId());

            event.setStatus(InboxEventStatus.PROCESSED);
            inboxRepository.save(event);
        } catch (Exception e) {
            log.error("\n\nError while processing event - {}", e.getMessage(), e);
            event.setStatus(InboxEventStatus.FAILED);
            inboxRepository.save(event);
            log.warn("\n\nEvent id={} marked as FAILED", event.getId());
        }
    }
}
