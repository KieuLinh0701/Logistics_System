package com.logistics.enums;

public class Promotion {
    public enum PromotionDiscountType {
        PERCENTAGE, // giảm theo %
        FIXED       // giảm theo số tiền cố định
    }

    public enum PromotionStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED
    }
}
