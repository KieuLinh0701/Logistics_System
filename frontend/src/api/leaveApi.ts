import axiosClient from "./axiosClient";
import type {ApiResponse} from "../types/response";
import type {ApproveLeavePayload, CreateLeavePayload, LeaveItem} from "../types/leave";

const leaveApi = {
  async createLeave(payload: CreateLeavePayload) {
    return axiosClient.post<ApiResponse<LeaveItem>>("/leaves", payload);
  },

  async getMyLeaves() {
    return axiosClient.get<ApiResponse<LeaveItem[]>>("/leaves/my");
  },

  async cancelLeave(id: number) {
    return axiosClient.put<ApiResponse<boolean>>(`/leaves/${id}/cancel`);
  },

  async getOfficeLeaves() {
    return axiosClient.get<ApiResponse<LeaveItem[]>>("/leaves/office");
  },

  async approveLeave(id: number, payload: ApproveLeavePayload) {
    return axiosClient.put<ApiResponse<LeaveItem>>(`/leaves/${id}/approve`, payload);
  },
};

export default leaveApi;
