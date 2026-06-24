package com.logistics.utils;

import com.logistics.enums.OrderStatus;

import java.util.Set;

public class ShipmentOrderUtils {

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_GENERAL = Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PICKUP_RETRY,
            OrderStatus.AT_ORIGIN_OFFICE,
            OrderStatus.IN_TRANSIT,
            OrderStatus.AT_DEST_OFFICE,
            OrderStatus.DELIVERY_RETRY,
            OrderStatus.RETURN_AT_ORIGIN_OFFICE,
            OrderStatus.RETURN_RETRY,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableGeneral(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_GENERAL.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Shipper của fromOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_SHIPPER_FROM_OFFICE_ASSIGNED = Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PICKUP_RETRY,
            OrderStatus.RETURN_AT_ORIGIN_OFFICE,
            OrderStatus.RETURN_RETRY
    );

    public static boolean isOrderAddableIfShipperFromOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_SHIPPER_FROM_OFFICE_ASSIGNED.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Shipper của toOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_SHIPPER_TO_OFFICE_ASSIGNED = Set.of(
            OrderStatus.AT_DEST_OFFICE,
            OrderStatus.DELIVERY_RETRY
    );

    public static boolean isOrderAddableIfShipperToOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_SHIPPER_TO_OFFICE_ASSIGNED.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi shipment được gán nhân viên là Driverư
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_DRIVER_ASSIGNED = Set.of(
            OrderStatus.AT_ORIGIN_OFFICE,
            OrderStatus.IN_TRANSIT,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableIfDriverAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_DRIVER_ASSIGNED.contains(status);
    }

}