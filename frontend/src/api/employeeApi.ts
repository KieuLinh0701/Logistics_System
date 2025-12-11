import type { ApiResponse, BulkResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { Product, UserProductActiveAndInstockRequest } from "../types/product";
import type { ManagerEmployee, ManagerEmployeeSearchRequest } from "../types/employee";

const employeeApi = {
  // Manager
  async listManagerEmployees(params: ManagerEmployeeSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployee>>>("/manager/employees", { params });
    return res;
  },

  async createManagerEmployee(data: FormData) {
    const res = await axiosClient.post<ApiResponse<Boolean>>("/manager/employees", data);
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

export default employeeApi;