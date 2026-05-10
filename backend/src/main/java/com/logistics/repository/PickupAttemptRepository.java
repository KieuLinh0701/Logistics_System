package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistics.entity.PickupAttempt;
import com.logistics.enums.PickupAttemptStatus;

public interface PickupAttemptRepository extends JpaRepository<PickupAttempt, Long> {
    long countByOrderIdAndStatus(Integer orderId, PickupAttemptStatus status);
    List<PickupAttempt> findByOrderIdOrderByAttemptedAtDesc(Integer orderId);
}
