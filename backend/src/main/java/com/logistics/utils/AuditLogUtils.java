package com.logistics.utils;

import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;

public class AuditLogUtils {

    public static String translateAuditLogStatus(AuditLogStatus value) {
        if (value == null) return "";
        return switch (value) {
            default -> value.name();
        };
    }

    public static String translateAuditLogAction(AuditLogAction value) {
        if (value == null) return "";
        return switch (value) {
            default -> value.name();
        };
    }
}