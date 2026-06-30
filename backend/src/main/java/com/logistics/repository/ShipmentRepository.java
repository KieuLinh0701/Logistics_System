package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerShipmentStatsDTO;
import com.logistics.entity.Shipment;
import com.logistics.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer>, JpaSpecificationExecutor<Shipment> {

    // Thống kê tổng quan theo officeId (fromOffice)
    @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerShipmentStatsDTO(" +
            "COUNT(s), " +
            "COALESCE(SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.PENDING THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.IN_TRANSIT THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.COMPLETED THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.CANCELLED THEN 1 ELSE 0 END), 0)) " +
            "FROM Shipment s " +
            "WHERE s.fromOffice.id = :officeId")
    ManagerShipmentStatsDTO getShipmentStatsByOffice(@Param("officeId") Integer officeId);

    Optional<Shipment> findByCode(String code);

    List<Shipment> findByStatus(ShipmentStatus status);

    List<Shipment> findByVehicleId(Integer vehicleId);

    List<Shipment> findByEmployeeId(Integer employeeId);

    @Query("""
                SELECT s FROM Shipment s
                WHERE s.employee.id = :employeeId
                  AND s.type = com.logistics.enums.ShipmentType.DELIVERY
                  AND s.status = com.logistics.enums.ShipmentStatus.IN_TRANSIT
                ORDER BY s.createdAt DESC
            """)
    Optional<Shipment> findActivePickupShipmentByEmployee(@Param("employeeId") Integer employeeId);

    @Query("""
                SELECT s FROM Shipment s
                WHERE s.type = com.logistics.enums.ShipmentType.DELIVERY
                  AND s.status = com.logistics.enums.ShipmentStatus.IN_TRANSIT
                  AND s.employee.id = :employeeId
                  AND EXISTS (
                      SELECT 1 FROM ShipmentOrder so
                      WHERE so.shipment.id = s.id AND so.order.id = :orderId
                  )
            """)
    Optional<Shipment> findActiveDeliveryShipmentForOrder(
            @Param("employeeId") Integer employeeId,
            @Param("orderId") Integer orderId);

    @Query("""
                SELECT s FROM Shipment s
                LEFT JOIN FETCH s.employee employee
                LEFT JOIN FETCH s.vehicle vehicle
                LEFT JOIN FETCH s.fromOffice fromOffice
                LEFT JOIN FETCH s.toOffice toOffice
                WHERE s.type = com.logistics.enums.ShipmentType.DELIVERY
                  AND s.employee.id = :employeeId
                  AND s.status IN (
                      com.logistics.enums.ShipmentStatus.PENDING,
                      com.logistics.enums.ShipmentStatus.IN_TRANSIT)
                ORDER BY s.createdAt DESC
            """)
    List<Shipment> findActiveDeliveryShipmentsByEmployee(@Param("employeeId") Integer employeeId);

    Optional<Shipment> findByEmployeeIdAndId(Integer employeeId, Integer id);

    @Query("""
                SELECT s FROM Shipment s
                WHERE s.type = com.logistics.enums.ShipmentType.DELIVERY
                  AND s.status = com.logistics.enums.ShipmentStatus.PENDING
                  AND s.employee.id = :employeeId
                  AND EXISTS (
                      SELECT 1 FROM ShipmentOrder so
                      WHERE so.shipment.id = s.id AND so.order.id = :orderId
                  )
            """)
    Optional<Shipment> findPendingDeliveryShipmentForOrder(
            @Param("employeeId") Integer employeeId,
            @Param("orderId") Integer orderId);
}
