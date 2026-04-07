package com.project.redis_idempotent_order_system.order.service;

import com.project.redis_idempotent_order_system.order.dto.OrderRequestDto;
import com.project.redis_idempotent_order_system.order.dto.OrderResponseDto;
import com.project.redis_idempotent_order_system.order.entity.Order;
import com.project.redis_idempotent_order_system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponseDto createOrder(String idempotencyKey, OrderRequestDto orderRequest) {
        Order order = Order.builder()
                .userId(1L)
                .productName(orderRequest.getProductName())
                .quantity(orderRequest.getQuantity())
                .createdAt(LocalDateTime.now())
                .build();
        Order saveOrder = orderRepository.save(order);
        return OrderResponseDto.from(saveOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문건이 없습니다. id = " + id));
        return OrderResponseDto.from(order);
    }
}
