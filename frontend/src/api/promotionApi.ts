import type { Promotion, CreatePromotionPayload, UpdatePromotionPayload, PromotionUserRequest, PromotionPublicRequest} from "../types/promotion";
import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";

const promotionApi = {
  // Public
  getActivePromotions: async (params?: PromotionPublicRequest) => {
    const res = await axiosClient.get<ApiResponse<ListResponse<Promotion>>>('/public/promotions/active', { params });
    return res;
  },

  // User
  getActiveUserPromotions: async (params?: PromotionUserRequest) => {
    const res = await axiosClient.get<ApiResponse<ListResponse<Promotion>>>('/user/promotions/active', { params });
    return res;
  },

  // Admin
  async listAdminPromotions(params: { page?: number; limit?: number; search?: string; status?: string; isGlobal?: boolean }) {
    const res = await axiosClient.get<ApiResponse<{ data: Promotion[]; pagination: any }>>("/admin/promotions", { params });
    return res;
  },

  async getAdminPromotionById(id: number) {
    const res = await axiosClient.get<ApiResponse<Promotion>>(`/admin/promotions/${id}`);
    return res;
  },

  async createAdminPromotion(data: CreatePromotionPayload) {
    const res = await axiosClient.post<ApiResponse<Promotion>>("/admin/promotions", data);
    return res;
  },

  async updateAdminPromotion(id: number, data: UpdatePromotionPayload) {
    const res = await axiosClient.put<ApiResponse<Promotion>>(`/admin/promotions/${id}`, data);
    return res;
  },

  async deleteAdminPromotion(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/promotions/${id}`);
    return res;
  },
};

export default promotionApi;