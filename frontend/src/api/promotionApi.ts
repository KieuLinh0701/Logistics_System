import type { PromotionResponse, PublicPromotionRequest } from "../types/promotion";
import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

const promotionApi = {
  // ---------------- Public ---------------- //
  getActivePromotions: async (params?: PublicPromotionRequest) => {
    const res = await axiosClient.get<ApiResponse<PromotionResponse>>('/public/promotions/active', { params });
    return res;
  },

  // ---------------- Admin ---------------- //

};

export default promotionApi;