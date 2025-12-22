import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerOrderShipment, ManagerOrderShipmentSearchRequest, ManagerShipment, ManagerShipmentAddEditRequest, ManagerShipmentSearchRequest } from "../types/shipment";
import type { SearchRequest } from "../types/request";

const shipmentApi = {
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

  async listManagerPendingShipments(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipment>>>("/manager/shipments/pending", { params });
    return res;
  },

  async createManagerShipment(data: ManagerShipmentAddEditRequest) {
    const res = await axiosClient.post<ApiResponse<Boolean>>("/manager/shipments", data);
    return res;
  },

  async updateManagerShipment(id: number, data: ManagerShipmentAddEditRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/shipments/${id}`, data);
    return res;
  },
};

export default shipmentApi;