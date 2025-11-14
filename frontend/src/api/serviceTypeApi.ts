import type { ApiResponse } from "../types/response";
import type { ServiceType } from "../types/serviceType";
import axiosClient from "./axiosClient";

const serviceTypeApi = {
  getActiveServiceTypes: async () => {
    const res = await axiosClient.get<ApiResponse<ServiceType[]>>('/public/service-types/active');
    return res;
  },
};

export default serviceTypeApi;