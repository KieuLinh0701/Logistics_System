package com.logistics.utils;

import java.util.Set;

import com.logistics.enums.OrderStatus;

public class OrderUtils {

    // Những trạng thái mà user được phép chuyển sang "Sẵn sàng để lấy"
    private static final Set<OrderStatus> USER_ALLOWED_TO_READY_STATUSES = Set.of(
            OrderStatus.CONFIRMED);

    public static boolean canUserSetReady(OrderStatus status) {
        return USER_ALLOWED_TO_READY_STATUSES.contains(status);
    }

    // Những trạng thái user được phép hủy
    private static final Set<OrderStatus> USER_CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.READY_FOR_PICKUP);

    public static boolean canUserCancel(OrderStatus status) {
        return USER_CANCELLABLE_STATUSES.contains(status);
    }

    // Những trạng thái mà user được phép chuyển sang trạng thái ĐANG XỬ LÝ
    // (PENDING)
    private static final Set<OrderStatus> STATUSES_ALLOWED_TO_PENDING = Set.of(
            OrderStatus.DRAFT);

    public static boolean canMoveToPending(OrderStatus status) {
        return STATUSES_ALLOWED_TO_PENDING.contains(status);
    }

    // Những trạng thái user được phép xóa
    private static final Set<OrderStatus> STATUSES_ALLOWED_TO_DELETE = Set.of(
            OrderStatus.DRAFT);

    public static boolean canUserDelete(OrderStatus status) {
        return STATUSES_ALLOWED_TO_DELETE.contains(status);
    }

    // Những trạng thái user/manager được phép in vận đơn
    private static final Set<OrderStatus> STATUSES_ALLOWED_TO_PRINT = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.CANCELLED);

    public static boolean canUserPrint(OrderStatus status) {
        return !STATUSES_ALLOWED_TO_PRINT.contains(status);
    }

    public static boolean canManagerPrint(OrderStatus status) {
        return !STATUSES_ALLOWED_TO_PRINT.contains(status);
    }

    // Những trạng thái manager được phép hủy
    private static final Set<OrderStatus> MANAGER_CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.PICKING_UP,
            OrderStatus.PICKED_UP,
            OrderStatus.AT_ORIGIN_OFFICE);

    public static boolean canManagerCancel(OrderStatus status) {
        return MANAGER_CANCELLABLE_STATUSES.contains(status);
    }

    // Những trạng thái Order mà manager được phép tạo chuyến giao hàng
    private static final Set<OrderStatus> VALID_ORDER_STATUSES_FOR_SHIPMENT_CREATION_MANAGER = Set.of(
            OrderStatus.AT_ORIGIN_OFFICE,
            OrderStatus.AT_DEST_OFFICE,
            OrderStatus.RETURNING,
            OrderStatus.IN_TRANSIT);

    public static boolean canManagerCreateShipment(OrderStatus status) {
        return VALID_ORDER_STATUSES_FOR_SHIPMENT_CREATION_MANAGER.contains(status);
    }

    // Những trạng thái Order mà manager được xác nhận là được người dùng bàn giao đến bưu cục
    private static final Set<OrderStatus> VALID_ORDER_STATUSES_FOR_MANAGER_SET_AT_ORIGIN_OFFICE = Set.of(
            OrderStatus.CONFIRMED);

    public static boolean canManagerSetAtOriginOffice(OrderStatus status) {
        return VALID_ORDER_STATUSES_FOR_MANAGER_SET_AT_ORIGIN_OFFICE.contains(status);
    }
}