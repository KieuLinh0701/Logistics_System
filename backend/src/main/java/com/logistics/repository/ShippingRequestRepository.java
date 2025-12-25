package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerShippingRequestStatsDTO;
import com.logistics.entity.Order;
import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingRequestRepository
                extends JpaRepository<ShippingRequest, Integer>, JpaSpecificationExecutor<ShippingRequest> {
        Optional<ShippingRequest> findByIdAndUserId(Integer id, int userId);

        @Query("""
                            SELECT r
                            FROM ShippingRequest r
                            WHERE r.user.id = :userId
                              AND r.requestType = :type
                              AND r.status IN :activeStatuses
                              AND (:order IS NULL OR r.order = :order)
                              AND (:requestContent IS NULL OR r.requestContent = :requestContent)
                        """)
        List<ShippingRequest> findActiveRequests(
                        @Param("userId") Integer userId,
                        @Param("type") ShippingRequestType type,
                        @Param("activeStatuses") Set<ShippingRequestStatus> activeStatuses,
                        @Param("order") Order order,
                        @Param("requestContent") String requestContent);

        // Thống kê tổng quan theo officeId
        @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerShippingRequestStatsDTO(" +
                        "COUNT(sr), " +
                        "COALESCE(SUM(CASE WHEN sr.status = com.logistics.enums.ShippingRequestStatus.PENDING THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN sr.status = com.logistics.enums.ShippingRequestStatus.PROCESSING THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN sr.status = com.logistics.enums.ShippingRequestStatus.RESOLVED THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN sr.status = com.logistics.enums.ShippingRequestStatus.REJECTED THEN 1 ELSE 0 END), 0), "
                        +
                        "COALESCE(SUM(CASE WHEN sr.status = com.logistics.enums.ShippingRequestStatus.CANCELLED THEN 1 ELSE 0 END), 0)) "
                        +
                        "FROM ShippingRequest sr WHERE sr.office.id = :officeId")
        ManagerShippingRequestStatsDTO getShippingRequestStatsByOffice(@Param("officeId") Integer officeId);

        @Query("""
                                SELECT r FROM ShippingRequest r
                                LEFT JOIN FETCH r.order o
                                WHERE o.id = :orderId AND r.requestType = com.logistics.enums.ShippingRequestType.DELIVERY_REMINDER
                        """)
        Optional<ShippingRequest> findDeliveryReminderByOrderId(@Param("orderId") Integer orderId);

        @Query("""
                                SELECT r FROM ShippingRequest r
                                LEFT JOIN FETCH r.order o
                                WHERE r.id = :id
                        """)
        Optional<ShippingRequest> findByIdWithOrder(@Param("id") Integer id);

}
