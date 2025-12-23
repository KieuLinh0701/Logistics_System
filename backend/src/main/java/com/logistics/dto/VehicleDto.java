package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

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
