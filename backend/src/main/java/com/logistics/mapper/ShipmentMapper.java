package com.logistics.mapper;

import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Shipment;
import com.logistics.entity.Vehicle;

public class ShipmentMapper {

    public static ManagerShipmentPerformanceDto toManagerShipmentPerformanceDto(
            Shipment entity,
            long orderCount,
            long totalWeight) {
        if (entity == null) {
            return null;
        }

        return new ManagerShipmentPerformanceDto(
                entity.getId(),
                entity.getCode(),
                mapVehiclePerformance(entity.getVehicle()),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getType() != null ? entity.getType().toString() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                orderCount,
                totalWeight);
    }

    private static ManagerShipmentPerformanceDto.VehicleShipment mapVehiclePerformance(Vehicle v) {
        if (v == null) {
            return null;
        }

        return new ManagerShipmentPerformanceDto.VehicleShipment(
                v.getId(),
                v.getLicensePlate(),
                v.getCapacity());
    }

    public static ManagerShipmentListDto toManagerShipmentListDto(Shipment entity) {
        if (entity == null) {
            return null;
        }

        return new ManagerShipmentListDto(
                entity.getId(),
                entity.getCode(),
                mapVehicle(entity.getVehicle()),
                mapEmployee(entity.getEmployee()),
                mapEmployee(entity.getCreatedBy()),
                mapOffice(entity.getFromOffice()),
                mapOffice(entity.getToOffice()),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getType() != null ? entity.getType().toString() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static ManagerShipmentListDto.VehicleShipment mapVehicle(Vehicle e) {
        if (e == null)
            return null;

        return new ManagerShipmentListDto.VehicleShipment(
                e.getId(),
                e.getLicensePlate(),
                e.getCapacity());
    }

    private static ManagerShipmentListDto.EmployeeShipment mapEmployee(Employee e) {
        if (e == null)
            return null;

        return new ManagerShipmentListDto.EmployeeShipment(
                e.getId(),
                e.getUser() != null ? e.getUser().getLastName() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getCode(),
                e.getUser() != null ? e.getUser().getPhoneNumber() : null,
                e.getUser() != null && e.getUser().getAccount() != null
                        ? e.getUser().getAccount().getEmail()
                        : null);
    }

    private static ManagerShipmentListDto.OfficeShipment mapOffice(Office o) {
        if (o == null)
            return null;

        return new ManagerShipmentListDto.OfficeShipment(
                o.getId(),
                o.getName(),
                o.getPostalCode(),
                o.getCityCode() != null ? o.getCityCode() : null,
                o.getWardCode() != null ? o.getWardCode() : null,
                o.getDetail(),
                o.getLatitude(),
                o.getLongitude());
    }
}