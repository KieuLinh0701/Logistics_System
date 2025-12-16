import type { ApiResponse, ListResponse } from "../types/response";
import axiosClient from "./axiosClient";
import type { ManagerEmployee, ManagerEmployeeSearchRequest, ManagerEmployeeWithShipperAssignments } from "../types/employee";

const employeeApi = {
  // Manager
  async listManagerEmployees(params: ManagerEmployeeSearchRequest) {
    const res = await axiosClient.get<ApiResponse<ListResponse<ManagerEmployee>>>("/manager/employees", { params });
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
};

export default employeeApi;