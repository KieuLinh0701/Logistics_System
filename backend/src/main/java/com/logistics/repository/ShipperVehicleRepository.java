package com.logistics.repository;

import com.logistics.entity.ShipperVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperVehicleRepository extends JpaRepository<ShipperVehicle, Integer> {
    Optional<ShipperVehicle> findByShipperId(Integer shipperId);
}
