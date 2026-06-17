import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient: Client;

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';

export const connectWebSocket = (userId: number, onMessage: (msg: any) => void) => {
  const socket = new SockJS(`${baseUrl}/ws?userId=${userId}`);
  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    reconnectDelay: 5000,
  });

  stompClient.onConnect = () => {
    console.log('Connected to WebSocket');
    stompClient.subscribe(`/user/queue/notifications`, (message) => {
      onMessage(JSON.parse(message.body));
    });
  };

  stompClient.activate();
};

export const disconnectWebSocket = () => {
  if (stompClient) stompClient.deactivate();
};