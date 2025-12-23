package com.logistics.enums;

public enum PaymentSubmissionBatchStatus {
    PENDING, // Shipper đã nộp tiền, chưa đối soát
    CHECKING, // Manager đang kiểm tra
    COMPLETED, // Đã đối soát xong toàn bộ batch
    PARTIAL, // Có đơn khớp + có đơn lệch
    CANCELLED // Huỷ phiên (hiếm, nhưng nên có)
}