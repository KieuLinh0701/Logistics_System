export const AUDIT_LOG_STATUS = ['SUCCESS', 'FAILED', 'FORBIDDEN'] as const;
export const translateAuditLogStatus = (value: string): string => {
  switch (value) {
    case 'SUCCESS': return 'Thành công';
    case 'FAILED': return 'Thất bại';
    case 'FORBIDDEN': return 'Bị từ chối';
    default: return value;
  }
};

export const AUDIT_LOG_ACTION = [
  'CREATE', 'UPDATE', 'DELETE', 'EXPORT', 'IMPORT', 'PAY',
  'APPROVE', 'REJECT', 'CANCEL', 'LOGIN', 'CONFIRM', 'PROCESS',
  'UPDATE_STATUS', 'PRINT', 'REGISTER', 'PASSWORD_RESET'
] as const;
export const translateAuditLogAction = (value: string): string => {
  switch (value) {
    case 'CREATE': return 'Tạo mới';
    case 'UPDATE': return 'Cập nhật';
    case 'DELETE': return 'Xóa';
    case 'EXPORT': return 'Xuất dữ liệu';
    case 'IMPORT': return 'Nhập dữ liệu';
    case 'PAY': return 'Thanh toán';
    case 'APPROVE': return 'Phê duyệt';
    case 'REJECT': return 'Từ chối';
    case 'CANCEL': return 'Hủy';
    case 'LOGIN': return 'Đăng nhập';
    case 'CONFIRM': return 'Xác nhận';
    case 'PROCESS': return 'Xử lý';
    case 'UPDATE_STATUS': return 'Cập nhật trạng thái';
    case 'PRINT': return 'In';
    case 'REGISTER': return 'Đăng ký';
    case 'PASSWORD_RESET': return 'Đặt lại mật khẩu';
    default: return value;
  }
};

export const AUDIT_LOG_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
] as const;
export const translateAuditLogFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    default: return value;
  }
};