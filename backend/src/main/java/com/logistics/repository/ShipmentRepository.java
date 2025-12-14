package com.logistics.repository;

import com.logistics.entity.Shipment;
import com.logistics.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer>, JpaSpecificationExecutor<Shipment> {
    Optional<Shipment> findByCode(String code);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByVehicleId(Integer vehicleId);
    List<Shipment> findByUserId(Integer userId);
}
