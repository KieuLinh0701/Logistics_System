import type { Promotion, PromotionUserRequest, PromotionPublicRequest } from "../types/promotion";
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

  // ---------------- Admin ---------------- //

};

export default promotionApi;