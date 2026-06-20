package com.logistics.utils;

public class RoleUtils {

    public static String translateSystemRoleName(String value, boolean isSystem) {
        if (value == null) return "";

        if (isSystem) {
            return switch (value) {
                case "User" -> "Chủ cửa hàng";
                case "Driver" -> "Tài xế lái xe";
                case "Manager" -> "Quản lý bưu cục";
                case "Admin" -> "Quản trị viên";
                case "Shipper" -> "Nhân viên giao hàng";
                default -> value;
            };
        }
        return value;
    }
}