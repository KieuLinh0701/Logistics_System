package com.logistics.enums;

public enum PaymentSubmissionStatus {
    PENDING,     // Shipper đang giữ tiền, chưa nộp
    PROCESSING,  // Manager đang xem xét, chưa chốt
    MATCHED,     // Manager xác nhận khớp
    MISMATCHED,  // Lệch, chưa xử lý xong
    ADJUSTED     // Đã chốt (dù lệch)
}