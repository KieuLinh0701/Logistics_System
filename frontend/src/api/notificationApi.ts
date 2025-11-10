import type { NotificationResponse } from "../types/notification";
import axiosClient from "./axiosClient";

const notificationApi = {
  getNotifications: async (params?: { page?: number; limit?: number; search?: string; isRead?: boolean; }) => {
    const res = await axiosClient.get<NotificationResponse>('/notifications', { params });
    return res;
  },

  markAsRead: async (notificationId: number) => {
    const res = await axiosClient.put<NotificationResponse>(`/notifications/${notificationId}/read`);
    return res;
  },

  markAllAsRead: async () => {
    const res = await axiosClient.put<NotificationResponse>('/notifications/mark-all-read');
    return res;
  },
};

export default notificationApi;