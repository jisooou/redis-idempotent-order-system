package com.project.redis_idempotent_order_system.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OrderRequestDto {
    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;
}
