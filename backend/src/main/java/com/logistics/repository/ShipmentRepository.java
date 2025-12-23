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
            "SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.PENDING THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.IN_TRANSIT THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.COMPLETED THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.status = com.logistics.enums.ShipmentStatus.CANCELLED THEN 1 ELSE 0 END)) " +
            "FROM Shipment s " +
            "WHERE s.fromOffice.id = :officeId")
    ManagerShipmentStatsDTO getShipmentStatsByOffice(@Param("officeId") Integer officeId);

    Optional<Shipment> findByCode(String code);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByVehicleId(Integer vehicleId);
    List<Shipment> findByEmployeeId(Integer employeeId);
}
