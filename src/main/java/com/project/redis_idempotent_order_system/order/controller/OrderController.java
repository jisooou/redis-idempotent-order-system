package com.project.redis_idempotent_order_system.order.controller;

import com.project.redis_idempotent_order_system.order.dto.OrderRequestDto;
import com.project.redis_idempotent_order_system.order.dto.OrderResponseDto;
import com.project.redis_idempotent_order_system.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public OrderResponseDto createOrder(@Valid @RequestBody OrderRequestDto orderRequest) {
        return orderService.createOrder(orderRequest);
    }

    @GetMapping("/{id}")
    public OrderResponseDto getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

}
