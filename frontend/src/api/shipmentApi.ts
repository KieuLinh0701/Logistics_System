import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerOrderShipment, ManagerOrderShipmentSearchRequest, ManagerShipment, ManagerShipmentSearchRequest } from "../types/shipment";

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

  // async createManagerShipment(data: Partial<ManagerEmployee>) {
  //   const res = await axiosClient.post<ApiResponse<Boolean>>("/manager/shipments", data);
  //   return res;
  // },

  // async updateManagerEmployee(id: number, data: Partial<ManagerEmployee>) {
  //   const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/employees/${id}`, data);
  //   return res;
  // },

  // async deleteUserProduct(productId: number) {
  //   const res = await axiosClient.delete<ApiResponse<string>>(`/user/products/${productId}`);
  //   return res;
  // },

  // async createBulkUserProduct(data: FormData) {
  //   const res = await axiosClient.post<BulkResponse<Product>>("/user/products/bulk", data);
  //   return res;
  // },

  // async getActiveAndInstockUserProducts(params: UserProductActiveAndInstockRequest) {
  //   const res = await axiosClient.get<ApiResponse<ListResponse<Product>>>("/user/products/active", { params });
  //   return res;
  // },
};

export default shipmentApi;