package com.project.redis_idempotent_order_system.order.repository;

import com.project.redis_idempotent_order_system.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
