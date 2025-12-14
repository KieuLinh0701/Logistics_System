import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

export interface Shipment {
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

export interface RouteInfo {
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

export interface DeliveryStop {
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

const shipmentApi = {
  // DRIVER
  async startShipment(shipmentId: number) {
    await axiosClient.post<ApiResponse<string>>(`/driver/shipments/${shipmentId}/start`);
  },

  async finishShipment(payload: { shipmentId: number; status: "COMPLETED" | "CANCELLED" }) {
    await axiosClient.post<ApiResponse<string>>("/driver/shipments/finish", payload);
  },

  async getDriverShipments(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/shipments", { params });
    const data = res.data || {};
    return {
      shipments: (data.shipments || []) as Shipment[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async getDriverRoute() {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/shipments/route");
    const data = res.data || {};
    return {
      routeInfo: (data.routeInfo || null) as RouteInfo | null,
      deliveryStops: (data.deliveryStops || []) as DeliveryStop[],
    };
  },

  async getDriverHistory(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/shipments/history", { params });
    const data = res.data || {};
    return {
      shipments: (data.shipments || []) as Shipment[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async updateVehicleTracking(payload: {
    shipmentId: number;
    latitude: number;
    longitude: number;
    speed: number;
  }) {
    await axiosClient.post<ApiResponse<string>>("/driver/shipments/tracking", payload);
  },

  async getVehicleTracking(shipmentId: number) {
    const res = await axiosClient.get<ApiResponse<any>>(`/driver/shipments/${shipmentId}/tracking`);
    const data = res.data || {};
    return {
      trackings: (data.trackings || []) as Array<{
        id: number;
        latitude: number;
        longitude: number;
        speed: number;
        recordedAt: string;
      }>,
    };
  },
};

export default shipmentApi;




