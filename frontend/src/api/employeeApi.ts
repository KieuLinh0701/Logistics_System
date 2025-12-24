import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerEmployee, ManagerEmployeePerformanceData, ManagerEmployeeSearchRequest, ManagerEmployeeWithShipperAssignments } from "../types/employee";
import type { SearchRequest } from "../types/request";
import type { ManagerShipment } from "../types/shipment";
import { axiosExport } from "./exportClient";

const employeeApi = {
  // Manager
  async listManagerEmployees(params: ManagerEmployeeSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployee>>>("/manager/employees", { params });
    return res;
  },

  async listManagerEmployeePerformances(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployeePerformanceData>>>("/manager/employees/performance", { params });
    return res;
  },

  async createManagerEmployee(data: Partial<ManagerEmployee>) {
    const res = await axiosClient.post<ApiResponse<Boolean>>("/manager/employees", data);
    return res;
  },

  async updateManagerEmployee(id: number, data: Partial<ManagerEmployee>) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/employees/${id}`, data);
    return res;
  },

  async getManagerActiveShippersWithActiveAssignments(params: ManagerEmployeeSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployeeWithShipperAssignments>>>("/manager/employees/shippers/active/with-assignments", { params });
    return res;
  },

  async getManagerActiveShippers(params: ManagerEmployeeSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployee>>>("/manager/employees/shippers/active", { params });
    return res;
  },

  async getManagerActiveEmployeesByShipmentType(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployee>>>("/manager/employees/shipment-type", { params });
    return res;
  },

  async listManagerShipmentsByEmployeeId(id: number, params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipment>>>(`/manager/employees/${id}/shipments`, { params });
    return res;
  },

  async exportManagerEmployeePerformance(params: SearchRequest) {
    try {
      const res = await axiosExport.get("/manager/employees/export", {
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

export default employeeApi;