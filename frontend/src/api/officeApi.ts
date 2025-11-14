import type { Office, OfficeSearchRequest } from "../types/office";
import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

const officeApi = {
  searchOffice: async (params?: OfficeSearchRequest) => {
    const res = await axiosClient.get<ApiResponse<Office[]>>('/public/offices/search', { params });
    return res;
  },
};

export default officeApi;