import type { Promotion, PublicPromotionRequest } from "../types/promotion";
import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";

const promotionApi = {
  // ---------------- Public ---------------- //
  getActivePromotions: async (params?: PublicPromotionRequest) => {
    const res = await axiosClient.get<ApiResponse<ListResponse<Promotion>>>('/public/promotions/active', { params });
    return res;
  },

  // ---------------- Admin ---------------- //

};

export default promotionApi;