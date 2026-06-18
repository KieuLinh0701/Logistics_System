package com.logistics.service.shipper;

import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingRequestDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingResponseDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleStatusUpdateRequestDto;
import com.logistics.entity.Employee;
import com.logistics.entity.ShipperVehicle;
import com.logistics.enums.ShipperVehicleStatus;
import com.logistics.enums.ShipperVehicleType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.EmployeeErrorCode;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.ShipperVehicleRepository;
import com.logistics.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ShipperVehicleSettingService {

    private final EmployeeRepository employeeRepository;
    private final ShipperVehicleRepository shipperVehicleRepository;

    @Transactional
    public ShipperVehicleSettingResponseDto getMyVehicleSetting() {
        Employee shipper = getCurrentShipperEmployee();
        ShipperVehicle vehicle = getOrCreateDefaultVehicle(shipper);
        return toResponse(vehicle);
    }

    @Transactional
    public ShipperVehicleSettingResponseDto updateMyVehicleSetting(ShipperVehicleSettingRequestDto request) {
        if (request == null) {
            throw new AppException(CommonErrorCode.BAD_REQUEST);
        }

        Employee shipper = getCurrentShipperEmployee();
        ShipperVehicle vehicle = getOrCreateDefaultVehicle(shipper);

        ShipperVehicleType targetType = request.getVehicleType() != null ? request.getVehicleType() : vehicle.getVehicleType();
        Integer maxOrders = request.getMaxOrders() != null ? request.getMaxOrders() : defaultMaxOrders(targetType);
        Integer maxWeightKg = request.getMaxWeightKg() != null ? request.getMaxWeightKg() : defaultMaxWeight(targetType);

        Integer batteryLevel;
        if (targetType == ShipperVehicleType.MOTORBIKE) {
            batteryLevel = null;
        } else {
            batteryLevel = request.getBatteryLevel() != null ? request.getBatteryLevel() : 100;
        }

        String validationError = validate(targetType, maxOrders, maxWeightKg, batteryLevel);
        if (validationError != null) {
            throw new AppException(CommonErrorCode.BAD_REQUEST);
        }

        vehicle.setVehicleType(targetType);
        vehicle.setMaxOrders(maxOrders);
        vehicle.setMaxWeightKg(maxWeightKg);
        vehicle.setBatteryLevel(batteryLevel);
        vehicle.setNotes(request.getNotes());

        if (vehicle.getStatus() == null) {
            vehicle.setStatus(ShipperVehicleStatus.ACTIVE);
        }

        ShipperVehicle saved = shipperVehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    @Transactional
    public ShipperVehicleSettingResponseDto updateMyVehicleStatus(ShipperVehicleStatusUpdateRequestDto request) {
        if (request == null || request.getStatus() == null) {
            throw new AppException(CommonErrorCode.BAD_REQUEST);
        }

        Employee shipper = getCurrentShipperEmployee();
        ShipperVehicle vehicle = getOrCreateDefaultVehicle(shipper);
        vehicle.setStatus(request.getStatus());

        ShipperVehicle saved = shipperVehicleRepository.save(vehicle);
        return toResponse(saved);
    }

    private Employee getCurrentShipperEmployee() {
        Integer userId = SecurityUtils.getAuthenticatedUserId();
        return employeeRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new AppException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private ShipperVehicle getOrCreateDefaultVehicle(Employee shipper) {
        return shipperVehicleRepository.findByShipperId(shipper.getId())
                .orElseGet(() -> {
                    ShipperVehicle vehicle = new ShipperVehicle();
                    vehicle.setShipper(shipper);
                    vehicle.setVehicleType(ShipperVehicleType.MOTORBIKE);
                    vehicle.setMaxOrders(20);
                    vehicle.setMaxWeightKg(35);
                    vehicle.setCurrentOrders(0);
                    vehicle.setCurrentWeightKg(BigDecimal.ZERO);
                    vehicle.setBatteryLevel(null);
                    vehicle.setStatus(ShipperVehicleStatus.ACTIVE);
                    vehicle.setNotes(null);
                    return shipperVehicleRepository.save(vehicle);
                });
    }

    private Integer defaultMaxOrders(ShipperVehicleType type) {
        return 20;
    }

    private Integer defaultMaxWeight(ShipperVehicleType type) {
        return type == ShipperVehicleType.ELECTRIC_BIKE ? 20 : 35;
    }

    private String validate(ShipperVehicleType type, Integer maxOrders, Integer maxWeightKg, Integer batteryLevel) {
        if (type == null) {
            return "vehicleType là bắt buộc";
        }
        if (maxOrders == null || maxOrders < 1 || maxOrders > 100) {
            return "maxOrders phải trong khoảng 1-100";
        }
        if (maxWeightKg == null || maxWeightKg < 1 || maxWeightKg > 80) {
            return "maxWeightKg phải trong khoảng 1-80";
        }
        if (type == ShipperVehicleType.ELECTRIC_BIKE) {
            if (batteryLevel == null || batteryLevel < 0 || batteryLevel > 100) {
                return "batteryLevel phải trong khoảng 0-100 với ELECTRIC_BIKE";
            }
        } else if (batteryLevel != null && (batteryLevel < 0 || batteryLevel > 100)) {
            return "batteryLevel phải trong khoảng 0-100";
        }
        return null;
    }

    private ShipperVehicleSettingResponseDto toResponse(ShipperVehicle vehicle) {
        return ShipperVehicleSettingResponseDto.builder()
                .id(vehicle.getId())
                .shipperId(vehicle.getShipper().getId())
                .vehicleType(vehicle.getVehicleType())
                .maxOrders(vehicle.getMaxOrders())
                .maxWeightKg(vehicle.getMaxWeightKg())
                .currentOrders(vehicle.getCurrentOrders())
                .currentWeightKg(vehicle.getCurrentWeightKg())
                .batteryLevel(vehicle.getBatteryLevel())
                .status(vehicle.getStatus())
                .notes(vehicle.getNotes())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
