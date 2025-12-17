package com.logistics.enums;

public enum SettlementStatus {
    PENDING, // Phiên vừa được tạo, đang tính toán, chưa chuyển tiền
    PARTIAL, // Chuyển tiền một phần, còn nợ một phần
    COMPLETED, // Đã chuyển hết tiền, hoàn tất
    FAILED // Có lỗi khi chuyển tiền, cần kiểm tra
}
