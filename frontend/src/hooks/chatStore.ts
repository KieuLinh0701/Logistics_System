import type {SupportMessage, SupportTicket} from "../types/support";

export type ChatState = "MINIMIZED" | "OPEN";
export type ChatView = "home" | "ticket-list" | "ticket-detail";

export type ChatPosition = {
  x: number;
  y: number;
};

export type ChatWidgetState = {
  chatState: ChatState;
  currentView: ChatView;
  unreadCount: number;
  messages: SupportMessage[];
  position: ChatPosition;
  ticketId: number | null;
  selectedTicket: SupportTicket | null;
};

type Listener = (state: ChatWidgetState) => void;

const listeners = new Set<Listener>();

const getDefaultPosition = (): ChatPosition => {
  if (typeof window === "undefined") {
    return { x: 0, y: 0 };
  }

  return {
    x: Math.max(12, window.innerWidth - 84),
    y: Math.max(12, window.innerHeight - 96),
  };
};

const loadPosition = (): ChatPosition => {
  if (typeof window === "undefined") {
    return getDefaultPosition();
  }

  try {
    const raw = localStorage.getItem("chat-position");
    if (!raw) {
      return getDefaultPosition();
    }
    const parsed = JSON.parse(raw) as ChatPosition;
    if (typeof parsed.x !== "number" || typeof parsed.y !== "number") {
      return getDefaultPosition();
    }
    return parsed;
  } catch {
    return getDefaultPosition();
  }
};

let state: ChatWidgetState = {
  chatState: "MINIMIZED",
  currentView: "home",
  unreadCount: 0,
  messages: [],
  position: loadPosition(),
  ticketId: null,
  selectedTicket: null,
};

const emit = () => {
  listeners.forEach((listener) => listener(state));
};

export const chatStore = {
  subscribe(listener: Listener) {
    listeners.add(listener);
    listener(state);
    return () => {
      listeners.delete(listener);
    };
  },

  getState() {
    return state;
  },

  setChatState(chatState: ChatState) {
    state = { ...state, chatState };
    emit();
  },

  setUnreadCount(unreadCount: number) {
    state = { ...state, unreadCount: Math.max(0, unreadCount) };
    emit();
  },

  incrementUnread() {
    state = { ...state, unreadCount: state.unreadCount + 1 };
    emit();
  },

  setMessages(messages: SupportMessage[]) {
    state = { ...state, messages };
    emit();
  },

  appendMessage(message: SupportMessage) {
    if (state.messages.some((item) => item.id === message.id)) {
      return;
    }
    state = { ...state, messages: [...state.messages, message] };
    emit();
  },

  setPosition(position: ChatPosition) {
    state = { ...state, position };
    emit();
  },

  setTicketId(ticketId: number | null) {
    state = { ...state, ticketId };
    emit();
  },

  setSelectedTicket(ticket: SupportTicket | null) {
    state = {
      ...state,
      selectedTicket: ticket,
      ticketId: ticket?.id ?? null,
    };
    emit();
  },

  setCurrentView(view: ChatView) {
    state = { ...state, currentView: view };
    emit();
  },

  selectTicket(ticket: SupportTicket) {
    state = {
      ...state,
      selectedTicket: ticket,
      ticketId: ticket.id,
      currentView: "ticket-detail",
      messages: [],
    };
    emit();
  },

  clearSelectedTicket() {
    state = {
      ...state,
      selectedTicket: null,
      ticketId: null,
      messages: [],
    };
    emit();
  },

  goHome() {
    state = {
      ...state,
      currentView: "home",
    };
    emit();
  },

  goTicketList() {
    state = {
      ...state,
      currentView: "ticket-list",
    };
    emit();
  },

  goTicketDetail(ticket: SupportTicket) {
    state = {
      ...state,
      selectedTicket: ticket,
      ticketId: ticket.id,
      currentView: "ticket-detail",
      messages: [],
    };
    emit();
  },

  reset() {
    state = {
      chatState: "MINIMIZED",
      currentView: "home",
      unreadCount: 0,
      messages: [],
      position: loadPosition(),
      ticketId: null,
      selectedTicket: null,
    };
    emit();
  },
};
