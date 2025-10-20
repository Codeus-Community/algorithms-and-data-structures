package com.codeus.outbox.orderservice.controller;

import com.codeus.outbox.orderservice.dto.OrderRequestDto;
import com.codeus.outbox.orderservice.dto.OrderResponseDto;
import com.codeus.outbox.orderservice.entity.Order;
import com.codeus.outbox.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/count")
    public long getAll() {
        return orderService.count();
    }

    @PostMapping
    public OrderResponseDto create(@RequestBody OrderRequestDto request) {
        Order order = orderService.create(request.description());
        return new OrderResponseDto(order);
    }
}
