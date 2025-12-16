export const canEditUserSenderInfo = (value: string) => {
  return ["DRAFT", "PENDING", "CONFIRMED"].includes(value);
};

// Điều kiện để thao tác với order của user
export const canEditUserOrder = (value: string) => {
  return ["DRAFT", "PENDING", "CONFIRMED"].includes(value);
};

export const canPublicUserOrder = (value: string) => {
  return ["DRAFT"].includes(value);
};

export const canCancelUserOrder = (value: string) => {
  return ["PENDING", "CONFIRMED", "READY_FOR_PICKUP"].includes(value)
};

export const canPrintUserOrder = (value: string) => {
  return !["DRAFT", "PENDING", "CANCELLED"].includes(value)
};

export const canDeleteUserOrder = (value: string) => {
  return ["DRAFT"].includes(value)
};

export const canReadyUserOrder = (value: string) => {
  return ["CONFIRMED"].includes(value)
};

// Điều kiện để thao tác với order của manager
export const canPrintManagerOrder = (value: string) => {
  return !["DRAFT", "PENDING", "CANCELLED"].includes(value)
};

export const canCancelManagerOrder = (value: string) => {
  return ["PENDING",
    "CONFIRMED",
    "READY_FOR_PICKUP",
    'PICKING_UP',
    'PICKED_UP',
    'AT_ORIGIN_OFFICE'].includes(value)
};

export const canAtOriginOfficeManagerOrder = (value: string) => {
  return ["CONFIRMED"].includes(value)
};

// này chưa chỉnh nha
export const canEditManagerOrder = (value: string) => {
  return ["DRAFT", "PENDING", "CONFIRMED"].includes(value);
};

export const ORDER_COD_STATUS = ['NONE', 'EXPECTED', 'PENDING', 'SUBMITTED', 'RECEIVED', 'TRANSFERRED'] as const;

export const ORDER_CREATOR_TYPES = ['USER', 'MANAGER', 'ADMIN'] as const;

export const ORDER_PAYER_TYPES = ['CUSTOMER', 'SHOP'] as const;

export const ORDER_PAYMENT_STATUS = ['PAID', 'UNPAID', 'REFUNDED'] as const;

export const ORDER_PICKUP_TYPES = ['PICKUP_BY_COURIER', 'AT_OFFICE'] as const;

export const ORDER_STATUS = [
  'DRAFT',
  'PENDING',
  'CONFIRMED',
  'READY_FOR_PICKUP',
  'PICKING_UP',
  'PICKED_UP',
  'AT_ORIGIN_OFFICE',
  'IN_TRANSIT',
  'AT_DEST_OFFICE',
  'DELIVERING',
  'DELIVERED',
  'FAILED_DELIVERY',
  'CANCELLED',
  'RETURNED',
] as const;

export const ORDER_FILTER_COD = ['ALL', 'YES', 'NO'] as const;

export const ORDER_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
  'COD_HIGH',
  'COD_LOW',
  'ORDER_VALUE_HIGH',
  'ORDER_VALUE_LOW',
  'FEE_HIGH',
  'FEE_LOW',
  'WEIGHT_HIGH',
  'WEIGHT_LOW',
] as const;

export const translateOrderCreatorType = (value: string): string => {
  switch (value) {
    case 'USER': return 'Người dùng';
    case 'MANAGER': return 'Quản lý';
    case 'ADMIN': return 'Quản trị viên';
    default: return value;
  }
};

export const translateOrderPayerType = (value: string): string => {
  switch (value) {
    case 'CUSTOMER': return 'Người nhận';
    case 'SHOP': return 'Người gửi';
    default: return value;
  }
};

export const translateOrderPaymentStatus = (value: string): string => {
  switch (value) {
    case 'PAID': return 'Đã thanh toán';
    case 'UNPAID': return 'Chưa thanh toán';
    case 'REFUNDED': return 'Đã hoàn tiền';
    default: return value;
  }
};

export const translateOrderPickupType = (value: string): string => {
  switch (value) {
    case 'PICKUP_BY_COURIER': return 'Lấy hàng tại nhà';
    case 'AT_OFFICE': return 'Giao tại bưu cục';
    default: return value;
  }
};

export const translateOrderStatus = (value: string): string => {
  switch (value) {
    case 'DRAFT': return 'Bản nháp';
    case 'PENDING': return 'Chờ duyệt';
    case 'CONFIRMED': return 'Đã xác nhận';
    case 'READY_FOR_PICKUP': return 'Sẵn sàng để lấy';
    case 'PICKING_UP': return 'Đang lấy hàng';
    case 'PICKED_UP': return 'Đã lấy hàng';
    case 'AT_ORIGIN_OFFICE': return 'Tại bưu cục gốc';
    case 'IN_TRANSIT': return 'Đang vận chuyển';
    case 'AT_DEST_OFFICE': return 'Tại bưu cục đích';
    case 'DELIVERING': return 'Đang giao';
    case 'DELIVERED': return 'Đã giao hàng';
    case 'FAILED_DELIVERY': return 'Giao thất bại';
    case 'CANCELLED': return 'Đã hủy';
    case 'RETURNED': return 'Đã hoàn trả';
    default: return value;
  }
};

export const translateOrderFilterCod = (value: string): string => {
  switch (value) {
    case 'ALL': return 'Tất cả COD';
    case 'YES': return 'Có COD';
    case 'NO': return 'Không COD';
    default: return value;
  }
};

export const translateOrderFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';

    case 'COD_HIGH': return 'COD cao nhất';
    case 'COD_LOW': return 'COD thấp nhất';

    case 'ORDER_VALUE_HIGH': return 'Giá trị đơn cao nhất';
    case 'ORDER_VALUE_LOW': return 'Giá trị đơn thấp nhất';

    case 'FEE_HIGH': return 'Phí dịch vụ cao nhất';
    case 'FEE_LOW': return 'Phí dịch vụ thấp nhất';

    case 'WEIGHT_HIGH': return 'Khối lượng cao nhất';
    case 'WEIGHT_LOW': return 'Khối lượng thấp nhất';

    default: return value;
  }
};

export const translateOrderCodStatus = (value: string): string => {
  switch (value) {
    case 'NONE':
      return 'Không COD';

    case 'EXPECTED':
      return 'Chưa thu COD';

    case 'PENDING':
      return 'Shipper giữ COD';

    case 'SUBMITTED':
      return 'Đã nộp chờ đối soát';

    case 'RECEIVED':
      return 'Bưu cục đã nhận';

    case 'TRANSFERRED':
      return 'Đã chuyển shop';

    default:
      return value;
  }
};