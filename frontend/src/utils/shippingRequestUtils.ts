// Điều kiện để thao tác với Shipping Request của user
export const canEditUserShippingRequest = (value: string) => {
  return ['PENDING'].includes(value);
};

export const canCancelUserShippingRequest = (value: string) => {
  return ['PENDING'].includes(value);
};

export const canEmptyTrackingNumberShippingRequest = (value: string) => {
  return ['INQUIRY', 'COMPLAINT'].includes(value);
};

export const canEmptyRequestContentShippingRequest = (value: string) => {
  return ['DELIVERY_REMINDER', 'PICKUP_REMINDER'].includes(value);
};

// Điều kiện để thao tác với Shipping Request của manager
export const canProcessingManagerShippingRequest = (value: string) => {
  return ['PENDING', 'PROCESSING'].includes(value);
};

export const SHIPPING_REQUEST_MESSAGES: Record<string, string> = {
  INQUIRY: "Yêu cầu hỗ trợ - Không bắt buộc mã đơn hàng",
  COMPLAINT: "Yêu cầu khiếu nại - Không bắt buộc mã đơn hàng",
  DELIVERY_REMINDER: "Hối giao hàng - Nội dung không bắt buộc",
  CHANGE_ORDER_INFO: "Thay đổi thông tin đơn hàng - Bắt buộc nhập mã đơn hàng và nội dung",
  PICKUP_REMINDER: "Hối lấy hàng - Nội dung không bắt buộc",
};

export const getShippingRequestMessage = (type?: string): string | undefined => {
  if (!type) return undefined;
  return SHIPPING_REQUEST_MESSAGES[type];
};

// Manager: các trạng thái được phép chuyển tiếp tùy theo status hiện tại
export const getAllowedManagerStatuses = (currentStatus?: string): string[] => {
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

export const SHIPPING_REQUEST_TYPES = ['COMPLAINT', 'PICKUP_REMINDER', 'DELIVERY_REMINDER', 'CHANGE_ORDER_INFO', 'INQUIRY'] as const;

export const SHIPPING_REQUEST_STATUS = ['PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED', 'CANCELLED'] as const;

export const SHIPPING_REQUEST_FILTER_SORT = [
  'NEWEST',
  'OLDEST'
] as const;

// Public 
export const SHIPPING_REQUEST_TYPES_PUBLIC = ['COMPLAINT', 'INQUIRY'] as const;

export const translateShippingRequestType = (value: string): string => {
  switch (value) {
    case 'COMPLAINT': return 'Khiếu nại';
    case 'PICKUP_REMINDER': return 'Hối lấy hàng';
    case 'DELIVERY_REMINDER': return 'Hối giao hàng';
    case 'CHANGE_ORDER_INFO': return 'Thay đổi thông tin ĐH';
    case 'INQUIRY': return 'Yêu cầu hỗ trợ';
    default: return value;
  }
};

export const translateShippingRequestStatus = (value: string): string => {
  switch (value) {
    case 'PENDING': return 'Chờ xử lý';
    case 'PROCESSING': return 'Đang xử lý';
    case 'RESOLVED': return 'Đã xử lý';
    case 'REJECTED': return 'Từ chối';
    case 'CANCELLED': return 'Đã hủy';
    default: return value;
  }
};

export const translateShippingRequestFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';
    default: return value;
  }
};