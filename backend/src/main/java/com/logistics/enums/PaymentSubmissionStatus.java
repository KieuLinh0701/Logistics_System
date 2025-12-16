package com.logistics.enums;

public enum PaymentSubmissionStatus {
    PENDING, // Đã tạo từ đơn, shipper đang giữ tiền
    IN_BATCH, // Đã đưa vào phiên nộp tiền
    MATCHED, // Khớp tiền
    MISMATCHED, // Lệch tiền
    ADJUSTED // Đã điều chỉnh & chốt
}