package com.logistics.utils;

import com.logistics.enums.PaymentSubmissionBatchStatus;

import java.util.Map;
import java.util.Set;

public class PaymentSubmissionBatchUtils {

    // Manager: các trạng thái được phép chuyển tiếp
    private static final Map<PaymentSubmissionBatchStatus, Set<PaymentSubmissionBatchStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map
            .of(
                    PaymentSubmissionBatchStatus.PROCESSING, Set.of(
                            PaymentSubmissionBatchStatus.COMPLETED),

                    PaymentSubmissionBatchStatus.COMPLETED, Set.of());

    public static boolean canManagerChangeStatus(
            PaymentSubmissionBatchStatus currentStatus,
            PaymentSubmissionBatchStatus targetStatus) {
        if (currentStatus == null || targetStatus == null)
            return false;

        return MANAGER_ALLOWED_STATUS_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(targetStatus);
    }

    // Dịch trạng thái sang tiếng Việt
    private static final Map<String, String> STATUS_MAP = Map.of(
            "OPEN", "Đang mở",
            "PROCESSING", "Đang đối soát",
            "COMPLETED", "Đã đối soát");

    public static String translateStatus(String status) {
        if (status == null)
            return "";
        return STATUS_MAP.getOrDefault(status, status);
    }
}