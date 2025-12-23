import type { ApiResponse, ListResponse } from "../types/response";
import type { ServiceType, ServiceTypeWithShippingRatesResponse, AdminServiceType, CreateServiceTypePayload, UpdateServiceTypePayload } from "../types/serviceType";
import axiosClient from "./axiosClient";

const serviceTypeApi = {
  // ---------------- Public ---------------- //
  getActiveServiceTypes: async () => {
    const res = await axiosClient.get<ApiResponse<ServiceType[]>>('/public/service-types/active');
    return res;
  },
  getActiveServicesWithRates: async () => {
    const res = await axiosClient.get<ApiResponse<ServiceTypeWithShippingRatesResponse[]>>('/public/service-types/active-with-rates');
    return res;
  },
  
  // Admin
  async listAdminServiceTypes(params: { page?: number; limit?: number; search?: string }) {
    const res = await axiosClient.get<ApiResponse<{ data: AdminServiceType[]; pagination: any }>>('/admin/service-types', { params });
    return res;
  },

  async getAdminServiceTypeById(id: number) {
    const res = await axiosClient.get<ApiResponse<AdminServiceType>>(`/admin/service-types/${id}`);
    return res;
  },

  async createAdminServiceType(data: CreateServiceTypePayload) {
    const res = await axiosClient.post<ApiResponse<AdminServiceType>>('/admin/service-types', data);
    return res;
  },

  async updateAdminServiceType(id: number, data: UpdateServiceTypePayload) {
    const res = await axiosClient.put<ApiResponse<AdminServiceType>>(`/admin/service-types/${id}`, data);
    return res;
  },

  async deleteAdminServiceType(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/service-types/${id}`);
    return res;
  },
};

export default serviceTypeApi;