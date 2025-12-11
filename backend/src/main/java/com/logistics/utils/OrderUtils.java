package com.logistics.utils;

import java.util.Set;

import com.logistics.enums.OrderStatus;

public class OrderUtils {

    // Những trạng thái user được phép hủy
    private static final Set<OrderStatus> USER_CANCELLABLE_STATUSES = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED);

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

    // Những trạng thái user được phép in vận đơn
    private static final Set<OrderStatus> STATUSES_ALLOWED_TO_PRINT = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.CANCELLED);

    public static boolean canUserPrint(OrderStatus status) {
        return !STATUSES_ALLOWED_TO_PRINT.contains(status);
    }
}