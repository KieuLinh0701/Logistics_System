import type { Office, OfficeSearchRequest } from "../types/office";
import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

const officeApi = {
  // ---------------- Public ---------------- //
  searchOffice: async (params?: OfficeSearchRequest) => {
    const res = await axiosClient.get<ApiResponse<Office[]>>('/public/offices/search', { params });
    return res;
  },
  getHeadOffice: async () => {
    const res = await axiosClient.get<ApiResponse<Office>>('/public/offices/head-office');
    return res;
  },

  // ---------------- Admin ---------------- //
};

export default officeApi;