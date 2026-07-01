package com.logistics.repository;

import com.logistics.entity.DeliveryAttempt;
import com.logistics.enums.DeliveryAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByOrderIdOrderByAttemptNumberDesc(Integer orderId);
    List<DeliveryAttempt> findByOrderIdAndStatus(Integer orderId, DeliveryAttemptStatus status);
    long countByOrderIdAndStatus(Integer orderId, DeliveryAttemptStatus status);
    long countByOrderId(Integer orderId);
}
