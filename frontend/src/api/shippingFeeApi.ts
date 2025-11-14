import type { ApiResponse } from "../types/response";
import type { CalculateShippingFeeRequest } from "../types/shippingFee";
import axiosClient from "./axiosClient";

const shippingFeeApi = {
  calculateShippingFee: async (params: CalculateShippingFeeRequest) => {
    const res = await axiosClient.get<ApiResponse<number>>(
      "/public/shipping-fee/calculate",
      { params }
    );
    return res;
  },
};

export default shippingFeeApi;