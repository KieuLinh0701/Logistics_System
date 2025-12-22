package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrderStatsDTO {
    private long total;                 // Tổng số đơn
    private long pending;               // Đơn chờ xác nhận (PENDING) -> fromOffice
    private long confirmed;             // Đơn đã xác nhận (CONFIRMED) -> fromOffice
    private long readyForPickup;        // Đơn sẵn cho shipper lấy (READY_FOR_PICKUP) -> fromOffice
    private long pickingOrPicked;       // Shipper đang lấy / đã lấy (PICKING / PICKED) -> fromOffice
    private long inWarehouse;           // Đơn trong kho (AT_ORIGIN_OFFICE -> fromOffice / AT_DEST_OFFICE -> toOffice)
    private long customerAtOffice;      // Khách chuẩn bị mang đến kho (CONFIRMED + pickupType = AT_OFFICE -> fromOffice)
    private long delivering;            // Đơn đang đi giao (DELIVERING --> toOffice)
    private long delivered;             // Đơn hoàn tất (DELIVERED -> toOffice)
    private long returned;             // Đơn hoàn tất trả về (RETURNED -> fromOffice)
    private long returning;             // Đơn đang trả về (RETURNING -> fromOffice)
    private long failedDelivery;        // Đơn giao thất bại (FAILED_DELIVERY -> toOffice)
}