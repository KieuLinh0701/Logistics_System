package com.logistics.enums;

public enum OrderCodStatus {
    NONE,           // Không cấu hình COD
    EXPECTED,       // Có COD nhưng CHƯA THU
    PENDING,        // Đã thu, shipper đang giữ tiền
    SUBMITTED,      // Đã đưa vào phiên nộp tiền
    RECEIVED,       // Hệ thống đã nhận tiền
    TRANSFERRED     // Đã chuyển tiền cho shop
}