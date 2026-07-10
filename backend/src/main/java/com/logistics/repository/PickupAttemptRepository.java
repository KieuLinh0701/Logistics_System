package com.logistics.repository;

import com.logistics.entity.PickupAttempt;
import com.logistics.enums.PickupAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PickupAttemptRepository extends JpaRepository<PickupAttempt, Long> {
    long countByOrderIdAndStatus(Integer orderId, PickupAttemptStatus status);
    List<PickupAttempt> findByOrderIdOrderByAttemptedAtDesc(Integer orderId);

    @Query("SELECT pa FROM PickupAttempt pa " +
            "JOIN FETCH pa.order o " +
            "JOIN FETCH pa.shipper s " +
            "WHERE (o.fromOffice.id = :officeId OR o.currentOffice.id = :officeId) " +
            "AND (:from IS NULL OR pa.attemptedAt >= :from) " +
            "AND (:to IS NULL OR pa.attemptedAt <= :to) " +
            "ORDER BY pa.attemptedAt DESC")
    List<PickupAttempt> findByOfficeAndDateRange(
            @Param("officeId") Integer officeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
