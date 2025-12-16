package com.logistics.utils;

import java.util.Map;
import java.util.Set;

import com.logistics.enums.PaymentSubmissionBatchStatus;

public class PaymentSubmissionBatchUtils {

    // Manager: các trạng thái được phép chuyển tiếp
    private static final Map<PaymentSubmissionBatchStatus, Set<PaymentSubmissionBatchStatus>> MANAGER_ALLOWED_STATUS_TRANSITIONS = Map
            .of(

                    PaymentSubmissionBatchStatus.PENDING, Set.of(
                            PaymentSubmissionBatchStatus.CHECKING,
                            PaymentSubmissionBatchStatus.CANCELLED),

                    PaymentSubmissionBatchStatus.CHECKING, Set.of(
                            PaymentSubmissionBatchStatus.COMPLETED,
                            PaymentSubmissionBatchStatus.PARTIAL),

                    PaymentSubmissionBatchStatus.PARTIAL, Set.of(
                            PaymentSubmissionBatchStatus.COMPLETED));

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