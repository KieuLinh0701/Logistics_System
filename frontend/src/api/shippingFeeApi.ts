import type { ShippingFeeResponse, CalculateShippingFeeRequest } from "../types/shippingFee";
import axiosClient from "./axiosClient";

const shippingFeeApi = {
  calculateShippingFee: async (params: CalculateShippingFeeRequest) => {
    const res = await axiosClient.get<ShippingFeeResponse<number>>(
      "/public/shipping-fee/calculate",
      { params }
    );
    return res;
  },
};

export default shippingFeeApi;