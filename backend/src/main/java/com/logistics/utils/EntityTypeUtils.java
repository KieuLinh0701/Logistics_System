package com.logistics.utils;

import com.logistics.enums.EntityType;

public class EntityTypeUtils {

    public static String translateEntityType(EntityType value) {
        if (value == null) return "";

        return switch (value) {
            case ACCOUNT -> "Tài khoản";
            case ACCOUNT_ROLE -> "Phân quyền tài khoản";
            case ADDRESS -> "Địa chỉ";
            case API_ROUTE_PLAN -> "Kế hoạch lộ trình API";
            case API_ROUTE_PLAN_ROUT -> "Tuyến đường kế hoạch API";
            case API_ROUTE_PLAN_STOP -> "Điểm dừng kế hoạch API";
            case AUDIT_LOG -> "Nhật ký hệ thống";
            case BANK_ACCOUNT -> "Tài khoản ngân hàng";
            case DELIVERY_ATTEMPT -> "Lần giao hàng";
            case EMPLOYEE -> "Nhân viên";
            case EMPLOYEE_LEAVE_REQUEST -> "Đơn xin nghỉ phép";
            case FEE_CONFIGURATION -> "Cấu hình phí";
            case FEEDBACK -> "Phản hồi";
            case INCIDENT_REPORT -> "Báo cáo sự cố";
            case JOB_APPLICATION -> "Đơn ứng tuyển";
            case JOB_POSTING -> "Tin tuyển dụng";
            case NOTIFICATION -> "Thông báo";
            case OFFICE -> "Văn phòng";
            case ORDER -> "Đơn hàng";
            case ORDER_HISTORY -> "Lịch sử đơn hàng";
            case ORDER_PRODUCT -> "Sản phẩm trong đơn hàng";
            case OTP -> "Mã OTP";
            case PAYMENT_SUBMISSION -> "Yêu cầu thanh toán";
            case PAYMENT_SUBMISSION_BATCH -> "Lô thanh toán";
            case PAYMENT_SUBMISSION_ITEM -> "Chi tiết thanh toán";
            case PERMISSION_API -> "Quyền truy cập API";
            case PERMISSION_GROUP -> "Nhóm quyền";
            case PERMISSION_GROUP_API -> "Nhóm quyền API";
            case PERMISSION_MODULE -> "Module phân quyền";
            case PICKUP_ATTEMPT -> "Lần lấy hàng";
            case PRODUCT -> "Sản phẩm";
            case PROMOTION -> "Khuyến mãi";
            case REGION -> "Khu vực";
            case ROLE -> "Vai trò";
            case SERVICE_TYPE -> "Loại dịch vụ";
            case SETTLEMENT_BATCH -> "Lô quyết toán";
            case SETTLEMENT_TRANSACTION -> "Giao dịch quyết toán";
            case SHIPMENT -> "Lô hàng";
            case SHIPMENT_ORDER -> "Đơn hàng vận chuyển";
            case SHIPPER_ASSIGNMENT -> "Phân công shipper";
            case SHIPPING_RATE -> "Cước phí vận chuyển";
            case SHIPPING_REQUEST -> "Yêu cầu vận chuyển";
            case SHIPPING_REQUEST_ATTACHMENT -> "Tệp đính kèm vận chuyển";
            case SHOP_WORK_HISTORY -> "Lịch sử làm việc của shop";
            case SUPPORT_MESSAGE -> "Tin nhắn hỗ trợ";
            case SUPPORT_TICKET -> "Phiếu hỗ trợ";
            case INTERNAL_CHAT_ROOM -> "Phòng chat nội bộ";
            case INTERNAL_CHAT_MESSAGE -> "Tin nhắn chat nội bộ";
            case SYSTEM_CONFIG -> "Cấu hình hệ thống";
            case USER -> "Người dùng";
            case USER_PROMOTION -> "Khuyến mãi người dùng";
            case USER_SETTLEMENT_SCHEDULE -> "Lịch quyết toán người dùng";
            case VEHICLE -> "Phương tiện";
            case VEHICLE_TRACKING -> "Theo dõi phương tiện";
        };
    }
}