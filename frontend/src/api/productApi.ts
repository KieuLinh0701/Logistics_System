import type { ApiResponse, BulkResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { Product, UserProductActiveAndInstockRequest, UserProductSearchRequest } from "../types/product";

const productApi = {
  // User
  async getUserProducts(params: UserProductSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Product>>>("/user/products", { params });
    return res;
  },

  async createUserProduct(data: FormData) {
    const res = await axiosClient.post<ApiResponse<Product>>("/user/products", data);
    return res;
  },

  async updateUserProduct(data: FormData) {
    const res = await axiosClient.put<ApiResponse<Product>>("/user/products", data);
    return res;
  },

  async deleteUserProduct(productId: number) {
    const res = await axiosClient.delete<ApiResponse<string>>(`/user/products/${productId}`);
    return res;
  },

  async createBulkUserProduct(data: FormData) {
    const res = await axiosClient.post<BulkResponse<Product>>("/user/products/bulk", data);
    return res;
  },

  async getActiveAndInstockUserProducts(params: UserProductActiveAndInstockRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Product>>>("/user/products/active", { params });
    return res;
  },
};

export default productApi;