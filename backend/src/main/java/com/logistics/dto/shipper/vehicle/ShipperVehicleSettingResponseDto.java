package com.logistics.dto.shipper.vehicle;

import com.logistics.enums.ShipperVehicleStatus;
import com.logistics.enums.ShipperVehicleType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ShipperVehicleSettingResponseDto {
    private Integer id;
    private Integer shipperId;
    private ShipperVehicleType vehicleType;
    private Integer maxOrders;
    private Integer maxWeightKg;
    private Integer currentOrders;
    private BigDecimal currentWeightKg;
    private Integer batteryLevel;
    private ShipperVehicleStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
