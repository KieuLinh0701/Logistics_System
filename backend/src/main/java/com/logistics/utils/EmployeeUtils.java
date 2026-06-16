package com.logistics.utils;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.EmployeeStatus;

public class EmployeeUtils {

    public static String translateEmployeeShift(EmployeeShift value) {
        if (value == null) return "";

        return switch (value) {
            case MORNING -> "Ca sáng";
            case AFTERNOON -> "Ca chiều";
            case EVENING -> "Ca tối";
            case FULL_DAY -> "Cả ngày";
            default -> value.name();
        };
    }

    public static String translateEmployeeStatus(EmployeeStatus value) {
        if (value == null) return "";

        return switch (value) {
            case ACTIVE -> "Đang làm việc";
            case INACTIVE -> "Ngưng hoạt động";
            case LEAVE -> "Đã nghỉ việc";
            default -> value.name();
        };
    }
}