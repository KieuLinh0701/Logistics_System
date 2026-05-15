package com.logistics.enums;

public enum FeeType {
    COD, // Phí thu hộ
    PACKAGING, // Phí đóng gói khi tạo đơn hàng tại quầy
    INSURANCE, // Phí bảo hiểm cho orderValue
    VAT, // Thuế VAT
    VOLUMETRIC_DIVISOR,
    // Hệ số quy đổi khối lượng thể tích (vd: 5000 hoặc 6000), dùng trong công thức: (length * width * height) / divisor để tính khối lượng tính phí
}