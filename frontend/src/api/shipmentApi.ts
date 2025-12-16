import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerOrderShipment, ManagerOrderShipmentSearchRequest, ManagerShipment, ManagerShipmentSearchRequest } from "../types/shipment";
import type { DriverShipment, DriverRouteInfo, DriverDeliveryStop } from "../types/shipment";

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
      shipments: (data.shipments || []) as DriverShipment[],
      pagination: data.pagination || { page: 1, limit: 10, total: 0 },
    };
  },

  async getDriverRoute() {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/shipments/route");
    const data = res.data || {};
    return {
      routeInfo: (data.routeInfo || null) as DriverRouteInfo | null,
      deliveryStops: (data.deliveryStops || []) as DriverDeliveryStop[],
    };
  },

  async getDriverHistory(params: { page?: number; limit?: number }) {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/shipments/history", { params });
    const data = res.data || {};
    return {
      shipments: (data.shipments || []) as DriverShipment[],
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

    // Manager
  async listManagerShipments(params: ManagerShipmentSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipment>>>("/manager/shipments", { params });
    return res;
  },

  async getManagerOrdersByShipmentId(id: number, params: ManagerOrderShipmentSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerOrderShipment>>>(`/manager/shipments/${id}`, { params });
    return res;
  },

  async cancelManagerShipment(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/manager/shipments/${id}/cancel`);
    return res;
  },
};

export default shipmentApi;
