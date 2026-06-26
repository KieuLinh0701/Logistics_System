package com.logistics.constants;

public class AuditLogDescriptionConstant {
    // Address
    public static final String ADDRESS_CREATE = "Thêm địa chỉ mới";
    public static final String ADDRESS_UPDATE = "Cập nhật địa chỉ";
    public static final String ADDRESS_DELETE = "Xóa địa chỉ";
    public static final String ADDRESS_SET_DEFAULT = "Đặt địa chỉ mặc định";

    // Recipient Address
    public static final String RECIPIENT_ADDRESS_CREATE = "Thêm địa chỉ người nhận mới";
    public static final String RECIPIENT_ADDRESS_UPDATE = "Cập nhật địa chỉ người nhận";
    public static final String RECIPIENT_ADDRESS_DELETE = "Xóa địa chỉ người nhận";
    public static final String RECIPIENT_ADDRESS_EXPORT = "Xuất báo cáo danh sách người nhận";

    // Bank Account
    public static final String BANK_ACCOUNT_CREATE = "Thêm tài khoàn ngân hàng mới";
    public static final String BANK_ACCOUNT_UPDATE = "Cập nhật tài khoàn ngân hàng";
    public static final String BANK_ACCOUNT_DELETE = "Xóa tài khoàn ngân hàng";
    public static final String BANK_ACCOUNT_SET_DEFAULT = "Đặt tài khoàn ngân hàng mặc định";

    // Employee
    public static final String EMPLOYEE_CREATE = "Thêm nhân viên mới";
    public static final String EMPLOYEE_UPDATE = "Cập nhật thông tin nhân viên";
    public static final String EMPLOYEE_UPDATE_STATUS = "Cập nhật trạng thái hoạt động của nhân viên";
    public static final String EMPLOYEE_EXPORT = "Xuất báo cáo danh sách nhân viên";
    public static final String EMPLOYEE_EXPORT_PERFORMANCE = "Xuất báo cáo hiệu suất nhân viên";
    public static final String EMPLOYEE_EXPORT_SHIPMENTS = "Xuất báo cáo danh sách chuyến hàng của nhân viên";
    public static final String EMPLOYEE_EXPORT_ASSIGNMENTS = "Xuất báo cáo nhân viên giao hàng và khu vực phân công";

    // Order
    public static final String ORDER_CREATE = "Tạo đơn hàng mới";
    public static final String ORDER_UPDATE = "Cập nhật thông tin đơn hàng";
    public static final String ORDER_PUBLIC = "Chuyển đơn hàng sang xử lý";
    public static final String ORDER_CANCEL = "Hủy đơn hàng";
    public static final String ORDER_DELETE = "Xóa đơn hàng";
    public static final String ORDER_SET_READY_FOR_PICKUP = "Chuyển đơn hàng sang trạng thái sẵn sàng lấy hàng";
    public static final String ORDER_SET_TRANSIT_TO_OFFICE = "Chuyển đơn hàng sang trạng thái đang chuyển về bưu cục";
    public static final String ORDER_EXPORT = "Xuất báo cáo danh sách đơn hàng";
    public static final String ORDER_PRINT = "In đơn hàng";
    public static final String ORDER_SET_AT_ORIGIN_OFFICE = "Cập nhật trạng thái đơn hàng đã đến bưu cục gốc";
    public static final String ORDER_CONFIRM_DESTINATION = "Cập nhật đơn hàng đã đến bưu cục gốc hay chưa";
    public static final String ORDER_CONFIRM_DESTINATION_BULK = "Cập nhật các đơn hàng đã đến bưu cục gốc hay chưa";
    public static final String ORDER_CONFIRM = "Xác nhận đơn hàng";
    public static final String ORDER_RETURNED = "Xác nhận đã hoàn hàng cho đơn hàng hoàn thất bại";
    public static final String ORDER_URGENT_EXPORT = "Xuất báo cáo danh sách đơn hàng khẩn cấp c";
    public static final String ORDER_URGENT_CONFIRM = "Xác nhận đơn hàng khẩn cấp cần xử lý";

    // Payment
    public static final String PAYMENT_VNPAY_CREATE_SETTLEMENT = "Tạo yêu cầu thanh toán đối soát qua VNPAY";

    // Product
    public static final String PRODUCT_CREATE = "Thêm sản phẩm mới";
    public static final String PRODUCT_UPDATE = "Cập nhật thông tin sản phẩm";
    public static final String PRODUCT_DELETE = "Xóa sản phẩm";
    public static final String PRODUCT_CREATE_BULK = "Nhập liệu sản phẩm hàng loạt";
    public static final String PRODUCT_EXPORT = "Xuất báo cáo sản phẩm";

    // Role
    public static final String ROLE_CREATE = "Tạo vai trò mới";
    public static final String ROLE_UPDATE = "Cập nhật thông tin vai trò";
    public static final String ROLE_DELETE = "Xóa vai trò";

    // Settlement Batch
    public static final String SETTLEMENT_EXPORT_LIST = "Xuất báo cáo danh sách phiên đối soát";
    public static final String SETTLEMENT_EXPORT_DETAIL = "Xuất báo cáo chi tiết phiên đối soát";

    // Shipping Request
    public static final String SHIPPING_REQUEST_CREATE = "Tạo yêu cầu hỗ trợ/khiếu nại";
    public static final String SHIPPING_REQUEST_UPDATE = "Cập nhật yêu cầu hỗ trợ/khiếu nại";
    public static final String SHIPPING_REQUEST_CANCEL = "Hủy yêu cầu hỗ trợ/khiếu nại";
    public static final String SHIPPING_REQUEST_EXPORT = "Xuất báo cáo yêu cầu hỗ trợ/khiếu nại";
    public static final String SHIPPING_REQUEST_PROCESSING = "Xử lý yêu cầu hỗ trợ/khiếu nại của khách hàng";
    public static final String SHIPPING_REQUEST_ASSIGN = "Admin phân công yêu cầu vận chuyển cho bưu cục";
    public static final String SHIPPING_REQUEST_UPDATE_STATUS = "Admin cập nhật trạng thái yêu cầu vận chuyển";

    // User Settlement Schedule
    public static final String USER_SETTLEMENT_SCHEDULE_UPDATE = "Cập nhật lịch đối soát";

    // AI Route
    public static final String AI_ROUTE_OPTIMIZE = "Chạy thuật toán tối ưu lộ trình AI";
    public static final String AI_ROUTE_CONFIRM = "Xác nhận kế hoạch lộ trình AI";
    public static final String AI_ROUTE_CANCEL = "Hủy kế hoạch lộ trình AI";

    // Incident Report
    public static final String INCIDENT_REPORT_PROCESSING = "Xử lý báo cáo sự cố vận hành";
    public static final String INCIDENT_REPORT_EXPORT = "Xuất báo cáo sự cố bưu cục";

    // Office
    public static final String OFFICE_UPDATE = "Cập nhật thông tin bưu cục";
    public static final String OFFICE_CREATE = "Tạo mới bưu cục";
    public static final String OFFICE_DELETE = "Xóa bưu cục";

    // Payment Submission Batch
    public static final String PAYMENT_SUBMISSION_BATCH_COMPLETE = "hoàn thành đợt đối soát thanh toán";
    public static final String PAYMENT_SUBMISSION_BATCH_PROCESSING = "Xử lý phiên đối soát thanh toán";
    public static final String PAYMENT_SUBMISSION_BATCH_EXPORT = "Xuất báo cáo phiên đối soát thanh toán";

    // Payment Submission
    public static final String PAYMENT_SUBMISSION_PROCESSING = "Xử lý chi tiết khoản đối soát thanh toán";
    public static final String PAYMENT_SUBMISSION_EXPORT = "Xuất báo cáo chi tiết khoản đối soát";

    // Shipment
    public static final String SHIPMENT_CREATE = "Tạo chuyến hàng mới";
    public static final String SHIPMENT_UPDATE = "Cập nhật thông tin chuyến hàng";
    public static final String SHIPMENT_CANCEL = "Hủy chuyến hàng";
    public static final String SHIPMENT_EXPORT_LIST = "Xuất báo cáo danh sách chuyến hàng";
    public static final String SHIPMENT_EXPORT_ORDERS = "Xuất báo cáo danh sách đơn hàng của chuyến hàng";
    public static final String SHIPMENT_EXPORT_PERFORMANCE = "Xuất báo cáo hiệu suất chuyến hàng của nhân viên";

    // Shipment Order
    public static final String SHIPMENT_ORDER_SAVE = "Lưu danh sách đơn hàng vào chuyến hàng";

    // Shipper Assignment
    public static final String SHIPPER_ASSIGNMENT_CREATE = "Tạo phân công giao hàng mới";
    public static final String SHIPPER_ASSIGNMENT_UPDATE = "Cập nhật phân công giao hàng";
    public static final String SHIPPER_ASSIGNMENT_DELETE = "Xóa phân công giao hàng";
    public static final String SHIPPER_ASSIGNMENT_EXPORT = "Xuất báo cáo phân công giao hàng";

    // Vehicle
    public static final String VEHICLE_UPDATE = "Cập nhật thông tin phương tiện vận tải";
    public static final String VEHICLE_EXPORT = "Xuất báo cáo danh sách phương tiện";
    public static final String VEHICLE_CREATE = "Thêm mới phương tiện vận chuyển";
    public static final String VEHICLE_DELETE = "Xóa thông tin phương tiện vận chuyển";

    // Auth
    public static final String AUTH_LOGIN = "Người dùng đăng nhập vào hệ thống";
    public static final String AUTH_REGISTER = "Đăng ký tài khoản mới";
    public static final String AUTH_PASSWORD_RESET = "Khôi phục mật khẩu tài khoản";

    // User
    public static final String USER_PASSWORD_UPDATE = "Cập nhật mật khẩu cá nhân";
    public static final String USER_EMAIL_UPDATE = "Gửi yêu cầu thay đổi email";
    public static final String USER_PROFILE_UPDATE = "Cập nhật thông tin cá nhân";
    public static final String USER_CREATE = "Tạo tài khoản người dùng mới";
    public static final String USER_UPDATE = "Cập nhật thông tin người dùng";
    public static final String USER_DELETE = "Xóa tài khoản người dùng";

    // Notification
    public static final String NOTIFICATION_MARK_AS_READ = "Đánh dấu thông báo là đã đọc";
    public static final String NOTIFICATION_MARK_ALL_AS_READ = "Đánh dấu tất cả thông báo là đã đọc";

    // Employee Leave
    public static final String EMPLOYEE_LEAVE_REQUEST_CREATE = "Nhân viên tạo đơn xin nghỉ phép";
    public static final String EMPLOYEE_LEAVE_REQUEST_CANCEL = "Nhân viên hủy đơn xin nghỉ phép";
    public static final String EMPLOYEE_LEAVE_REQUEST_APPROVE = "Quản lý duyệt đơn xin nghỉ phép";

    // Job Posting
    public static final String JOB_POSTING_CREATE = "Tạo mới tin tuyển dụng";
    public static final String JOB_POSTING_UPDATE = "Cập nhật tin tuyển dụng";
    public static final String JOB_POSTING_DELETE = "Xóa tin tuyển dụng";

    // Job Application
    public static final String JOB_APPLICATION_CREATE = "Ứng viên nộp hồ sơ ứng tuyển";
    public static final String JOB_APPLICATION_UPDATE_STATUS = "Cập nhật trạng thái hồ sơ ứng tuyển";

    // Fee Configuration
    public static final String FEE_CONFIG_CREATE = "Tạo cấu hình phí mới";
    public static final String FEE_CONFIG_UPDATE = "Cập nhật cấu hình phí";
    public static final String FEE_CONFIG_DELETE = "Xóa cấu hình phí";

    // Promotion
    public static final String PROMOTION_CREATE = "Tạo chương trình khuyến mãi mới";
    public static final String PROMOTION_UPDATE = "Cập nhật chương trình khuyến mãi";
    public static final String PROMOTION_DELETE = "Xóa chương trình khuyến mãi";

    // Service Type
    public static final String SERVICE_TYPE_CREATE = "Tạo loại dịch vụ mới";
    public static final String SERVICE_TYPE_UPDATE = "Cập nhật loại dịch vụ";
    public static final String SERVICE_TYPE_DELETE = "Xóa loại dịch vụ";

    // Order - Shipper actions
    public static final String ORDER_CLAIM_REQUEST = "Yêu cầu nhận đơn lấy hàng";
    public static final String ORDER_CLAIM = "Nhận đơn hàng thành công";
    public static final String ORDER_UNCLAIM = "Hủy nhận đơn hàng";
    public static final String ORDER_PICKED_UP = "Xác nhận đã lấy hàng";
    public static final String ORDER_DELIVERY_SUCCESS = "Giao hàng thành công";
    public static final String ORDER_DELIVERY_FAILED = "Ghi nhận giao hàng thất bại";
    public static final String ORDER_DELIVERY_ATTEMPT = "Ghi nhận lần giao hàng";
    public static final String ORDER_RETURN_FAILED = "Trả hàng giao thất bại về bưu cục";
    public static final String ORDER_RETURN_TO_ORIGIN = "Trả hàng về kho gốc";

    // Incident Report
    public static final String INCIDENT_REPORT_CREATE = "Tạo báo cáo sự cố vận chuyển";

    // Shipper Vehicle Setting
    public static final String SHIPPER_VEHICLE_SETTING_UPDATE = "Cập nhật cài đặt phương tiện";

    // === DRIVER MODULE ===

    // Shipment - Driver actions
    public static final String SHIPMENT_START = "Bắt đầu vận chuyển chuyến hàng";
    public static final String SHIPMENT_FINISH = "Hoàn tất vận chuyển chuyến hàng";

    // Vehicle Tracking
    public static final String VEHICLE_TRACKING_UPDATE = "Cập nhật vị trí phương tiện";

    // COD
    public static final String COD_COLLECT = "Thu tiền COD";
    public static final String COD_SUBMIT = "Nộp tiền COD";

    // Support Ticket
    public static final String SUPPORT_TICKET_CREATE = "Tạo ticket hỗ trợ";
    public static final String SUPPORT_TICKET_ASSIGN = "Phân công ticket hỗ trợ";
    public static final String SUPPORT_TICKET_CLOSE = "Đóng ticket hỗ trợ";
    public static final String SUPPORT_TICKET_FORCE_CLOSE = "Buộc đóng ticket hỗ trợ";
    public static final String SUPPORT_TICKET_REOPEN = "Mở lại ticket hỗ trợ";

    // Support Message
    public static final String SUPPORT_MESSAGE_CREATE = "Gửi tin nhắn hỗ trợ";
    public static final String SUPPORT_MESSAGE_MARK_READ = "Đánh dấu tin nhắn đã đọc";

    // Internal Chat
    public static final String INTERNAL_CHAT_MESSAGE_CREATE = "Gửi tin nhắn nội bộ";
    public static final String INTERNAL_CHAT_MESSAGE_MARK_READ = "Đánh dấu tin nhắn nội bộ đã đọc";
    
    // Audit Log
    public static final String AUDIT_LOG_EXPORT_BY_EMPLOYEE = "Xuất báo cáo lịch sử hoạt động của nhân viên";
    public static final String AUDIT_LOG_EXPORT = "Xuất báo cáo lịch sử hoat động";
    public static final String AUDIT_LOG_EXPORT_BY_USER = "Xuất báo cáo lịch sử hoạt động của người đùng";

}
