package com.logistics.utils;

import java.util.Map;
import java.util.Set;

import com.logistics.enums.PaymentSubmissionStatus;

public class PaymentSubmissionUtils {

    private static final Map<PaymentSubmissionStatus, Set<PaymentSubmissionStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map
            .of(
                    PaymentSubmissionStatus.MISMATCHED, Set.of(
                            PaymentSubmissionStatus.ADJUSTED));

    public static boolean canManagerChangeStatus(PaymentSubmissionStatus currentStatus,
            PaymentSubmissionStatus targetStatus) {
        if (currentStatus == null || targetStatus == null)
            return false;
        return MANAGER_ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    // Chuyển trạng thái sang tiếng việt
    private static final Map<String, String> STATUS_MAP = Map.of(
            "PENDING", "Đã nộp tiền",
            "CHECKING", "Đang đối soát",
            "COMPLETED", "Đã đối soát",
            "PARTIAL", "Lệch tiền",
            "CANCELLED", "Đã huỷ");

    public static String translateStatus(String status) {
        if (status == null)
            return "";
        return STATUS_MAP.getOrDefault(status, status);
    }
}