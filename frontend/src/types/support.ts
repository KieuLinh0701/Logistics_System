export type SupportMessageType = "TEXT" | "IMAGE" | "SYSTEM";
export type SupportMessageSenderType = "USER" | "MANAGER" | "ADMIN" | "SYSTEM" | "BOT";
export type SupportTicketStatus = "OPEN" | "PENDING" | "ASSIGNED" | "RESOLVED" | "CLOSED";

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

  status?: SupportTicketStatus;
  subject?: string | null;
  priority?: string | null;
  officeId?: number | null;
  officeName?: string | null;
  unreadCount?: number;
  isAssigned?: boolean;
  closedAt?: string | null;
  closedByName?: string | null;
}

export interface SupportTicketDetail {
  ticket: SupportTicket;
}

export interface SupportMessage {
  id: number;
  ticketId: number;
  senderAccountId: number;
  senderType?: SupportMessageSenderType | null;
  senderLabel?: string | null;
  isBotMessage?: boolean;
  message: string;
  messageType: SupportMessageType;
  isInternalNote: boolean;
  createdAt: string;
  senderName?: string | null;
  senderImage?: string | null;
  isRead?: boolean;
  imageUrl?: string | null;
}

export interface CreateSupportTicketPayload {
  subject?: string;
  initialMessage: string;
  managerAccountId?: number;
  priority?: string;
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

export interface AssignTicketPayload {
  assigneeAccountId?: number;
  officeId?: number;
  note?: string;
}

export interface CloseTicketPayload {
  note?: string;
}

export interface BotPreviewResponse {
  intent: string;
  reply: string;
  suggestCreateTicket: boolean;
  suggestViewTickets: boolean;
}

export interface SupportAssignOfficeOption {
  id: number;
  name: string;
}

export interface SupportAssignManagerOption {
  accountId: number;
  fullName: string;
  email?: string | null;
  phone?: string | null;
}

export interface SupportAssignOptionsResponse {
  ticketId: number;
  suggestedOffices: SupportAssignOfficeOption[];
  allOffices: SupportAssignOfficeOption[] | null;
}
