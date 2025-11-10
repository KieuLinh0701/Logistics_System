import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient: Client;

export const connectWebSocket = (userId: number, onMessage: (msg: any) => void) => {
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    reconnectDelay: 5000,
  });

  stompClient.onConnect = () => {
    console.log('Connected to WebSocket');

    stompClient.subscribe(`/user/${userId}/queue/notifications`, (message) => {
      onMessage(JSON.parse(message.body));
    });
  };

  stompClient.activate();
};

export const disconnectWebSocket = () => {
  if (stompClient) stompClient.deactivate();
};