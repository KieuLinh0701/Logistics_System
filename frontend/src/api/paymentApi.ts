import axiosClient from "./axiosClient";
import type {ApiResponse} from "../types/response";
import type {PaymentCheck} from "../types/payment";

const paymentApi = {
    // User
    async checkPaymentVPN(param: PaymentCheck) {
        const res = await axiosClient.post<ApiResponse<boolean>>("/user/payment/vnpay/check", param);
        return res;
    },

    async createVNPayURLForSettlements() {
        const res = await axiosClient.post<ApiResponse<string>>("/user/payment/vnpay/settlements");
        return res;
    },
};

export default paymentApi;