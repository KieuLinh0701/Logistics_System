import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";
import type { PaymentCheck, PaymentRequest } from "../types/payment";

const paymentApi = {
  // User
  async createVNPayURL(param: PaymentRequest) {
    const res = await axiosClient.post<ApiResponse<string>>("/user/payment/vnpay/get-url", param);
    return res;
  },

  async checkPaymentVPN(param: PaymentCheck) {
    const res = await axiosClient.post<ApiResponse<boolean>>("/user/payment/vnpay/check", param);
    return res;
  },
};

export default paymentApi;