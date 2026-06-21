import axiosClient from "./axiosClient";
import type { ApiResponse } from "../types/response";

export interface InternalChatRoom {
  id: number;
  employeeAccountId: number;
  employeeName: string;
  employeeRole: string;
  employeeAvatar: string | null;
  managerAccountId: number;
  managerName: string;
  managerAvatar: string | null;
  officeId: number;
  officeName: string;
  lastMessage: string | null;
  lastMessageAt: string | null;
  lastSenderAccountId: number | null;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface InternalChatMessage {
  id: number;
  roomId: number;
  senderAccountId: number;
  senderName: string;
  senderRole: string;
  senderAvatar: string | null;
  message: string;
  messageType?: string;
  imageUrl?: string | null;
  isMine: boolean;
  isRead: boolean;
  createdAt: string;
}

export interface SendMessagePayload {
  message: string;
}

const internalChatApi = {
  getMyRoom() {
    return axiosClient.get<ApiResponse<InternalChatRoom>>("/internal-chat/my-room");
  },

  getRooms() {
    return axiosClient.get<ApiResponse<InternalChatRoom[]>>("/internal-chat/rooms");
  },

  getRoom(roomId: number) {
    return axiosClient.get<ApiResponse<InternalChatRoom>>(`/internal-chat/rooms/${roomId}`);
  },

  getMessages(roomId: number) {
    return axiosClient.get<ApiResponse<InternalChatMessage[]>>(`/internal-chat/rooms/${roomId}/messages`);
  },

  sendMessage(roomId: number, payload: SendMessagePayload) {
    return axiosClient.post<ApiResponse<InternalChatMessage>>(`/internal-chat/rooms/${roomId}/messages`, payload);
  },

  markAsRead(roomId: number) {
    return axiosClient.post<ApiResponse<null>>(`/internal-chat/rooms/${roomId}/read`);
  },

  uploadImage(roomId: number, file: File) {
    console.log("[internalChatApi.uploadImage] Request:", {
      roomId,
      fileName: file.name,
      fileType: file.type,
      fileSize: file.size,
      formDataKey: "file",
    });

    const formData = new FormData();
    formData.append("file", file);

    return axiosClient
      .post<ApiResponse<InternalChatMessage>>(`/internal-chat/rooms/${roomId}/messages/image`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      })
      .then((res) => {
        console.log("[internalChatApi.uploadImage] Success:", res);
        return res;
      })
      .catch((error) => {
        console.error("[internalChatApi.uploadImage] Error:", {
          status: error.response?.status,
          data: error.response?.data,
          message: error.message,
        });
        throw error;
      });
  },
};

export default internalChatApi;
