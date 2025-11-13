import type { ServiceType, ServiceTypeResponse } from "../types/serviceType";
import axiosClient from "./axiosClient";

const serviceTypeApi = {
  getActiveServiceTypes: async () => {
    const res = await axiosClient.get<ServiceTypeResponse<ServiceType[]>>('/public/service-types/active');
    return res;
  },
};

export default serviceTypeApi;