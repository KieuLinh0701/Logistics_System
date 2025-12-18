export const canEditManagerIncident = (value: string) => {
  return ["PENDING",
    "PROCESSING"].includes(value)
};

export const INCIDENT_PRIORITYS = ['LOW', 'MEDIUM', 'HIGH'] as const;
export const translateIncidentPriority = (value: string): string => {
  switch (value) {
    case 'LOW': return 'Thấp';
    case 'MEDIUM': return 'Trung bình';
    case 'HIGH': return 'Cao';
    default: return value;
  }
};

export const INCIDENT_STATUSES = ['PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED'] as const;
export const translateIncidentStatus = (value: string): string => {
  switch (value) {
    case 'PENDING': return 'Chờ xử lý';
    case 'PROCESSING': return 'Đang xử lý';
    case 'RESOLVED': return 'Đã giải quyết';
    case 'REJECTED': return 'Bị từ chối';
    default: return value;
  }
};

export const INCIDENT_TYPES = ['RECIPIENT_NOT_AVAILABLE', 'WRONG_ADDRESS', 'PACKAGE_DAMAGED', 'RECIPIENT_REFUSED', 'SECURITY_ISSUE', 'OTHER'] as const;
export const translateIncidentType = (value: string): string => {
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

export const INCIDENT_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
] as const;
export const translateIncidentFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    default: return value;
  }
};

// Manager: các trạng thái được phép chuyển tiếp tùy theo status hiện tại
export const getAllowedManagerIncidentReportStatuses = (currentStatus?: string): string[] => {
  if (!currentStatus) return [];

  switch (currentStatus) {
    case 'PENDING':
      return ['PROCESSING', 'RESOLVED', 'REJECTED'];

    case 'PROCESSING':
      return ['RESOLVED', 'REJECTED'];

    case 'RESOLVED':
    case 'REJECTED':
    case 'CANCELLED':
      return [];

    default:
      return [];
  }
};