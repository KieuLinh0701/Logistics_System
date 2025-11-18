import type { ApiResponse } from "../types/response";
import type { ServiceType, ServiceTypeWithShippingRatesResponse } from "../types/serviceType";
import axiosClient from "./axiosClient";

const serviceTypeApi = {
  // ---------------- Public ---------------- //
  getActiveServiceTypes: async () => {
    const res = await axiosClient.get<ApiResponse<ServiceType[]>>('/public/service-types/active');
    return res;
  },
  getActiveServicesWithRates: async () => {
    const res = await axiosClient.get<ApiResponse<ServiceTypeWithShippingRatesResponse[]>>('/public/service-types/active-with-rates');
    return res;
  },

  // ---------------- Admin ---------------- //
};

export default serviceTypeApi;