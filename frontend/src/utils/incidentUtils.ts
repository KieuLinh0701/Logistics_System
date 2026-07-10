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
    case 'REJECTED': return 'Từ chối';
    default: return value;
  }
};

export const INCIDENT_TYPES = [
  'DAMAGED_PARCEL',
  'LOST_PARCEL',
  'COD_DISPUTE',
  'CUSTOMER_CONFLICT',
  'SAFETY_INCIDENT',
  'VEHICLE_BREAKDOWN',
  'TRAFFIC_ACCIDENT',
  'SYSTEM_ERROR',
  'BARCODE_SCAN_ERROR',
  'WRONG_ORDER_ASSIGNMENT',
  'OFFICE_OPERATION_ISSUE',
  'DELIVERY_EXCEPTION',
  'PICKUP_EXCEPTION',
  'RETURN_EXCEPTION',
  'OTHER'
] as const;
export const translateIncidentType = (value: string): string => {
  switch (value) {
    case 'DAMAGED_PARCEL': return 'Hàng hóa bị hư hỏng';
    case 'LOST_PARCEL': return 'Hàng hóa bị thất lạc';
    case 'COD_DISPUTE': return 'Tranh chấp COD';
    case 'CUSTOMER_CONFLICT': return 'Tranh chấp với khách hàng';
    case 'SAFETY_INCIDENT': return 'Sự cố an toàn';
    case 'VEHICLE_BREAKDOWN': return 'Phương tiện hư hỏng';
    case 'TRAFFIC_ACCIDENT': return 'Tai nạn giao thông';
    case 'SYSTEM_ERROR': return 'Lỗi hệ thống';
    case 'BARCODE_SCAN_ERROR': return 'Lỗi quét mã vận đơn';
    case 'WRONG_ORDER_ASSIGNMENT': return 'Phân công sai đơn hàng';
    case 'OFFICE_OPERATION_ISSUE': return 'Sự cố tại bưu cục';
    case 'DELIVERY_EXCEPTION': return 'Sự cố giao hàng bất thường';
    case 'PICKUP_EXCEPTION': return 'Sự cố lấy hàng bất thường';
    case 'RETURN_EXCEPTION': return 'Sự cố hoàn hàng';
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