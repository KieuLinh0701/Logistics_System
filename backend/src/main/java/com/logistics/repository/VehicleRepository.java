package com.logistics.repository;

import com.logistics.dto.manager.dashboard.ManagerVehicleStatsDto;
import com.logistics.entity.Vehicle;
import com.logistics.enums.VehicleStatus;
import com.logistics.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer>, JpaSpecificationExecutor<Vehicle> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByOfficeId(Integer officeId);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByType(VehicleType type);

    boolean existsByLicensePlate(String licensePlate);

    // Thống kê tổng quan theo officeId
    @Query("SELECT new com.logistics.dto.manager.dashboard.ManagerVehicleStatsDto(" +
            "COUNT(v), " +
            "COALESCE(SUM(CASE WHEN v.status = com.logistics.enums.VehicleStatus.AVAILABLE THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN v.status = com.logistics.enums.VehicleStatus.IN_USE THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN v.status = com.logistics.enums.VehicleStatus.MAINTENANCE THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN v.status = com.logistics.enums.VehicleStatus.ARCHIVED THEN 1 ELSE 0 END), 0)) " +
            "FROM Vehicle v WHERE v.office.id = :officeId")
    ManagerVehicleStatsDto getVehicleStatsByOffice(@Param("officeId") Integer officeId);

    // Lấy số lượng xe theo VehicleType cho 1 office
    @Query("SELECT v.type, COUNT(v) FROM Vehicle v WHERE v.office.id = :officeId GROUP BY v.type")
    List<Object[]> countVehiclesByTypeForOffice(@Param("officeId") Integer officeId);
}
