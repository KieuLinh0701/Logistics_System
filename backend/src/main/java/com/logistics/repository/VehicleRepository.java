package com.logistics.repository;

import com.logistics.entity.Vehicle;
import com.logistics.enums.VehicleStatus;
import com.logistics.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    List<Vehicle> findByOfficeId(Integer officeId);
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByType(VehicleType type);
    boolean existsByLicensePlate(String licensePlate);
}



