export type SupportMessageType = "TEXT" | "IMAGE" | "SYSTEM";

export interface SupportTicket {
  id: number;
  code: string;
  createdByAccountId: number;
  createdByImage?: string | null;
  createdByName?: string | null;
  assignedToAccountId: number | null;
  assignedToName: string | null;
  totalMessages: number;
  latestMessage: string | null;
  latestMessageSenderAccountId: number | null;
  latestMessageAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SupportTicketDetail {
  ticket: SupportTicket;
}

export interface SupportMessage {
  id: number;
  ticketId: number;
  senderAccountId: number;
  message: string;
  messageType: SupportMessageType;
  isInternalNote: boolean;
  createdAt: string;
  senderName?: string | null;
  senderImage?: string | null;
}

export interface CreateSupportTicketPayload {
  initialMessage: string;
  managerAccountId?: number;
}

export interface SendSupportMessagePayload {
  message: string;
  messageType: SupportMessageType;
  isInternalNote?: boolean;
}

export interface SupportChatSendPayload {
  ticketId: number;
  senderAccountId: number;
  message: string;
  messageType: SupportMessageType;
  isInternalNote?: boolean;
}
