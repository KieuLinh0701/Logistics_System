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

export interface NotificationResponse {
  success: boolean;
  data?: {
    notifications: Notification[];
    pagination: {
      total: number;
      page: number;
      limit: number;
      totalPages: number;
    };
    unreadCount: number;
  };
  message?: string;
}