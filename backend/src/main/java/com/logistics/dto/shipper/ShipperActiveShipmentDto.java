package com.logistics.dto.shipper;

import com.logistics.enums.RouteStopType;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private Integer totalOrders;

    private Integer orderCount;

    // Tiến độ quét mã
    private Integer totalCount;
    private Integer scannedCount;
    private Boolean isReadyToStart;

    private Integer scannedDeliveryStopCount;

    private Integer totalDeliveryStopCount;

    private List<ShipmentOrderInfo> orders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentOrderInfo {
        private Integer shipmentId;
        private Integer orderId;
        private RouteStopType stopType;
        private Integer stopSequence;
        private String trackingNumber;
        private String recipientName;
        private LocalDateTime scannedAt;
    }

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
