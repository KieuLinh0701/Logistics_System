import type { Office, OfficeSearchRequest, AdminOffice, CreateOfficePayload, UpdateOfficePayload } from "../types/office";
import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";

const officeApi = {
  searchOffice: async (params?: OfficeSearchRequest) => {
    const res = await axiosClient.get<ApiResponse<Office[]>>('/public/offices/search', { params });
    return res;
  },

  getHeadOffice: async () => {
    const res = await axiosClient.get<ApiResponse<Office>>('/public/offices/head-office');
    return res;
  },

  // Admin
  async listAdminOffices(params: { page?: number; limit?: number; search?: string }) {
    const res = await axiosClient.get<ApiResponse<ListResponse<AdminOffice>>>("/admin/offices", { params });
    return res;
  },

  async getAdminOfficeById(id: number) {
    const res = await axiosClient.get<ApiResponse<AdminOffice>>(`/admin/offices/${id}`);
    return res;
  },

  async createAdminOffice(data: CreateOfficePayload) {
    const res = await axiosClient.post<ApiResponse<AdminOffice>>("/admin/offices", data);
    return res;
  },

  async updateAdminOffice(id: number, data: UpdateOfficePayload) {
    const res = await axiosClient.put<ApiResponse<AdminOffice>>(`/admin/offices/${id}`, data);
    return res;
  },

  async deleteAdminOffice(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/offices/${id}`);
    return res;
  },
};

export default officeApi;