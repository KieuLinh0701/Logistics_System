import type { ApiResponse, ListResponse } from "../types/response";
import type { AdminOrder, CreateOrderSuccess, Order, OrderPrint, UserOrderRequest, UserOrderSearchRequest } from "../types/order";
import axiosClient from "./axiosClient";

const orderApi = {
  // Admin
  async listAdminOrders(params: { page?: number; limit?: number; search?: string; status?: string }) {
    const res = await axiosClient.get<ApiResponse<ListResponse<AdminOrder>>>("/admin/orders", { params });
    return res;
  },

  async updateAdminOrderStatus(id: number, status: string) {
    const res = await axiosClient.put<ApiResponse<AdminOrder>>(`/admin/orders/${id}/status`, { status });
    return res;
  },

  async deleteAdminOrder(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/orders/${id}`);
    return res;
  },

  // User
  async listUserOrders(params: UserOrderSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Order>>>("/user/orders", { params });
    return res;
  },

  async createUserOrder(params: UserOrderRequest) {
    const res = await axiosClient.post<ApiResponse<CreateOrderSuccess>>("/user/orders", params);
    return res;
  },

  async getUserOrderByTrackingNumber(trackingNumber: string) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/${trackingNumber}`);
    return res;
  },

  async getUserOrderById(id: number) {
    const res = await axiosClient.get<ApiResponse<Order>>(`/user/orders/id/${id}`);
    return res;
  },

  async publicUserOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<string>>(`/user/orders/${id}/public`);
    return res;
  },

  async cancelUserOrder(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/user/orders/${id}/cancel`);
    return res;
  },

  async deleteUserOrder(id: number) {
    const res = await axiosClient.delete<ApiResponse<Boolean>>(`/user/orders/${id}`);
    return res;
  },

  async printUserOrders(orderIds: number[]) {
    const query = orderIds.join(",");
    const res = await axiosClient.get<ApiResponse<OrderPrint[]>>(`/user/orders/print?orderIds=${query}`);
    return res;
  },
};

export default orderApi;