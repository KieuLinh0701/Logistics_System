package com.logistics.utils;

import com.logistics.enums.ProductStatus;
import com.logistics.enums.ProductType;

public class ProductUtils {

    public static String translateProductType(ProductType value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case FRESH -> "Tươi sống";
            case LETTER -> "Thư từ";
            case GOODS -> "Hàng hóa";
            default -> value.name();
        };
    }

    public static String translateProductStatus(ProductStatus value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case ACTIVE -> "Đang bán";
            case INACTIVE -> "Ngừng bán";
            default -> value.name();
        };
    }
}