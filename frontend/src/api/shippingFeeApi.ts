import type {ApiResponse} from "../types/response";
import type {
    CalculateShippingFeeRequest,
    CalculateTotalFeeUserRequest,
    CalculateWeightRequest
} from "../types/shippingFee";
import axiosClient from "./axiosClient";

const shippingFeeApi = {
  // common
  calculateShippingFee: async (params: CalculateShippingFeeRequest) => {
    const res = await axiosClient.get<ApiResponse<number>>(
      "/public/fees/shipping",
      { params }
    );
    return res;
  },

  calculateTotalFeeUser: async (params: CalculateTotalFeeUserRequest) => {
    const res = await axiosClient.get<ApiResponse<number>>(
      "/public/fees/total",
      { params }
    );
    return res;
  },

  calculateTotalFeeMananager: async (params: CalculateTotalFeeUserRequest) => {
    const res = await axiosClient.get<ApiResponse<number>>(
      "/public/fees/total-manager",
      { params }
    );
    return res;
  },

  calculateWeight: async (params: CalculateWeightRequest) => {
    const res = await axiosClient.get<ApiResponse<number>>(
      "/public/fees/weight",
      { params }
    );
    return res;
  },
};

export default shippingFeeApi;