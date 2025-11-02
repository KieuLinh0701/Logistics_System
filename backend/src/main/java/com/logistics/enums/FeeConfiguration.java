package com.logistics.enums;

public class FeeConfiguration {
    public enum FeeType {
        COD, // Phí thu hộ
        PACKAGING, // Phí đóng gói khi tạo đơn hàng tại quầy
        INSURANCE, // Phí bảo hiểm cho orderValue
        VAT, // Thuế VAT
    }

    public enum CodFeeType {
        FIXED, // Phí cố định
        PERCENTAGE // Phí theo %
    }
}
