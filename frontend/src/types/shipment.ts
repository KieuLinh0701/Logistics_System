import type {ManagerEmployee} from "./employee";
import type {Office} from "./office";
import type {Vehicle} from "./vehicle";
import type {ListResponse} from "./response.ts";

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
  orderCount?: number;
  totalWeight?: number;
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
  direction: string;
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
  recipientName: string;
  recipientPhone: string;
  recipientFullAddress: string;
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
  pendingDestinationConfirm: boolean;
}

export interface ManagerOrderShipmentSearchRequest {
  page?: number;
  limit?: number;
  search?: string; 
}

export interface ManagerShipmentAddEditRequest {
  type: string;
  vehicleId?: number;
  toOfficeId?: number;
  employeeId?: number;
}

// DRIVER
export interface DriverShipment {
  id: number;
  code: string;
  status: string;
  employee?: { id: number; userId?: number; };
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
  toOffice?: {
    id: number;
    name: string;
    address?: string;
    latitude?: number | null;
    longitude?: number | null;
  };
}

export interface DriverDeliveryStop {
  id: number;
  officeName: string;
  officeAddress?: string;
  office?: {
    id: number;
    name: string;
    address?: string;
    latitude?: number | null;
    longitude?: number | null;
  };
  orderCount: number;
  orders: Array<{
    id: number;
    trackingNumber: string;
  }>;
  status: string;
}
export interface GetOrdersByShipmentIdManagerResponse {
  orders: ListResponse<ManagerOrderShipment>;
  status: string;
  type: string;
}