import axiosClient from "./axiosClient";
import type { ApiResponse, ListResponse } from "../types/response";
import type { AdminOrder } from "../types/order";
import type { AdminUser } from "../types/user";
import type { AdminVehicle } from "../types/vehicle";
import type { AdminOffice } from "../types/office";
import type { ManagerDashboardOverviewResponse, UserDashboardChartResponse, UserDashboardOverviewResponse } from "../types/dashboard";
import type { SearchRequest } from "../types/request";

const dashboardApi = {
  // Admin 
  async getAdminCounts() {
    const usersRes = await axiosClient.get<ApiResponse<ListResponse<AdminUser>>>('/admin/users', { params: { page: 1, limit: 1 } });
    const ordersRes = await axiosClient.get<ApiResponse<ListResponse<AdminOrder>>>('/admin/orders', { params: { page: 1, limit: 1 } });
    const vehiclesRes = await axiosClient.get<ApiResponse<ListResponse<AdminVehicle>>>('/admin/vehicles', { params: { page: 1, limit: 1 } });
    const officesRes = await axiosClient.get<ApiResponse<ListResponse<AdminOffice>>>('/admin/offices', { params: { page: 1, limit: 1 } });

    const usersTotal = usersRes && (usersRes as any).data && (usersRes as any).data.pagination ? (usersRes as any).data.pagination.total : 0;
    const ordersTotal = ordersRes && (ordersRes as any).data && (ordersRes as any).data.pagination ? (ordersRes as any).data.pagination.total : 0;
    const vehiclesTotal = vehiclesRes && (vehiclesRes as any).data && (vehiclesRes as any).data.pagination ? (vehiclesRes as any).data.pagination.total : 0;
    const officesTotal = officesRes && (officesRes as any).data && (officesRes as any).data.pagination ? (officesRes as any).data.pagination.total : 0;

    return { usersTotal, ordersTotal, vehiclesTotal, officesTotal };
  },

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