import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";
import type { Address, AddressRequest } from "../types/address";

const addressApi = {
  // User
  async getUserAddresses() {
    const res = await axiosClient.get<ApiResponse<Address[]>>("/user/addresses");
    return res;
  },

  async createUserAddress(data: AddressRequest) {
    const res = await axiosClient.post<ApiResponse<Address>>("/user/addresses", data);
    return res;
  },

  async updateUserAddress(id: number, data: AddressRequest) {
    const res = await axiosClient.put<ApiResponse<Address>>(`/user/addresses/${id}`, data);
    return res;
  },

  async deleteUserAddress(id: number) {
    const res = await axiosClient.delete<ApiResponse<string>>(`/user/addresses/${id}`);
    return res;
  },

  async setDefaultUserAddress(id: number) {
    const res = await axiosClient.patch<ApiResponse<string>>(`/user/addresses/${id}/default`);
    return res;
  },
};

export default addressApi;