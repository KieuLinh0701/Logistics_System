package com.logistics.mapper;

import com.logistics.dto.VehicleDto;
import com.logistics.entity.Vehicle;

public class VehicleMapper {

    public static VehicleDto toDto(Vehicle entity) {
        if (entity == null) {
            return null;
        }

        return new VehicleDto(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getType().name(),
                entity.getCapacity(),
                entity.getStatus().name(),
                entity.getDescription(),
                entity.getLastMaintenanceAt(),
                entity.getNextMaintenanceDue(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getGpsDeviceId());
    }
}