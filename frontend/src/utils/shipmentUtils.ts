// Status có thể hủy Shipment của Manager
export const canCancelManagerShipment = (value: string) => {
  return ["PENDING"].includes(value)
};

// Status có thể edit Shipment của Manager
export const canEditManagerShipment = (value: string) => {
  return ["PENDING"].includes(value)
};

// Status có thể change orders in Shipment của Manager
export const canEditOrdersManagerShipment = (value: string, type: string) => {
  return (["PENDING", "IN_TRANSIT"].includes(value) && type === "DELIVERY") ||
      (["PENDING"].includes(value) && type === "TRANSFER");
};

export const canConfirmDestinationOrdersManagerShipment = (value: string) => {
  return ["COMPLETED"].includes(value);
};

export const canDeleteOrdersManagerShipment = (value: string) => {
  return ["PENDING"].includes(value)
};

export const SHIPMENT_STATUSES = ['PENDING', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'] as const;
export const translateShipmentStatus = (value: string): string => {
  switch (value) {
    case 'PENDING':
      return 'Chờ khởi hành';
    case 'IN_TRANSIT':
      return 'Đang vận chuyển';
    case 'COMPLETED':
      return 'Đã hoàn thành';
    case 'CANCELLED':
      return 'Đã hủy';
    default:
      return value;
  }
};

export const SHIPMENT_TYPES = ['DELIVERY', 'TRANSFER'] as const;
export const translateShipmentType = (value: string): string => {
  switch (value) {
    case 'DELIVERY':
      return 'Giao hàng';
    case 'TRANSFER':
      return 'Trung chuyển';
    default:
      return value;
  }
};

export const SHIPMENT_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
] as const;
export const translateShipmentFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';
    default:
      return value;
  }
};