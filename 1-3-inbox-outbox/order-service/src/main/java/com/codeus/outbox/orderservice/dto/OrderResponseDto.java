package com.codeus.outbox.orderservice.dto;

import com.codeus.outbox.orderservice.entity.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderResponseDto(UUID id, String description, String status, Instant createdAt) {

    public OrderResponseDto(Order order) {
        this(order.getId(), order.getDescription(), order.getStatus(), order.getCreatedAt());
    }
}
