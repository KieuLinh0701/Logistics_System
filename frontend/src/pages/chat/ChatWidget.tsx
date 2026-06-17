import {message} from "antd";
import {useCallback, useEffect, useMemo, useState} from "react";
import supportApi from "../../api/supportApi";
import type {SupportMessage, SupportTicket} from "../../types/support";
import {chatStore} from "../../hooks/chatStore";
import {useChatSocket} from "../../hooks/useChatSocket";
import {getUserId, getUserRole} from "../../utils/authUtils";
import ChatBubble from "./ChatBubble.tsx";
import ChatModal from "./ChatModal.tsx";

const ChatWidget: React.FC = () => {
  const [role, setRole] = useState<string | null>(getUserRole());
  const [accountId, setAccountId] = useState<number | null>(getUserId());

  const canUseWidget = role === "user";

  const [state, setState] = useState(chatStore.getState());
  const [isMobile, setIsMobile] = useState(false);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (!canUseWidget) {
      return;
    }

    const unsubscribe = chatStore.subscribe(setState);
    return () => {
      unsubscribe();
    };
  }, [canUseWidget]);

  useEffect(() => {
    const handler = () => {
      setRole(getUserRole());
      setAccountId(getUserId());
    };

    window.addEventListener("auth-change", handler);
    return () => window.removeEventListener("auth-change", handler);
  }, []);

  useEffect(() => {
    const updateViewport = () => {
      setIsMobile(window.innerWidth <= 768);
    };

    updateViewport();
    window.addEventListener("resize", updateViewport);

    return () => {
      window.removeEventListener("resize", updateViewport);
    };
  }, []);

  const { send: sendRealtime, connected } = useChatSocket({
    ticketId: state.ticketId,
    accountId: accountId || 0,
    onIncoming: (incoming) => {
      chatStore.appendMessage(incoming);

      if (incoming.senderAccountId !== accountId && chatStore.getState().chatState === "MINIMIZED") {
        chatStore.incrementUnread();
      }
    },
  });

  const hydrateTicket = useCallback(async (ticket: SupportTicket) => {
    chatStore.setTicketId(ticket.id);

    const msgRes = await supportApi.getMessages(ticket.id);
    if (msgRes.success && msgRes.data) {
      chatStore.setMessages(msgRes.data as SupportMessage[]);
      return;
    }
    chatStore.setMessages([]);
  }, []);

  useEffect(() => {
    if (!canUseWidget || !accountId) {
      return;
    }

    let mounted = true;

    const initConversation = async () => {
      try {
        const ticketsRes = await supportApi.getMyTickets();
        if (!mounted) {
          return;
        }

        if (!ticketsRes.success || !ticketsRes.data || ticketsRes.data.length === 0) {
          chatStore.setTicketId(null);
          chatStore.setMessages([]);
          return;
        }

        const list = ticketsRes.data;
        const activeTicket = list[0];
        if (activeTicket) {
          await hydrateTicket(activeTicket);
        }
      } catch {
        chatStore.setTicketId(null);
        chatStore.setMessages([]);
      }
    };

    void initConversation();

    return () => {
      mounted = false;
    };
  }, [canUseWidget, accountId, hydrateTicket]);

  const openChat = useCallback(async () => {
    chatStore.setChatState("OPEN");
    chatStore.setUnreadCount(0);

    if (!state.ticketId) {
      return;
    }

    try {
      await supportApi.markMessagesRead(state.ticketId);
    } catch {
      // ignore
    }
  }, [state.ticketId]);

  useEffect(() => {
    if (!canUseWidget) {
      return;
    }

    const onOpenFromEvent = (event: Event) => {
      const customEvent = event as CustomEvent<{ ticketId?: number }>;
      const targetTicketId = customEvent.detail?.ticketId;
      if (targetTicketId && targetTicketId !== chatStore.getState().ticketId) {
        void supportApi.getTicketDetail(targetTicketId).then((ticketRes) => {
          if (!ticketRes.success || !ticketRes.data?.ticket) {
            return;
          }
          void hydrateTicket(ticketRes.data.ticket);
          void openChat();
        });
        return;
      }

      void openChat();
    };

    window.addEventListener("support-chat-open", onOpenFromEvent as EventListener);
    return () => {
      window.removeEventListener("support-chat-open", onOpenFromEvent as EventListener);
    };
  }, [canUseWidget, hydrateTicket, state.ticketId, openChat]);

  const minimizeChat = () => {
    chatStore.setChatState("MINIMIZED");
  };

  const onPositionChange = (position: { x: number; y: number }) => {
    chatStore.setPosition(position);
    localStorage.setItem("chat-position", JSON.stringify(position));
  };

  const sendMessage = async (content: string) => {
    const trimmed = content.trim();
    if (!trimmed || !accountId) {
      return;
    }

    setSending(true);

    try {
      const currentTicketId = chatStore.getState().ticketId;

      if (!currentTicketId) {
        const createRes = await supportApi.createTicket({
          initialMessage: trimmed,
        });

        if (!createRes.success || !createRes.data) {
          message.error(createRes.message || "Không thể tạo cuộc trò chuyện hỗ trợ");
          return;
        }

        await hydrateTicket(createRes.data);
        chatStore.setUnreadCount(0);
        return;
      }

      if (connected) {
        sendRealtime({
          ticketId: currentTicketId,
          senderAccountId: accountId,
          message: trimmed,
          messageType: "TEXT",
          isInternalNote: false,
        });
        return;
      }

      const sendRes = await supportApi.sendMessage(currentTicketId, {
        message: trimmed,
        messageType: "TEXT",
        isInternalNote: false,
      });

      if (!sendRes.success) {
        message.error(sendRes.message || "Không thể gửi tin nhắn");
        return;
      }

      if (sendRes.data) {
        chatStore.appendMessage(sendRes.data as SupportMessage);
      }
    } catch {
      message.error("Không thể gửi tin nhắn");
    } finally {
      setSending(false);
    }
  };

  const isOpen = state.chatState === "OPEN";

  const modalMessages = useMemo(() => state.messages, [state.messages]);

  if (!canUseWidget || !accountId) {
    return null;
  }

  return (
    <>
      {!isOpen ? (
        <ChatBubble
          unreadCount={state.unreadCount}
          position={state.position}
          onOpen={() => void openChat()}
          onPositionChange={onPositionChange}
          draggableEnabled={!isMobile}
        />
      ) : null}

      <ChatModal
        open={isOpen}
        messages={modalMessages}
        currentAccountId={accountId}
        sending={sending}
        isMobile={isMobile}
        onClose={minimizeChat}
        onSend={sendMessage}
      />
    </>
  );
};

export default ChatWidget;
