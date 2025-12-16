import type { ApiResponse, ListResponse } from "../types/response";
import type { ManagerShippingRequestSearchRequest, PublicShippingRequestCreate, ShippingRequest, UserShippingRequestSearchRequest } from "../types/shippingRequest";
import axiosClient from "./axiosClient";

const shippingRequestApi = {
  // User
  async listUserShippingRequests(params: UserShippingRequestSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ShippingRequest>>>("/user/shipping-requests", { params });
    return res;
  },

  async createUserShippingRequest(data: FormData) {
    const res = await axiosClient.post<ApiResponse<boolean>>("/user/shipping-requests", data);
    return res;
  },

  async updateUserShippingRequest(id: number, data: FormData) {
    const res = await axiosClient.put<ApiResponse<boolean>>(`/user/shipping-requests/${id}`, data);
    return res;
  },

  async getUserShippingRequestById(id: number) {
    const res = await axiosClient.get<ApiResponse<ShippingRequest>>(`/user/shipping-requests/${id}`);
    return res;
  },

  async getUserShippingRequestByIdForEdit(id: number) {
    const res = await axiosClient.get<ApiResponse<ShippingRequest>>(`/user/shipping-requests/${id}/edit`);
    return res;
  },

  async cancelUserShippingRequest(id: number) {
    const res = await axiosClient.patch<ApiResponse<Boolean>>(`/user/shipping-requests/${id}/cancel`);
    return res;
  },

  // Manager
  async listManagerShippingRequests(params: ManagerShippingRequestSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ShippingRequest>>>("/manager/shipping-requests", { params });
    return res;
  },

  async getManagerShippingRequestById(id: number) {
    const res = await axiosClient.get<ApiResponse<ShippingRequest>>(`/manager/shipping-requests/${id}`);
    return res;
  },

  async processingManagerShippingRequest(id: number, data: FormData) {
    const res = await axiosClient.put<ApiResponse<boolean>>(`/manager/shipping-requests/${id}`, data);
    return res;
  },

  // Public
  async createPublicShippingRequest(data: PublicShippingRequestCreate) {
    const res = await axiosClient.post<ApiResponse<boolean>>("/public/shipping-requests", data);
    return res;
  },
};

export default shippingRequestApi;