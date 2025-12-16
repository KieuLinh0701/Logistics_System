import type { ManagerEmployee } from "./employee";
import type { Office } from "./office";
import type { Vehicle } from "./vehicle";

export interface ManagerEmployeePerformanceShipment {
  id: number;
  code: string;
  vehicle: {
    licensePlate: string;
    capacity: number;
  }
  status: string;
  startTime: Date;
  endTime: Date;
  orderCount: number;
  totalWeight: number;
}

export interface ManagerShipment {
  id: number;
  code: string;

  vehicle: Vehicle;

  employee: ManagerEmployee;

  createdBy: ManagerEmployee;

  fromOffice: Office;

  toOffice: Office;

  status: string;
  type: string;

  startTime: string; 
  endTime: string; 
  createdAt: string;
  updatedAt: string;

  orders: ManagerOrderShipment[];
}

export interface ManagerShipmentSearchRequest {
  page: number;
  limit: number;
  search?: string;
  sort?: string;
  status?: string;
  type?: string;
  startDate?: string;
  endDate?: string;
}

export interface ManagerOrderShipment {
  id: number;
  trackingNumber: string;
  status: string;
  weight: number;
  cod: number;
  totalFee: number;
  paymentStatus: string;
  payer: string;
  recipient: {
    id: number;
    name: string;
    phone: string;
    cityCode: number;
    wardCode: number;
    detail: string;
  }
  toOffice: {
    id: number;
    name: string;
    postalCode: string;
    cityCode: number;
    wardCode: number;
    detail: string;
    latitude: number;
    longitude: number;
  };
}

export interface ManagerOrderShipmentSearchRequest {
  page: number;
  limit: number;
  search?: string; 
}

// DRIVER
export interface DriverShipment {
  id: number;
  code: string;
  status: string;
  startTime?: string;
  endTime?: string;
  createdAt?: string;
  vehicle?: {
    id: number;
    licensePlate: string;
    type: string;
  };
  fromOffice?: {
    id: number;
    name: string;
  };
  toOffice?: {
    id: number;
    name: string;
  };
  orders?: Array<{
    id: number;
    trackingNumber: string;
    toOffice?: {
      id: number;
      name: string;
    };
  }>;
  orderCount?: number;
}

export interface DriverRouteInfo {
  id: number;
  code?: string;
  name: string;
  status: string;
  totalStops: number;
  totalOrders: number;
  startTime?: string;
  fromOffice?: {
    id: number;
    name: string;
  };
}

export interface DriverDeliveryStop {
  id: number;
  officeName: string;
  officeAddress?: string;
  orderCount: number;
  orders: Array<{
    id: number;
    trackingNumber: string;
  }>;
  status: string;
}
