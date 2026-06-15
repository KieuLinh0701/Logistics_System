package com.logistics.enums;

public enum SettlementStatus {
    PENDING, // Phiên vừa được tạo, đang tính toán, chưa chuyển tiền
    COMPLETED, // Đã chuyển hết tiền, hoàn tất, khấu trừ
    FAILED, // Có lỗi khi chuyển tiền, cần kiểm tra
}