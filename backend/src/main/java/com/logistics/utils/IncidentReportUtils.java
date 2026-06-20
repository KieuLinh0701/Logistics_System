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
            case RECIPIENT_NOT_AVAILABLE -> "Người nhận không có mặt";
            case WRONG_ADDRESS -> "Sai địa chỉ";
            case PACKAGE_DAMAGED -> "Hàng bị hỏng";
            case RECIPIENT_REFUSED -> "Người nhận từ chối";
            case SECURITY_ISSUE -> "Vấn đề an ninh";
            case OTHER -> "Khác";
            default -> value.name();
        };
    }
}