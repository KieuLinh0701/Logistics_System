package com.logistics.repository;

import com.logistics.entity.AiRoutePlanRoute;
import com.logistics.enums.AiRoutePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRoutePlanRouteRepository extends JpaRepository<AiRoutePlanRoute, Long> {

    @Query("""
            SELECT DISTINCT r FROM AiRoutePlanRoute r
            JOIN FETCH r.plan p
            LEFT JOIN FETCH r.stops s
            LEFT JOIN FETCH s.order
            WHERE r.shipperEmployeeId = :employeeId
              AND p.status = :status
              AND r.isActive = true
            ORDER BY p.confirmedAt DESC
            """)
    List<AiRoutePlanRoute> findConfirmedRoutesForShipper(
            @Param("employeeId") Integer employeeId,
            @Param("status") AiRoutePlanStatus status);

    @Query("""
            SELECT DISTINCT r FROM AiRoutePlanRoute r
            JOIN FETCH r.plan p
            LEFT JOIN FETCH r.stops s
            LEFT JOIN FETCH s.order
            WHERE r.shipperEmployeeId = :employeeId
              AND p.status = :status
              AND r.isActive = true
            ORDER BY r.reoptimizedAt DESC NULLS LAST, r.id DESC
            """)
    List<AiRoutePlanRoute> findActiveConfirmedRoutesForShipper(
            @Param("employeeId") Integer employeeId,
            @Param("status") AiRoutePlanStatus status);

    @Query("""
            SELECT r FROM AiRoutePlanRoute r
            LEFT JOIN FETCH r.plan p
            LEFT JOIN FETCH r.stops s
            LEFT JOIN FETCH s.order
            WHERE r.id = :routeId
            ORDER BY s.stopSequence ASC
            """)
    Optional<AiRoutePlanRoute> findByIdWithDetails(@Param("routeId") Long routeId);

    // ==================== Phase 3C: advisory mirror for Shipment ====================

    /**
     * Tìm active AI route theo shipmentId (dùng để mirror pickup stop vào AiRoutePlanStop
     * khi shipper insert pickup vào shipment). Trả về nhiều route vì một shipment có thể
     * đã qua vài lần re-optimize (version history).
     */
    @Query("""
            SELECT r FROM AiRoutePlanRoute r
            LEFT JOIN FETCH r.stops s
            WHERE r.shipmentId = :shipmentId
              AND r.isActive = true
            ORDER BY CASE WHEN r.reoptimizedAt IS NULL THEN 1 ELSE 0 END ASC,
                     r.reoptimizedAt DESC,
                     r.id DESC
            """)
    List<AiRoutePlanRoute> findActiveByShipmentId(@Param("shipmentId") Integer shipmentId);
}
