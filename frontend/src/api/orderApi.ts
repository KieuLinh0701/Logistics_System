import type { ApiResponse, ListResponse } from "../types/response";
import type { AdminOrder } from "../types/order";
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
};

export default orderApi;


