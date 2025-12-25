import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";
import type { PaymentCheck, PaymentRequest, PaymentsRequest } from "../types/payment";

const paymentApi = {
  // User
  async createVNPayURLFromList(param: PaymentRequest) {
    const res = await axiosClient.post<ApiResponse<string>>("/user/payment/vnpay/get-url-list", param);
    return res;
  },

  async createVNPayURLFromDetail(param: PaymentRequest) {
    const res = await axiosClient.post<ApiResponse<string>>("/user/payment/vnpay/get-url-detail", param);
    return res;
  },

  async checkPaymentVPN(param: PaymentCheck) {
    const res = await axiosClient.post<ApiResponse<boolean>>("/user/payment/vnpay/check", param);
    return res;
  },

  async createVNPayURLForSettlements(param: PaymentsRequest) {
    const res = await axiosClient.post<ApiResponse<string>>("/user/payment/vnpay/settlements", param);
    return res;
  },
};

export default paymentApi;