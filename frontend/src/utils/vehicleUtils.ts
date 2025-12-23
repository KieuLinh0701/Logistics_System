export const VEHICLE_TYPES = ['TRUCK', 'VAN', 'CONTAINER'] as const;

export const VEHICLE_STATUSES = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'ARCHIVED'] as const;

export const VEHICLE_FILTER_SORT = [
  'NEWEST',
  'OLDEST',
  'CAPACITY_HIGH',
  'CAPACITY_LOW',
] as const;

export const translateVehicleType = (value: string): string => {
  switch (value) {
    case 'TRUCK': return 'Xe tải';
    case 'VAN': return 'Xe van';
    case 'CONTAINER': return 'Xe container';
    default: return value;
  }
};

export const translateVehicleStatus = (value: string): string => {
  switch (value) {
    case 'AVAILABLE': return 'Sẵn sàng';
    case 'IN_USE': return 'Đang sử dụng';
    case 'MAINTENANCE': return 'Đang bảo trì';
    case 'ARCHIVED': return 'Ngừng hoạt động';
    default: return value;
  }
};

export const translateVehicleFilterSort = (value: string): string => {
  switch (value) {
    case 'NEWEST': return 'Mới nhất';
    case 'OLDEST': return 'Cũ nhất';
    case 'CAPACITY_HIGH': return 'Tải trọng cao nhất';
    case 'CAPACITY_LOW': return 'Tải trọng thấp nhất';
    default: return value;
  }
};