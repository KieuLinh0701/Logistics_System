package com.logistics.dto.shipper;

import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperActiveShipmentDto {

    private Integer id;
    private String code;
    private ShipmentStatus status;
    private ShipmentType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Vehicle basic info
    private VehicleInfo vehicle;

    // Employee basic info
    private EmployeeInfo employee;

    // Office basic info
    private OfficeInfo fromOffice;
    private OfficeInfo toOffice;

    // Order count
    private Integer orderCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private Integer id;
        private String licensePlate;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInfo {
        private Integer id;
        private String code;
        private String fullName;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfficeInfo {
        private Integer id;
        private String name;
        private String code;
    }
}
