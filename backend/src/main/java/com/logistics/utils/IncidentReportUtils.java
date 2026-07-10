package com.logistics.utils;

import com.logistics.enums.IncidentPriority;
import com.logistics.enums.IncidentStatus;
import com.logistics.enums.IncidentType;

import java.util.Map;
import java.util.Set;

public class IncidentReportUtils {

    private static final Map<IncidentStatus, Set<IncidentStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map.of(
        IncidentStatus.PENDING, Set.of(
            IncidentStatus.PROCESSING,
            IncidentStatus.RESOLVED, 
            IncidentStatus.REJECTED
        ),
        IncidentStatus.PROCESSING, Set.of(
            IncidentStatus.RESOLVED,
            IncidentStatus.REJECTED
        )
    );

    public static boolean canManagerChangeStatus(IncidentStatus currentStatus, IncidentStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        return MANAGER_ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public static String translateIncidentPriority(IncidentPriority value) {
        if (value == null) return "";
        return switch (value) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            default -> value.name();
        };
    }

    public static String translateIncidentStatus(IncidentStatus value) {
        if (value == null) return "";
        return switch (value) {
            case PENDING -> "Chờ xử lý";
            case PROCESSING -> "Đang xử lý";
            case RESOLVED -> "Đã giải quyết";
            case REJECTED -> "Từ chối";
            default -> value.name();
        };
    }

    public static String translateIncidentType(IncidentType value) {
        if (value == null) return "";
        return switch (value) {
            case DAMAGED_PARCEL -> "Hàng hóa bị hư hỏng";
            case LOST_PARCEL -> "Hàng hóa bị thất lạc";
            case COD_DISPUTE -> "Tranh chấp COD";
            case CUSTOMER_CONFLICT -> "Tranh chấp với khách hàng";
            case SAFETY_INCIDENT -> "Sự cố an toàn";
            case VEHICLE_BREAKDOWN -> "Phương tiện hư hỏng";
            case TRAFFIC_ACCIDENT -> "Tai nạn giao thông";
            case SYSTEM_ERROR -> "Lỗi hệ thống";
            case BARCODE_SCAN_ERROR -> "Lỗi quét mã vận đơn";
            case WRONG_ORDER_ASSIGNMENT -> "Phân công sai đơn hàng";
            case OFFICE_OPERATION_ISSUE -> "Sự cố tại bưu cục";
            case DELIVERY_EXCEPTION -> "Sự cố giao hàng bất thường";
            case PICKUP_EXCEPTION -> "Sự cố lấy hàng bất thường";
            case RETURN_EXCEPTION -> "Sự cố hoàn hàng";
            case OTHER -> "Khác";
            default -> value.name();
        };
    }
}