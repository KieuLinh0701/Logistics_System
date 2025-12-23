import type { FeeConfiguration, CreateFeeConfigurationPayload, UpdateFeeConfigurationPayload } from "../types/feeConfiguration";
import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";

const feeConfigurationApi = {
  // ---------------- Admin ---------------- //
  async listAdminFeeConfigurations(params: { page?: number; limit?: number; search?: string; feeType?: string; serviceTypeId?: number; active?: boolean }) {
    const res = await axiosClient.get<ApiResponse<ListResponse<FeeConfiguration>>>("/admin/fee-configurations", { params });
    return res;
  },

  async getAdminFeeConfigurationById(id: number) {
    const res = await axiosClient.get<ApiResponse<FeeConfiguration>>(`/admin/fee-configurations/${id}`);
    return res;
  },

  async createAdminFeeConfiguration(data: CreateFeeConfigurationPayload) {
    const res = await axiosClient.post<ApiResponse<FeeConfiguration>>("/admin/fee-configurations", data);
    return res;
  },

  async updateAdminFeeConfiguration(id: number, data: UpdateFeeConfigurationPayload) {
    const res = await axiosClient.put<ApiResponse<FeeConfiguration>>(`/admin/fee-configurations/${id}`, data);
    return res;
  },

  async deleteAdminFeeConfiguration(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/fee-configurations/${id}`);
    return res;
  },
};

export default feeConfigurationApi;


