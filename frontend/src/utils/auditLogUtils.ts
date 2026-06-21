export const AUDIT_LOG_STATUS = ['LOW', 'MEDIUM', 'HIGH'] as const;
export const translateAuditLogStatus = (value: string): string => {
  switch (value) {
    case 'LOW': return 'Thấp';
    case 'MEDIUM': return 'Trung bình';
    case 'HIGH': return 'Cao';
    default: return value;
  }
};

export const AUDIT_LOG_ENTITY = ['PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED'] as const;
export const translateAuditLogEntity = (value: string): string => {
  switch (value) {
    case 'PENDING': return 'Chờ xử lý';
    case 'PROCESSING': return 'Đang xử lý';
    case 'RESOLVED': return 'Đã giải quyết';
    case 'REJECTED': return 'Từ chối';
    default: return value;
  }
};

export const AUDIT_LOG_ACTION = ['RECIPIENT_NOT_AVAILABLE', 'WRONG_ADDRESS', 'PACKAGE_DAMAGED', 'RECIPIENT_REFUSED', 'SECURITY_ISSUE', 'OTHER'] as const;
export const translateAuditLogAction = (value: string): string => {
  switch (value) {
    case 'RECIPIENT_NOT_AVAILABLE': return 'Người nhận không có mặt';
    case 'WRONG_ADDRESS': return 'Sai địa chỉ';
    case 'PACKAGE_DAMAGED': return 'Hàng bị hỏng';
    case 'RECIPIENT_REFUSED': return 'Người nhận từ chối';
    case 'SECURITY_ISSUE': return 'Vấn đề an ninh';
    case 'OTHER': return 'Khác';
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