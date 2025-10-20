package com.codeus.outbox.orderservice.service;

import com.codeus.outbox.orderservice.entity.Order;
import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.entity.OutboxEventStatus;
import com.codeus.outbox.orderservice.kafka.KafkaPublisher;
import com.codeus.outbox.orderservice.repository.OrderRepository;
import com.codeus.outbox.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaPublisher kafkaPublisher;

    public long count() {
        return orderRepository.count();
    }

    //todo: make this logic atomic
    public Order create(String description) {
        Order order = new Order();
        order.setDescription(description);
        order.setStatus("NEW");
        order.setCreatedAt(Instant.now());
        order = orderRepository.save(order);

        // todo: need to replace with outbox logic
        kafkaPublisher.publish(order);

        return order;
    }
}
