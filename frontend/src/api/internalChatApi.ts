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
};

export default internalChatApi;
