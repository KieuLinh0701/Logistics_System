import axiosClient from "./axiosClient";
import type {ApiResponse} from "../types/response";
import type {
    CreateSupportTicketPayload,
    SendSupportMessagePayload,
    SupportMessage,
    SupportTicket,
    SupportTicketDetail,
} from "../types/support";

const supportApi = {
  async createTicket(payload: CreateSupportTicketPayload) {
    return axiosClient.post<ApiResponse<SupportTicket>>("/support/tickets", payload);
  },

  async getMyTickets() {
    return axiosClient.get<ApiResponse<SupportTicket[]>>("/support/tickets/my");
  },

  async getTicketDetail(id: number) {
    return axiosClient.get<ApiResponse<SupportTicketDetail>>(`/support/tickets/${id}`);
  },

  async getMessages(id: number) {
    return axiosClient.get<ApiResponse<SupportMessage[]>>(`/support/tickets/${id}/messages`);
  },

  async sendMessage(id: number, payload: SendSupportMessagePayload) {
    return axiosClient.post<ApiResponse<SupportMessage>>(`/support/tickets/${id}/messages`, payload);
  },

  async markMessagesRead(ticketId: number) {
    return axiosClient.post<ApiResponse<null>>(`/support/messages/mark-read`, { ticketId });
  },
};

export default supportApi;
