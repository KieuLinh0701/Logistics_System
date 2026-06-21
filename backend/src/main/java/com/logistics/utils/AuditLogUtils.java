package com.logistics.utils;

import com.logistics.enums.AuditLogAction;
import com.logistics.enums.AuditLogStatus;

public class AuditLogUtils {

    public static String translateAuditLogStatus(AuditLogStatus value) {
        if (value == null) return "";
        return switch (value) {
            case SUCCESS -> "Thành công";
            case FAILED -> "Thất bại";
            case FORBIDDEN -> "Bị từ chối";
        };
    }

    public static String translateAuditLogAction(AuditLogAction value) {
        if (value == null) return "";
        return switch (value) {
            case CREATE -> "Tạo mới";
            case UPDATE -> "Cập nhật";
            case DELETE -> "Xóa";
            case EXPORT -> "Xuất dữ liệu";
            case IMPORT -> "Nhập dữ liệu";
            case PAY -> "Thanh toán";
            case APPROVE -> "Phê duyệt";
            case REJECT -> "Từ chối";
            case CANCEL -> "Hủy";
            case LOGIN -> "Đăng nhập";
            case CONFIRM -> "Xác nhận";
            case PROCESS -> "Xử lý";
            case UPDATE_STATUS -> "Cập nhật trạng thái";
            case PRINT -> "In";
            case REGISTER -> "Đăng ký";
            case PASSWORD_RESET -> "Đặt lại mật khẩu";
        };
    }
}