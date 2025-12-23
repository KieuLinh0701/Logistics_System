// User
export interface UserOrderStats {
  total: number;              // Tổng số đơn đã tạo
  draft: number;              // Đơn nháp
  pending: number;            // Đơn chờ xử lý
  confirmed: number;          // Đơn đã xác nhận
  readyForPickup: number;     // Đơn đã sẵn sàng
  pickingUp: number;          // Đơn chuẩn bị lấy
  shipping: number;           // Đơn đang vận chuyển (picked_up, at_origin_office, in_transit, at_dest_office)
  delivering: number;         // Đơn đang giao
  delivered: number;          // Đơn giao thành công  
  failedDelivery: number;     // Đơn giao thất bại
  returning: number;          // Đơn đang hoàn
  returnedCancelled: number;  // Đơn đã hoàn/hủy
}

export interface UserProductStats {
  total: number;
  outOfStock: number;
  lowStock: number;
  active: number;
}

export interface UserRevenueStats {
  received: number;           // Tiền đã nhận (cod thu về)
  nextSettlement: number;     // Tiền chuẩn bị nhận trong phiên tới
  pendingDebt: number;        // Tiền còn nợ (nợ sau trừ cod chưa thanh toán) --> này chưa đúng lắm
  nextSettlementDate: string; // Thời gian đối soát sắp tới
}

export interface UserProductCounts {
    [key: string]: number;
}

export interface UserTopProductItem {
  id: number;
  name: string;
  total: number;
}

export interface UserOrderTimeLineItem {
  date: string;
  createdCount: number;
  deliveredCount: number;
}

export interface UserDashboardOverviewResponse {
  orders: UserOrderStats;
  products: UserProductStats;
  revenue: UserRevenueStats;
  productCounts: UserProductCounts;
}

export interface UserDashboardChartResponse {
  topSelling: UserTopProductItem[];
  topReturned: UserTopProductItem[];
  orderTimelines: UserOrderTimeLineItem[];
}

// Manager
export interface ManagerOrderStats {
  total: number;
  pending: number;           // PENDING
  confirmed: number;         // CONFIRMED
  readyForPickup: number;    // READY_FOR_PICKUP
  pickingOrPicked: number;   // PICKING_UP / PICKED_UP
  inWarehouse: number;       // AT_ORIGIN_OFFICE / AT_DEST_OFFICE
  customerAtOffice: number;  // CONFIRMED + pickupType = AT_OFFICE
  delivering: number;        // DELIVERING
  delivered: number;         // DELIVERED
  returned: number;          // RETURNED
  returning: number;         // RETURNING
  failedDelivery: number;    // FAILED_DELIVERY
}
export interface ManagerEmployeeStats {
  total: number;
  active: number;
  inactive: number;
  leave: number;
}
export interface ManagerIncidentStats {
  total: number;
  pendingLow: number;
  pendingMedium: number;
  pendingHight: number;
  pending: number;
  processing: number;
  resolved: number;
  rejected: number;
}
export interface ManagerShipmentStats {
  total: number;
  pending: number;
  inTransit: number;
  completed: number;
  cancelled: number;
}
export interface ManagerShippingRequestStats {
  total: number;
  pending: number;
  processing: number;
  resolved: number;
  rejected: number;
  cancelled: number;
}
export interface ManagerPaymentSubmissionBatchStats {
  total: number;
  pending: number;
  checking: number;
  completed: number;
  partial: number;
  cancelled: number;
}
export interface ManagerVehicleStats {
  total: number;
  available: number;
  inUse: number;
  maintenance: number;
  archived: number;
}
export interface ManagerDashboardOverviewResponse {
  employees: ManagerEmployeeStats;
  incidents: ManagerIncidentStats;
  orders: ManagerOrderStats;
  payments: ManagerPaymentSubmissionBatchStats;
  shipments: ManagerShipmentStats;
  shippingRequests: ManagerShippingRequestStats;
  vehicles: ManagerVehicleStats;
  vehicleCounts?: Record<string, number>; 
  employeeShiftCounts?: Record<string, number>; 
}