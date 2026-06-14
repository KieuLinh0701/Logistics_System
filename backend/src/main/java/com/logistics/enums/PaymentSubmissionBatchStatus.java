package com.logistics.enums;

public enum PaymentSubmissionBatchStatus {
    OPEN,        // Đang gom đơn, shipper chưa đến nộp
    PROCESSING,     // Manager đang xử lý, có thể có MISMATCHED cần điều chỉnh
    COMPLETED,   // Đã đối soát xong (tất cả MATCHED/ADJUSTED)
}