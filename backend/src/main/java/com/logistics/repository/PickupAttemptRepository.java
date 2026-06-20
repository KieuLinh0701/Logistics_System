package com.logistics.repository;

import com.logistics.entity.PickupAttempt;
import com.logistics.enums.PickupAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupAttemptRepository extends JpaRepository<PickupAttempt, Long> {
    long countByOrderIdAndStatus(Integer orderId, PickupAttemptStatus status);
    List<PickupAttempt> findByOrderIdOrderByAttemptedAtDesc(Integer orderId);
}
