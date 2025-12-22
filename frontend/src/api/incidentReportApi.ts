import type { Incident, ManagerIncidentUpdateRequest } from "../types/incidentReport";
import type { SearchRequest } from "../types/request";
import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";

const incidentReportApi = {
  // Manager
  async listManagerIncidentReports(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<Incident>>>("/manager/incident-reports", { params });
    return res;
  },

  async getManagerIncidentReportById(id: number) {
    const res = await axiosClient.get<ApiResponse<Incident>>(`/manager/incident-reports/${id}`);
    return res;
  },

  async processingManagerIncidentReport(id: number, data: ManagerIncidentUpdateRequest) {
    const res = await axiosClient.put<ApiResponse<Boolean>>(`/manager/incident-reports/${id}`, data);
    return res;
  },
};

export default incidentReportApi;