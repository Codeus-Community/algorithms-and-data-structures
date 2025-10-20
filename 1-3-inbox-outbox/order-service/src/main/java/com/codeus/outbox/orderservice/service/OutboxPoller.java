package com.codeus.outbox.orderservice.service;

import com.codeus.outbox.orderservice.kafka.KafkaPublisher;
import com.codeus.outbox.orderservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final KafkaPublisher kafkaPublisher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        //todo: implement sending Outbox events to kafka
    }
}
