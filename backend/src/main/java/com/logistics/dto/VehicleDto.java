package com.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDto {
    private Integer id;
    private String licensePlate;
    private String type;
    private BigDecimal capacity;
    private String status;
    private String description;
    private LocalDateTime lastMaintenanceAt;
    private LocalDateTime nextMaintenanceDue;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String gpsDeviceId;
}
