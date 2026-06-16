import type {ApiResponse, ListResponse} from "../types/response";
import type {AdminVehicle, ManagerVehicleEditRequest, ManagerVehicleSearchRequest, Vehicle} from "../types/vehicle";
import axiosClient from "./axiosClient";
import type {SearchRequest} from "../types/request.ts";
import {axiosExport} from "./exportClient.ts";

const vehicleApi = {
  async listAdminVehicles(params: { page?: number; limit?: number; search?: string; type?: string; status?: string }) {
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

  async exportManagerVehicles(params: ManagerVehicleSearchRequest) {
    try {
      const res = await axiosExport.get("/manager/vehicles/export", {
        params,
        responseType: "blob",
      });

      const blob = res.data;
      const contentDisposition = res.headers['content-disposition'];

      let fileName = "BaoCao.xlsx";

      if (contentDisposition) {
        let fileNameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].trim());
        } else {
          fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].trim();
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return { success: true, fileName };
    } catch (error) {
      return { success: false, error };
    }
  },
};

export default vehicleApi;


