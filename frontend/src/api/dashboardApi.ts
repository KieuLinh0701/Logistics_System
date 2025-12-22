import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";
import type { ManagerDashboardOverviewResponse, UserDashboardChartResponse, UserDashboardOverviewResponse } from "../types/dashboard";
import type { SearchRequest } from "../types/request";

const dashboardApi = {
  // User
  async getUserOverview() {
    const res = await axiosClient.get<ApiResponse<UserDashboardOverviewResponse>>("/user/dashboard/overview");
    return res;
  },

  async getUserChart(params: SearchRequest) {
    const res = await axiosClient.get<ApiResponse<UserDashboardChartResponse>>("/user/dashboard/chart", { params });
    return res;
  },

  // Manager
  async getManagerOverview() {
    const res = await axiosClient.get<ApiResponse<ManagerDashboardOverviewResponse>>("/manager/dashboard/overview");
    return res;
  },
};

export default dashboardApi;