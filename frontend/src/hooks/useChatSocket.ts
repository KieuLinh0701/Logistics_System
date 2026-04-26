import { useEffect, useRef, useState } from "react";
import { connectSupportSocket, type SupportSocketClient } from "../socket/supportSocket";
import type { SupportChatSendPayload, SupportMessage } from "../types/support";

type UseChatSocketParams = {
  ticketId: number | null;
  accountId: number;
  onIncoming: (message: SupportMessage) => void;
};

const playNotificationSound = () => {
  try {
    const audioContext = new AudioContext();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();

    oscillator.type = "triangle";
    oscillator.frequency.value = 740;
    gainNode.gain.value = 0.035;

    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);

    oscillator.start();
    oscillator.stop(audioContext.currentTime + 0.12);
  } catch {
    // ignore
  }
};

export const useChatSocket = ({ ticketId, accountId, onIncoming }: UseChatSocketParams) => {
  const clientRef = useRef<SupportSocketClient | null>(null);
  const onIncomingRef = useRef(onIncoming);
  const [connected, setConnected] = useState(false);

  onIncomingRef.current = onIncoming;

  useEffect(() => {
    if (!ticketId || !accountId) {
      clientRef.current?.disconnect();
      clientRef.current = null;
      setConnected(false);
      return;
    }

    const client = connectSupportSocket({
      ticketId,
      accountId,
      onMessage: (msg) => {
        onIncomingRef.current(msg);
        if (msg.senderAccountId !== accountId) {
          playNotificationSound();
        }
      },
      onError: () => {
        setConnected(false);
      },
    });

    clientRef.current = client;

    const syncConnection = window.setInterval(() => {
      setConnected(client.isConnected());
    }, 1000);

    return () => {
      window.clearInterval(syncConnection);
      client.disconnect();
      clientRef.current = null;
      setConnected(false);
    };
  }, [ticketId, accountId]);

  const send = (payload: SupportChatSendPayload) => {
    clientRef.current?.send(payload);
  };

  return {
    send,
    connected,
  };
};
