package com.logistics.utils;

import com.logistics.enums.SettlementStatus;

public class SettlementBatchUtils {

    public static String translateSettlementBatchStatus(SettlementStatus value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case PENDING -> "Chờ chuyển tiền";
            case COMPLETED -> "Đã chuyển tiền";
            case FAILED -> "Chuyển tiền thất bại";
            default -> value.name();
        };
    }
}