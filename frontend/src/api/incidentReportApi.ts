import type {Incident, ManagerIncidentUpdateRequest} from "../types/incidentReport";
import type {SearchRequest} from "../types/request";
import type {ApiResponse, ListResponse} from "../types/response";
import axiosClient from "./axiosClient";
import type {ManagerOrderSearchRequest} from "../types/order.ts";
import {axiosExport} from "./exportClient.ts";

const incidentReportApi = {
  // Manager
  async listManagerIncidentReports(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Incident>>>("/manager/incident-reports", { params });
    return res;
  },

  async getManagerIncidentReportById(id: number) {
    return await axiosClient.get<ApiResponse<Incident>>(`/manager/incident-reports/${id}`);
  },

  async processingManagerIncidentReport(id: number, data: ManagerIncidentUpdateRequest) {
    return await axiosClient.put<ApiResponse<void>>(`/manager/incident-reports/${id}`, data);
  },

  async exportManagerIncidentReports(params: SearchRequest) {
    try {
      const res = await axiosExport.get("/manager/incident-reports/export", {
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

export default incidentReportApi;