import type { ApiResponse, ListResponse } from "../types/response";
import type { AdminVehicle, ManagerVehicleEditRequest, ManagerVehicleSearchRequest, Vehicle } from "../types/vehicle";
import axiosClient from "./axiosClient";

const vehicleApi = {
  async listAdminVehicles(params: { page?: number; limit?: number; search?: string }) {
    const res = await axiosClient.get<ApiResponse<{ data: AdminVehicle[]; pagination: any }>>("/admin/vehicles", { params });
    return res;
  },

  async createAdminVehicle(data: {
    licensePlate: string;
    type: string;
    capacity: number;
    status: string;
    description?: string;
    officeId: number;
  }) {
    const res = await axiosClient.post<ApiResponse<AdminVehicle>>("/admin/vehicles", data);
    return res;
  },

  async updateAdminVehicle(id: number, data: {
    type?: string;
    capacity?: number;
    status?: string;
    description?: string;
    officeId?: number;
  }) {
    const res = await axiosClient.put<ApiResponse<AdminVehicle>>(`/admin/vehicles/${id}`, data);
    return res;
  },

  async deleteAdminVehicle(id: number) {
    const res = await axiosClient.delete<ApiResponse<null>>(`/admin/vehicles/${id}`);
    return res;
  },

  // Manager
  async listManagerVehicles(params: ManagerVehicleSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Vehicle>>>("/manager/vehicles", { params });
    return res;
  },

  async updateManagerVehicle(id: number, data: ManagerVehicleEditRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/vehicles/${id}`, data);
    return res;
  },

  async getAvailableVehicles() {
    const res = await axiosClient.get<ApiResponse<Vehicle[]>>("/manager/vehicles/available");
    return res;
  },
};

export default vehicleApi;


