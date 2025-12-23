package com.logistics.repository;

import com.logistics.entity.VehicleTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleTrackingRepository extends JpaRepository<VehicleTracking, Integer> {
    List<VehicleTracking> findByShipmentId(Integer shipmentId);
    List<VehicleTracking> findByVehicleId(Integer vehicleId);
    
    @Query("SELECT vt FROM VehicleTracking vt WHERE vt.shipment.id = :shipmentId ORDER BY vt.recordedAt DESC")
    List<VehicleTracking> findByShipmentIdOrderByRecordedAtDesc(@Param("shipmentId") Integer shipmentId);
    
    @Query("SELECT vt FROM VehicleTracking vt WHERE vt.shipment.id = :shipmentId AND vt.recordedAt >= :startTime ORDER BY vt.recordedAt ASC")
    List<VehicleTracking> findByShipmentIdAndRecordedAtAfter(@Param("shipmentId") Integer shipmentId, @Param("startTime") LocalDateTime startTime);
}

