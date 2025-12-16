package com.logistics.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.logistics.entity.Office;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;

public class ShippingRequestUtils {

    private static final Map<ShippingRequestStatus, Set<ShippingRequestStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map.of(
        ShippingRequestStatus.PENDING, Set.of(
            ShippingRequestStatus.PROCESSING,
            ShippingRequestStatus.RESOLVED, 
            ShippingRequestStatus.REJECTED
        ),
        ShippingRequestStatus.PROCESSING, Set.of(
            ShippingRequestStatus.RESOLVED,
            ShippingRequestStatus.REJECTED
        )
    );

    public static boolean canManagerChangeStatus(ShippingRequestStatus currentStatus, ShippingRequestStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        return MANAGER_ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    // Những yêu cầu mà contentRequest được null
    private static final Set<ShippingRequestType> REQUEST_TYPE_ALLOWED_EMPTY_CONTENT_REQUEST = Set.of(
            ShippingRequestType.DELIVERY_REMINDER,
            ShippingRequestType.PICKUP_REMINDER);

    public static boolean canUserEmptyContentRequest(ShippingRequestType value) {
        return REQUEST_TYPE_ALLOWED_EMPTY_CONTENT_REQUEST.contains(value);
    }

    // Những yêu cầu mà trackingNumber được null
    private static final Set<ShippingRequestType> REQUEST_TYPE_ALLOWED_EMPTY_TRACKING_NUMBER = Set.of(
            ShippingRequestType.COMPLAINT,
            ShippingRequestType.INQUIRY);

    public static boolean canUserEmptyTrackingNumber(ShippingRequestType value) {
        return REQUEST_TYPE_ALLOWED_EMPTY_TRACKING_NUMBER.contains(value);
    }

    // Những yêu cầu có thể được thực hiện tùy vào trạng thái đơn hàng
    public static boolean isValidRequestForOrder(ShippingRequestType requestType, OrderStatus orderStatus) {
        switch (requestType) {
            case CHANGE_ORDER_INFO:
                return !EnumSet.of(
                        OrderStatus.DRAFT,
                        OrderStatus.DELIVERED,
                        OrderStatus.CANCELLED,
                        OrderStatus.RETURNED).contains(orderStatus);

            case DELIVERY_REMINDER:
                return EnumSet.of(
                        OrderStatus.AT_ORIGIN_OFFICE,
                        OrderStatus.IN_TRANSIT,
                        OrderStatus.AT_DEST_OFFICE,
                        OrderStatus.DELIVERING,
                        OrderStatus.RETURNING).contains(orderStatus);

            case PICKUP_REMINDER:
                return EnumSet.of(
                        OrderStatus.READY_FOR_PICKUP,
                        OrderStatus.PICKING_UP).contains(orderStatus);

            case COMPLAINT:
                return !EnumSet.of(
                        OrderStatus.DRAFT,
                        OrderStatus.PENDING).contains(orderStatus);

            default:
                return false;
        }
    }

    // Những yêu cầu có thể được thực hiện tùy vào trạng thái đơn hàng
    public static Office getValidOfficeForRequest(
            ShippingRequestType requestType,
            OrderStatus orderStatus,
            Office fromOffice,
            Office toOffice) {

        switch (requestType) {
            case CHANGE_ORDER_INFO:
                if (EnumSet.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.READY_FOR_PICKUP,
                        OrderStatus.PICKING_UP,
                        OrderStatus.PICKED_UP,
                        OrderStatus.AT_ORIGIN_OFFICE).contains(orderStatus)) {
                    return fromOffice;
                }
                if (EnumSet.of(
                        OrderStatus.IN_TRANSIT,
                        OrderStatus.AT_DEST_OFFICE,
                        OrderStatus.DELIVERING,
                        OrderStatus.FAILED_DELIVERY,
                        OrderStatus.RETURNING).contains(orderStatus)) {
                    return toOffice;
                }
                break;

            case DELIVERY_REMINDER:
                if (EnumSet.of(
                        OrderStatus.AT_ORIGIN_OFFICE,
                        OrderStatus.IN_TRANSIT).contains(orderStatus)) {
                    return fromOffice;
                }
                if (EnumSet.of(
                        OrderStatus.AT_DEST_OFFICE,
                        OrderStatus.DELIVERING,
                        OrderStatus.RETURNING).contains(orderStatus)) {
                    return toOffice;
                }
                break;

            case PICKUP_REMINDER:
                if (EnumSet.of(
                        OrderStatus.READY_FOR_PICKUP,
                        OrderStatus.PICKING_UP).contains(orderStatus)) {
                    return fromOffice;
                }
                break;
            default:
                return null;
        }

        return null;
    }

    public static String generateRequestCode(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id không được null khi tạo code");
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "SR" + date + id;
    }

    public static final Set<ShippingRequestStatus> ACTIVE_STATUSES = Set.of(ShippingRequestStatus.PENDING,
            ShippingRequestStatus.PROCESSING);

    // Những yêu cầu mà được hủy
    private static final Set<ShippingRequestStatus> REQUEST_TYPE_ALLOWED_CANCEL = Set.of(
            ShippingRequestStatus.PENDING);

    public static boolean canUserCancel(ShippingRequestStatus value) {
        return REQUEST_TYPE_ALLOWED_CANCEL.contains(value);
    }

    public static String getCancelErrorMessage(ShippingRequestStatus status) {
        if (status == ShippingRequestStatus.PROCESSING) {
            return "Yêu cầu không thể hủy vì đang được xử lý";
        } else if (status == ShippingRequestStatus.RESOLVED) {
            return "Yêu cầu không thể hủy vì đã hoàn tất xử lý";
        } else if (status == ShippingRequestStatus.REJECTED) {
            return "Yêu cầu không thể hủy vì đã bị từ chối";
        } else {
            return "Yêu cầu không thể hủy ở trạng thái hiện tại";
        }
    }

}