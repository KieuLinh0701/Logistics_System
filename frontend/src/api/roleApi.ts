import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

type Role = { id: number; name: string };

const roleApi = {
  async getAdminRoles() {
    const res = await axiosClient.get<ApiResponse<Role[]>>('/admin/roles');
    return res;
  }
};

export default roleApi;
