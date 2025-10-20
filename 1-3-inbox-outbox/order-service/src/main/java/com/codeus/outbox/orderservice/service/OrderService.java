package com.codeus.outbox.orderservice.service;

import com.codeus.outbox.orderservice.entity.Order;
import com.codeus.outbox.orderservice.entity.OutboxEvent;
import com.codeus.outbox.orderservice.entity.OutboxEventStatus;
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
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public long count() {
        return orderRepository.count();
    }

    @Transactional
    public Order create(String description) {
        Order order = new Order();
        order.setDescription(description);
        order.setStatus("NEW");
        order.setCreatedAt(Instant.now());
        order = orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId(order.getId());
        event.setEventType("order-created");
        event.setStatus(OutboxEventStatus.NEW);
        event.setCreatedAt(Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(order);
            event.setPayload(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order event", e);
        }
        outboxRepository.save(event);

        return order;
    }
}
