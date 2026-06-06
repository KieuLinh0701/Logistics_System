package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistics.entity.DeliveryAttempt;
import com.logistics.enums.DeliveryAttemptStatus;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByOrderIdOrderByAttemptNumberDesc(Integer orderId);
    long countByOrderIdAndStatus(Integer orderId, DeliveryAttemptStatus status);
    long countByOrderId(Integer orderId);
}
