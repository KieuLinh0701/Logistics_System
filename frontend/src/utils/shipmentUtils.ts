export const SHIPMENT_STATUSES = ['ACTIVE', 'INACTIVE', 'LEAVE'] as const;

export const SHIPMENT_FILTER_SORT = [
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

export const translateShipmentStatus = (value: string): string => {
  switch (value) {
    case 'ACTIVE': return 'Đang làm việc';
    case 'INACTIVE': return 'Ngưng hoạt động';
    case 'LEAVE': return 'Đã nghỉ việc';
    default: return value;
  }
};

export const translateShipmentFilterSort = (value: string): string => {
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