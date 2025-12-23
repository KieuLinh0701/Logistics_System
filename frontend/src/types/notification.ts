import type { Pagination } from "./response";

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  relatedId?: number;
  relatedType?: string;
  userId: number;
  createdAt: string;
  updatedAt: string;
  creatorName?: string;
}

export interface NotificationSearchRequest {
  page?: number; 
  limit?: number; 
  search?: string; 
  isRead?: boolean; 
}

export interface NotificationResponse {
  notifications: Notification[];
  pagination: Pagination;
  unreadCount: number;
}