// Status có thể hủy Shipment của Manager
export const canCancelManagerShipment = (value: string) => {
  return ["PENDING"].includes(value)
};

// Status có thể edit Shipment của Manager
export const canEditManagerShipment = (value: string) => {
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

export const EMPLOYY_PERFOMANCE_SHIPMENT_FILTER_SORT = [
  'TOTAL_ORDERS_HIGH',
  'TOTAL_ORDERS_LOW',
  'TOTAL_SHIPMENTS_HIGH',
  'TOTAL_SHIPMENTS_LOW',
  'COMPLETED_ORDERS_HIGH',
  'COMPLETED_ORDERS_LOW',
  'COMPLETION_RATE_HIGH',
  'COMPLETION_RATE_LOW',
  'AVG_TIME_PER_ORDER_HIGH',
  'AVG_TIME_PER_ORDER_LOW',
] as const;
export const translateEmployeePerformanceShipmentFilterSort = (value: string): string => {
  switch (value) {
    case 'TOTAL_ORDERS_HIGH': return 'Số đơn nhiều nhất';
    case 'TOTAL_ORDERS_LOW': return 'Số đơn ít nhất';

    case 'TOTAL_SHIPMENTS_HIGH': return 'Số chuyến nhiều nhất';
    case 'TOTAL_SHIPMENTS_LOW': return 'Số chuyến ít nhất';

    case 'COMPLETED_ORDERS_HIGH': return 'Đơn hoàn thành nhiều nhất';
    case 'COMPLETED_ORDERS_LOW': return 'Đơn hoàn thành ít nhất';

    case 'COMPLETION_RATE_HIGH': return 'Tỉ lệ hoàn thành cao';
    case 'COMPLETION_RATE_LOW': return 'Tỉ lệ hoàn thành thấp';

    case 'AVG_TIME_PER_ORDER_HIGH': return 'Thời gian trung bình cao';
    case 'AVG_TIME_PER_ORDER_LOW': return 'Thời gian trung bình thấp';

    default:
      return value;
  }
};