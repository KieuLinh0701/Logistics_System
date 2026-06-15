import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { SupportChatSendPayload, SupportMessage } from "../types/support";

type ConnectOptions = {
  accountId: number;
  ticketId: number;
  onMessage: (message: SupportMessage) => void;
  onError?: (error: string) => void;
};

export type SupportSocketClient = {
  send: (payload: SupportChatSendPayload) => void;
  disconnect: () => void;
  isConnected: () => boolean;
};

export const connectSupportSocket = ({ accountId, ticketId, onMessage, onError }: ConnectOptions): SupportSocketClient => {
  void accountId;
  const wsBaseUrl = (import.meta.env.VITE_API_BASE || "http://localhost:8080/api").replace(/\/api$/, "");
  const socket = new SockJS(`${wsBaseUrl}/ws?userId=${accountId}`);

  const stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    debug: () => {},
  });

  stompClient.onConnect = () => {
    stompClient.subscribe(`/topic/support/${ticketId}`, (frame) => {
      try {
        const data = JSON.parse(frame.body) as SupportMessage;
        onMessage(data);
      } catch {
        onError?.("Không thể đọc dữ liệu chat realtime");
      }
    });
  };

  stompClient.onStompError = (frame) => {
    onError?.(frame.headers.message || "Lỗi websocket support");
  };

  stompClient.activate();

  return {
    send: (payload: SupportChatSendPayload) => {
      if (!stompClient.connected) {
        onError?.("WebSocket chưa kết nối");
        return;
      }
      stompClient.publish({
        destination: "/app/chat.sendMessage",
        body: JSON.stringify(payload),
      });
    },
    disconnect: () => {
      stompClient.deactivate();
    },
    isConnected: () => stompClient.connected,
  };
};
