package com.logistics.utils;

import com.logistics.enums.*;

import java.util.Set;

public class OrderUtils {

    // Những trạng thái mà user được phép chuyển sang "Sẵn sàng để lấy"
    private static final Set<OrderStatus> USER_ALLOWED_TO_READY_STATUSES = Set.of(
            OrderStatus.PENDING);

    public static boolean canUserSetReady(OrderStatus status) {
        return USER_ALLOWED_TO_READY_STATUSES.contains(status);
    }

    // Những trạng thái mà user được phép chuyển sang "Đang chuyển về bưu cục"
    private static final Set<OrderStatus> USER_ALLOWED_TO_TRANSIT_TO_OFFICE_STATUSES = Set.of(
            OrderStatus.CONFIRMED);

    public static boolean canUserSetTransitToOffice(OrderStatus status) {
        return USER_ALLOWED_TO_TRANSIT_TO_OFFICE_STATUSES.contains(status);
    }

    // Những trạng thái user được phép hủy
    private static final Set<OrderStatus> USER_CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.TRANSIT_TO_OFFICE,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.URGENT_PICKUP);

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
    private static final Set<OrderStatus> STATUSES_NOT_ALLOWED_TO_PRINT = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.CANCELLED,
            OrderStatus.PICKUP_FAILED_FINAL,
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED);

    public static boolean canUserPrint(OrderStatus status) {
        return !STATUSES_NOT_ALLOWED_TO_PRINT.contains(status);
    }

    public static boolean canManagerPrint(OrderStatus status) {
        return !STATUSES_NOT_ALLOWED_TO_PRINT.contains(status);
    }

    // Những trạng thái manager được phép hủy
    private static final Set<OrderStatus> MANAGER_CANCEL_USER_ORDER_STATUSES = Set.of();

    private static final Set<OrderStatus> MANAGER_CANCEL_OFFICE_ORDER_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PICKING_UP,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.URGENT_PICKUP,
            OrderStatus.AT_ORIGIN_OFFICE);

    public static boolean canManagerCancel(
            OrderStatus status,
            OrderCreatorType creatorType) {
        if (creatorType == OrderCreatorType.USER) {
            return MANAGER_CANCEL_USER_ORDER_STATUSES.contains(status);
        }
        return MANAGER_CANCEL_OFFICE_ORDER_STATUSES.contains(status);
    }

    // Những trạng thái mà manager được phép chuyển sang "Đã hoàn hàng"
    private static final Set<OrderStatus> MANAGER_ALLOWED_TO_RETURNED_STATUSES = Set.of(
            OrderStatus.RETURN_FAILED_FINAL);

    public static boolean canManagerSetReturned(OrderStatus status) {
        return MANAGER_ALLOWED_TO_RETURNED_STATUSES.contains(status);
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

    // Những trạng thái Order mà manager được xác nhận là được người dùng bàn giao
    // đến bưu cục
    private static final Set<OrderStatus> VALID_ORDER_STATUSES_FOR_MANAGER_SET_AT_ORIGIN_OFFICE = Set.of(
            OrderStatus.TRANSIT_TO_OFFICE);

    public static boolean canManagerSetAtOriginOffice(OrderStatus status) {
        return VALID_ORDER_STATUSES_FOR_MANAGER_SET_AT_ORIGIN_OFFICE.contains(status);
    }

    // Những trạng thái mà Manager được phép chuyển sang trạng thái Xác nhận
    // (CONFIRMED)
    private static final Set<OrderStatus> STATUSES_ALLOWED_TO_CONFIRMED_FOR_MANAGER = Set.of(
            OrderStatus.PENDING);

    public static boolean canManagerConfirm(OrderStatus status, OrderPickupType pickupType) {
        return STATUSES_ALLOWED_TO_CONFIRMED_FOR_MANAGER.contains(status)
                && (pickupType == OrderPickupType.AT_OFFICE);
    }

    public static String translateOrderStatus(OrderStatus status) {
        if (status == null)
            return "";

        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING -> "Đang chờ đóng gói";
            case CONFIRMED -> "Đã nhận đơn";
            case TRANSIT_TO_OFFICE -> "Đang chuyển về bưu cục'";
            case READY_FOR_PICKUP -> "Sẵn sàng để lấy";
            case URGENT_PICKUP -> "Ưu tiên lấy hàng";

            case PICKUP_RETRY -> "Lấy hàng thất bại - Thử lại";
            case PICKUP_FAILED_FINAL -> "Lấy hàng thất bại - Dừng";

            case PICKING_UP -> "Đang lấy hàng";
            case PICKED_UP -> "Đã lấy hàng";
            case AT_ORIGIN_OFFICE -> "Tại bưu cục gốc";
            case IN_TRANSIT -> "Đang vận chuyển";
            case AT_DEST_OFFICE -> "Tại bưu cục đích";
            case DELIVERING -> "Đang giao";
            case DELIVERED -> "Đã giao hàng";

            case PARTIAL_DELIVERY -> "Giao hàng một phần";
            case PARTIAL_RETURN -> "Hoàn trả một phần";
            case FAILED_DELIVERY -> "Giao thất bại";
            case DELIVERY_RETRY -> "Giao thất bại - Thử lại";
            case DELIVERY_FAILED_FINAL -> "Giao thất bại - Dừng";

            case CANCELLED -> "Đã hủy";
            case RETURNING -> "Đang hoàn trả";
            case RETURN_RETRY_AT_ORIGIN_OFFICE -> "Đã hoàn về bưu cục xuất phát";
            case RETURN_RETRY -> "Hoàn hàng lại";
            case RETURN_FAILED_FINAL -> "Hoàn hàng thất bại cuối cùng";
            case RETURNED -> "Đã hoàn trả";

            default -> status.name();
        };
    }

    public static String translateOrderPickupType(OrderPickupType type) {
        if (type == null)
            return "";

        return switch (type) {
            case AT_OFFICE -> "Giao tại bưu cục";
            case PICKUP_BY_COURIER -> "Lấy hàng tại nhà";
            default -> type.name();
        };
    }

    public static String translateOrderPayerType(OrderPayerType value) {
        if (value == null)
            return "";

        return switch (value) {
            case CUSTOMER -> "Người nhận";
            case SHOP -> "Người gửi";
            default -> value.name();
        };
    }

    public static String translateOrderPaymentStatus(OrderPaymentStatus value) {
        if (value == null)
            return "";

        return switch (value) {
            case PAID -> "Đã thanh toán";
            case UNPAID -> "Chưa thanh toán";
            case REFUNDED -> "Đã hoàn tiền";
            default -> value.name();
        };
    }

    public static String translateOrderCodStatus(OrderCodStatus value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case NONE -> "Không COD";
            case EXPECTED -> "Chưa thu COD";
            case PENDING -> "Shipper giữ COD";
            case SUBMITTED -> "Đã nộp chờ đối soát";
            case RECEIVED -> "Bưu cục đã nhận";
            case TRANSFERRED -> "Đã chuyển shop";
            default -> value.name();
        };
    }

    public static String translateOrderCreatorType(OrderCreatorType value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case USER -> "Người dùng";
            case MANAGER -> "Quản lý";
            case ADMIN -> "Quản trị viên";
            default -> value.name();
        };
    }
}