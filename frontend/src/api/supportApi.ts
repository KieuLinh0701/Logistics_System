import axiosClient from "./axiosClient";
import type {ApiResponse} from "../types/response";
import type {
    BotPreviewResponse,
    CreateSupportTicketPayload,
    SendSupportMessagePayload,
    SupportMessage,
    SupportTicket,
    SupportTicketDetail,
    AssignTicketPayload,
    CloseTicketPayload,
    SupportAssignOptionsResponse,
    SupportAssignManagerOption,
} from "../types/support";

const supportApi = {
  async previewBotMessage(message: string) {
    return axiosClient.post<ApiResponse<BotPreviewResponse>>("/support/bot/preview", { message });
  },

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

  async uploadImage(ticketId: number, file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return axiosClient.post<ApiResponse<SupportMessage>>(`/support/tickets/${ticketId}/messages/image`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },

  async markMessagesRead(ticketId: number) {
    return axiosClient.post<ApiResponse<null>>(`/support/messages/mark-read`, { ticketId });
  },

  async assignTicket(id: number, payload: AssignTicketPayload) {
    return axiosClient.post<ApiResponse<SupportTicket>>(`/support/tickets/${id}/assign`, payload);
  },

  async closeTicket(id: number, payload?: CloseTicketPayload) {
    return axiosClient.post<ApiResponse<SupportTicket>>(`/support/tickets/${id}/close`, payload || {});
  },

  async reopenTicket(id: number) {
    return axiosClient.post<ApiResponse<SupportTicket>>(`/support/tickets/${id}/reopen`);
  },

  async getAssignOptions(ticketId: number) {
    return axiosClient.get<ApiResponse<SupportAssignOptionsResponse>>(`/support/tickets/${ticketId}/assign-options`);
  },

  async getManagersByOffice(officeId: number) {
    return axiosClient.get<ApiResponse<SupportAssignManagerOption[]>>(`/support/offices/${officeId}/managers`);
  },
};

export default supportApi;
