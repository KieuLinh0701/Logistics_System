import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerShipperAssignment, ManagerShipperAssignmentEditRequest, ManagerShipperAssignmentSearchRequest } from "../types/shipperAssignment";
import { axiosExport } from "./exportClient";

const shipperAssignmentApi = {
  // Manager
  async createManagerShipperAssignment(data: ManagerShipperAssignmentEditRequest) {
    const res = await axiosClient.post<ApiResponse<Boolean>>("/manager/shipper-assignments", data);
    return res;
  },

  async updateManagerShipperAssignment(id: number, data: ManagerShipperAssignmentEditRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/shipper-assignments/${id}`, data);
    return res;
  },

  async deleteManagerFutureShipperAssignment(id: number) {
    const res = await axiosClient.delete<ApiResponse<Boolean>>(`/manager/shipper-assignments/${id}`);
    return res;
  },

  async listManagerShipperAssignments(params: ManagerShipperAssignmentSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerShipperAssignment>>>("/manager/shipper-assignments", { params });
    return res;
  },

  async exportManagerShipperAssignments(params: ManagerShipperAssignmentSearchRequest) {
    try {
      const res = await axiosExport.get("/manager/shipper-assignments/export", {
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

export default shipperAssignmentApi;