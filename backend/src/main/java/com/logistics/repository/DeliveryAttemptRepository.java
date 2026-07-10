package com.logistics.repository;

import com.logistics.entity.DeliveryAttempt;
import com.logistics.enums.DeliveryAttemptStatus;
import com.logistics.enums.DeliveryAttemptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByOrderIdOrderByAttemptNumberDesc(Integer orderId);
    List<DeliveryAttempt> findByOrderIdAndStatus(Integer orderId, DeliveryAttemptStatus status);
    long countByOrderIdAndStatus(Integer orderId, DeliveryAttemptStatus status);
    long countByOrderId(Integer orderId);

    long countByOrderIdAndStatusAndAttemptType(Integer orderId, DeliveryAttemptStatus status, DeliveryAttemptType attemptType);
    long countByOrderIdAndAttemptType(Integer orderId, DeliveryAttemptType attemptType);
    List<DeliveryAttempt> findByOrderIdAndAttemptTypeOrderByAttemptNumberDesc(Integer orderId, DeliveryAttemptType attemptType);
    List<DeliveryAttempt> findByOrderIdAndAttemptTypeAndStatus(Integer orderId, DeliveryAttemptType attemptType, DeliveryAttemptStatus status);

    @Query("SELECT da FROM DeliveryAttempt da " +
            "JOIN FETCH da.order o " +
            "JOIN FETCH da.shipper s " +
            "WHERE (o.fromOffice.id = :officeId OR o.currentOffice.id = :officeId) " +
            "AND (:from IS NULL OR da.attemptedAt >= :from) " +
            "AND (:to IS NULL OR da.attemptedAt <= :to) " +
            "ORDER BY da.attemptedAt DESC")
    List<DeliveryAttempt> findByOfficeAndDateRange(
            @Param("officeId") Integer officeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
