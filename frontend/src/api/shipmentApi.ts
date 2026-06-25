import type {ApiResponse, ListResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {
  DriverDeliveryStop,
  DriverRouteInfo,
  DriverShipment,
  GetOrdersByShipmentIdManagerResponse,
  ManagerOrderShipmentSearchRequest,
  ManagerShipment,
  ManagerShipmentAddEditRequest,
  ManagerShipmentSearchRequest,
  ShipperOrderShipmentSearchRequest,
  ShipperShipmentSearchRequest
} from "../types/shipment";
import type {SearchRequest} from "../types/request";
import {axiosExport} from "./exportClient";
import type {Order} from "../types/order.ts";

const shipmentApi = {
  // DRIVER
  async startShipment(shipmentId: number) {
    const res = await axiosClient.post<ApiResponse<string>>(`/driver/shipments/${shipmentId}/start`);
    return res;
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
      debug: data.debug || null,
    };
  },

  async getDriverContext() {
    const res = await axiosClient.get<ApiResponse<any>>("/driver/context");
    const data = res.data || {};
    return {
      office: data.office || null,
      vehicles: data.vehicles || [],
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
    const res = await axiosClient.get<ApiResponse<GetOrdersByShipmentIdManagerResponse>>(`/manager/shipments/${id}`, { params });
    return res;
  },

  async getManagerAllOrderIdsByShipmentId(id: number, params: ManagerOrderShipmentSearchRequest) {
    const res = await axiosClient.get<ApiResponse<number[]>>(`/manager/shipments/${id}/all-ids`, { params });
    return res;
  },

  async cancelManagerShipment(id: number) {
    const res = await axiosClient.patch<ApiResponse<void>>(`/manager/shipments/${id}/cancel`);
    return res;
  },

  async listManagerPendingShipments(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipment>>>("/manager/shipments/pending", { params });
    return res;
  },

  async createManagerShipment(data: ManagerShipmentAddEditRequest) {
    return await axiosClient.post<ApiResponse<boolean>>("/manager/shipments", data);
  },

  async updateManagerShipment(id: number, data: ManagerShipmentAddEditRequest) {
    return await axiosClient.put<ApiResponse<void>>(`/manager/shipments/${id}`, data);
  },

  async startManagerShipment(id: number) {
    return await axiosClient.post<ApiResponse<string>>(`/manager/shipments/${id}/start`);
  },

  async finishManagerShipment(id: number) {
    return await axiosClient.post<ApiResponse<string>>(`/manager/shipments/${id}/finish`);
  },

  async startShipperDeliveryShipment(id: number) {
    return await axiosClient.post<ApiResponse<string>>(`/shipper/shipments/${id}/start`);
  },

  async finishShipperDeliveryShipment(id: number) {
    return await axiosClient.post<ApiResponse<string>>(`/shipper/shipments/${id}/finish`);
  },

  async exportManagerShipmentPerformance(id: number, params: SearchRequest) {
    try {
      const res = await axiosExport.get(`/manager/employees/${id}/shipments/export`, {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },

  async exportManagerShipments(params: ManagerShipmentSearchRequest) {
    try {
      const res = await axiosExport.get("/manager/shipments/export", {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },

  async listShipperActiveShipments() {
    return axiosClient.get("/shipper/shipments/active");
  },

  async exportManagerOrdersByShipmentId(id: number, params: ManagerOrderShipmentSearchRequest) {
    try {
      const res = await axiosExport.get(`/manager/shipments/${id}/export`, {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },


  // Shipper
  async listShipperShipments(params: ShipperShipmentSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipment>>>("/shipper/shipments/history", { params });
    return res;
  },

  async exportShipperShipments(params: ShipperShipmentSearchRequest) {
    try {
      const res = await axiosExport.get("/shipper/shipments/history/export", {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },

  async getShipperOrdersByShipmentId(id: number, params: ShipperOrderShipmentSearchRequest) {
    return await axiosClient.get<ApiResponse<ListResponse<Order>>>(`/shipper/shipments/history/${id}`, { params });
  },

  async exportShipperOrdersByShipmentId(id: number, params: ShipperOrderShipmentSearchRequest) {
    try {
      const res = await axiosExport.get(`/shipper/shipments/history/${id}/export`, {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },
};

export default shipmentApi;
