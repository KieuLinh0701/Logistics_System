export const ENTITY_TYPE = [
  'ACCOUNT', 'ACCOUNT_ROLE', 'ADDRESS', 'API_ROUTE_PLAN', 'API_ROUTE_PLAN_ROUT',
  'API_ROUTE_PLAN_STOP', 'AUDIT_LOG', 'BANK_ACCOUNT', 'DELIVERY_ATTEMPT', 'EMPLOYEE',
  'EMPLOYEE_LEAVE_REQUEST', 'FEE_CONFIGURATION', 'FEEDBACK', 'INCIDENT_REPORT',
  'JOB_APPLICATION', 'JOB_POSTING', 'NOTIFICATION', 'OFFICE', 'ORDER',
  'ORDER_HISTORY', 'ORDER_PRODUCT', 'OTP', 'PAYMENT_SUBMISSION', 'PAYMENT_SUBMISSION_BATCH',
  'PAYMENT_SUBMISSION_ITEM', 'PERMISSION_API', 'PERMISSION_GROUP', 'PERMISSION_GROUP_API',
  'PERMISSION_MODULE', 'PICKUP_ATTEMPT', 'PRODUCT', 'PROMOTION', 'REGION', 'ROLE',
  'SERVICE_TYPE', 'SETTLEMENT_BATCH', 'SETTLEMENT_TRANSACTION', 'SHIPMENT',
  'SHIPMENT_ORDER', 'SHIPPER_ASSIGNMENT', 'SHIPPING_RATE', 'SHIPPING_REQUEST',
  'SHIPPING_REQUEST_ATTACHMENT', 'SHOP_WORK_HISTORY', 'SUPPORT_MESSAGE',
  'SUPPORT_TICKET', 'SYSTEM_CONFIG', 'USER', 'USER_PROMOTION',
  'USER_SETTLEMENT_SCHEDULE', 'VEHICLE', 'VEHICLE_TRACKING'
] as const;

export const translateEntityType = (value: string): string => {
  switch (value) {
    case 'ACCOUNT': return 'Tài khoản';
    case 'ACCOUNT_ROLE': return 'Phân quyền tài khoản';
    case 'ADDRESS': return 'Địa chỉ';
    case 'API_ROUTE_PLAN': return 'Kế hoạch lộ trình API';
    case 'API_ROUTE_PLAN_ROUT': return 'Tuyến đường kế hoạch API';
    case 'API_ROUTE_PLAN_STOP': return 'Điểm dừng kế hoạch API';
    case 'AUDIT_LOG': return 'Nhật ký hệ thống';
    case 'BANK_ACCOUNT': return 'Tài khoản ngân hàng';
    case 'DELIVERY_ATTEMPT': return 'Lần giao hàng';
    case 'EMPLOYEE': return 'Nhân viên';
    case 'EMPLOYEE_LEAVE_REQUEST': return 'Đơn xin nghỉ phép';
    case 'FEE_CONFIGURATION': return 'Cấu hình phí';
    case 'FEEDBACK': return 'Phản hồi';
    case 'INCIDENT_REPORT': return 'Báo cáo sự cố';
    case 'JOB_APPLICATION': return 'Đơn ứng tuyển';
    case 'JOB_POSTING': return 'Tin tuyển dụng';
    case 'NOTIFICATION': return 'Thông báo';
    case 'OFFICE': return 'Văn phòng';
    case 'ORDER': return 'Đơn hàng';
    case 'ORDER_HISTORY': return 'Lịch sử đơn hàng';
    case 'ORDER_PRODUCT': return 'Sản phẩm trong đơn hàng';
    case 'OTP': return 'Mã OTP';
    case 'PAYMENT_SUBMISSION': return 'Yêu cầu thanh toán';
    case 'PAYMENT_SUBMISSION_BATCH': return 'Lô thanh toán';
    case 'PAYMENT_SUBMISSION_ITEM': return 'Chi tiết thanh toán';
    case 'PERMISSION_API': return 'Quyền truy cập API';
    case 'PERMISSION_GROUP': return 'Nhóm quyền';
    case 'PERMISSION_GROUP_API': return 'Nhóm quyền API';
    case 'PERMISSION_MODULE': return 'Module phân quyền';
    case 'PICKUP_ATTEMPT': return 'Lần lấy hàng';
    case 'PRODUCT': return 'Sản phẩm';
    case 'PROMOTION': return 'Khuyến mãi';
    case 'REGION': return 'Khu vực';
    case 'ROLE': return 'Vai trò';
    case 'SERVICE_TYPE': return 'Loại dịch vụ';
    case 'SETTLEMENT_BATCH': return 'Lô quyết toán';
    case 'SETTLEMENT_TRANSACTION': return 'Giao dịch quyết toán';
    case 'SHIPMENT': return 'Lô hàng';
    case 'SHIPMENT_ORDER': return 'Đơn hàng vận chuyển';
    case 'SHIPPER_ASSIGNMENT': return 'Phân công shipper';
    case 'SHIPPING_RATE': return 'Cước phí vận chuyển';
    case 'SHIPPING_REQUEST': return 'Yêu cầu vận chuyển';
    case 'SHIPPING_REQUEST_ATTACHMENT': return 'Tệp đính kèm vận chuyển';
    case 'SHOP_WORK_HISTORY': return 'Lịch sử làm việc của shop';
    case 'SUPPORT_MESSAGE': return 'Tin nhắn hỗ trợ';
    case 'SUPPORT_TICKET': return 'Phiếu hỗ trợ';
    case 'SYSTEM_CONFIG': return 'Cấu hình hệ thống';
    case 'USER': return 'Người dùng';
    case 'USER_PROMOTION': return 'Khuyến mãi người dùng';
    case 'USER_SETTLEMENT_SCHEDULE': return 'Lịch quyết toán người dùng';
    case 'VEHICLE': return 'Phương tiện';
    case 'VEHICLE_TRACKING': return 'Theo dõi phương tiện';
    default: return value;
  }
};