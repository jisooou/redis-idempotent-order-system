package com.project.redis_idempotent_order_system.order.service;

import com.project.redis_idempotent_order_system.order.dto.OrderRequestDto;
import com.project.redis_idempotent_order_system.order.dto.OrderResponseDto;
import com.project.redis_idempotent_order_system.order.entity.Order;
import com.project.redis_idempotent_order_system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String IDEMPOTENCY_PREFIX = "Idempotency-key: ";
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE_PREFIX = "DONE: ";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public OrderResponseDto createOrder(String idempotencyKey, OrderRequestDto orderRequest) {
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING, IDEMPOTENCY_TTL);
        if (Boolean.FALSE.equals(isFirstRequest)) {
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value != null && value.startsWith(DONE_PREFIX)) {
                Long orderId = Long.parseLong(value.substring(DONE_PREFIX.length()));
                Order existOrder = orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 주문건이 없습니다. Id = " + orderId));
                return OrderResponseDto.from(existOrder);
            }
            if (PROCESSING.equals(value)) {
                throw new IllegalArgumentException("이미 처리 중인 요청입니다. 잠시 후 다시 요청해 주세요.");
            }
            throw new IllegalArgumentException("알 수 없는 요청입니다.");
        }
        try {
            Order order = Order.builder()
                    .userId(1L)
                    .productName(orderRequest.getProductName())
                    .quantity(orderRequest.getQuantity())
                    .createdAt(LocalDateTime.now())
                    .build();
            Order saveOrder = orderRepository.save(order);

            redisTemplate.opsForValue()
                    .set(redisKey, DONE_PREFIX + saveOrder.getId(), IDEMPOTENCY_TTL);

            return OrderResponseDto.from(saveOrder);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }

    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문건이 없습니다. id = " + id));
        return OrderResponseDto.from(order);
    }
}
