package com.project.redis_idempotent_order_system.order.dto;

import com.project.redis_idempotent_order_system.order.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponseDto {
    private Long id;
    private Long userId;
    private String productName;
    private Integer quantity;
    private LocalDateTime createdAt;

    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
