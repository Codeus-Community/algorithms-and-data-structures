package com.codeus.outbox.orderservice.kafka;

import com.codeus.outbox.orderservice.entity.Order;
import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    //todo: need to use OutboxEvent
    public void publish(Order order) {
        log.info("\n\n➡️ Publishing event to Kafka topic=order-created");

        //todo: pass id to handle it on consumer side
        //todo: pass payload of our event
        //todo: use .whenComplete to check status of sending kafka event and update event in database
        kafkaTemplate.send("order-created", order.toString());

        int random = ThreadLocalRandom.current().nextInt(1, 101); // 1–100
        boolean shouldDuplicate = random <= 70;
        if (shouldDuplicate) {
            //todo: please make same sending as logic above ^
            kafkaTemplate.send("order-created", order.toString());
        }
    }
}
