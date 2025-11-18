import type { NotificationResponse, NotificationSearchRequest } from "../types/notification";
import type { ApiResponse } from "../types/response";
import axiosClient from "./axiosClient";

const notificationApi = {
  // ---------------- Public ---------------- //
  getNotifications: async (params?: NotificationSearchRequest) => {
    const res = await axiosClient.get<ApiResponse<NotificationResponse>>('/notifications', { params });
    return res;
  },
  markAsRead: async (notificationId: number) => {
    const res = await axiosClient.put<ApiResponse<NotificationResponse>>(`/notifications/${notificationId}/read`);
    return res;
  },
  markAllAsRead: async () => {
    const res = await axiosClient.put<ApiResponse<NotificationResponse>>('/notifications/mark-all-read');
    return res;
  },

  // ---------------- Admin ---------------- //
};

export default notificationApi;