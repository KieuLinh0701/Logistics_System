package com.logistics.utils;

import java.util.Set;

import com.logistics.enums.OrderStatus;

public class ShipmentOrderUtils {

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng
    private static final Set<OrderStatus> ORDER_STATUSES_NOT_ADDABLE_GENERAL = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PICKING_UP,
            OrderStatus.PICKED_UP,
            OrderStatus.DELIVERING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED,
            OrderStatus.RETURNED);

    public static boolean isOrderAddableGeneral(OrderStatus status) {
        return !ORDER_STATUSES_NOT_ADDABLE_GENERAL.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Shipper của fromOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_SHIPPER_FROM_OFFICE_ASSIGNED = Set.of(
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableIfShipperFromOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_SHIPPER_FROM_OFFICE_ASSIGNED.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Shipper của toOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_SHIPPER_TO_OFFICE_ASSIGNED = Set.of(
            OrderStatus.AT_DEST_OFFICE,
            OrderStatus.FAILED_DELIVERY,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableIfShipperToOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_SHIPPER_TO_OFFICE_ASSIGNED.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Driver của fromOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_DRIVER_FROM_OFFICE_ASSIGNED = Set.of(
            OrderStatus.AT_ORIGIN_OFFICE,
            OrderStatus.IN_TRANSIT,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableIfDriverFromOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_DRIVER_FROM_OFFICE_ASSIGNED.contains(status);
    }

    // Các trạng thái của Order mà Manager được phép thêm vào chuyến hàng khi
    // shipment được gán nhân viên là Driver của toOffice
    private static final Set<OrderStatus> ORDER_STATUSES_ADDABLE_IF_DRIVER_TO_OFFICE_ASSIGNED = Set.of(
            OrderStatus.IN_TRANSIT,
            OrderStatus.RETURNING);

    public static boolean isOrderAddableIfDriverToOfficeAssigned(OrderStatus status) {
        return ORDER_STATUSES_ADDABLE_IF_DRIVER_TO_OFFICE_ASSIGNED.contains(status);
    }

}