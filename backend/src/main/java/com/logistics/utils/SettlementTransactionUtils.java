package com.logistics.utils;

import com.logistics.enums.SettlementTransactionStatus;
import com.logistics.enums.SettlementTransactionType;

public class SettlementTransactionUtils {

    public static String translateSettlementTransactionType(SettlementTransactionType value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case SHOP_TO_SYSTEM -> "Shop chuyển";
            case SYSTEM_TO_SHOP -> "Hệ thống chuyển";
            default -> value.name();
        };
    }

    public static String translateSettlementTransactionStatus(SettlementTransactionStatus value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case PENDING -> "Đang xử lý";
            case SUCCESS -> "Thành công";
            case FAILED -> "Thất bại";
            default -> value.name();
        };
    }
}