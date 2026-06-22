import {Button, message, Typography} from "antd";
import {useCallback, useEffect, useMemo, useState} from "react";
import {ArrowLeftOutlined, CloseOutlined, CustomerServiceOutlined, InfoCircleOutlined} from "@ant-design/icons";
import supportApi from "../../api/supportApi";
import type {SupportMessage} from "../../types/support";
import {chatStore} from "../../hooks/chatStore";
import {useChatSocket} from "../../hooks/useChatSocket";
import {getUserId, getUserRole, hasPermissionGroup} from "../../utils/authUtils";
import ChatBubble from "./ChatBubble.tsx";
import ChatHome from "./ChatHome.tsx";
import TicketListView from "./TicketListView.tsx";
import MessageList from "./MessageList";
import ChatMessageInput from "../../components/chat/ChatMessageInput";

const { Text } = Typography;

const statusLabel = (status?: string) => {
  switch (status) {
    case "OPEN": return "Mới";
    case "PENDING": return "Chờ xử lý";
    case "ASSIGNED": return "Đã phân công";
    case "RESOLVED": return "Đã giải quyết";
    case "CLOSED": return "Đã đóng";
    default: return status || "";
  }
};

const ChatWidget: React.FC = () => {
  const [role, setRole] = useState<string | null>(getUserRole());
  const [accountId, setAccountId] = useState<number | null>(getUserId());

  const canUseWidget = hasPermissionGroup(['GROUP_USER', 'USER_SUPPORT_TICKET']);

  const [state, setState] = useState(chatStore.getState());
  const [isMobile, setIsMobile] = useState(false);
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);

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

  const shouldConnectSocket = state.currentView === "ticket-detail" && state.ticketId;

  const { send: sendRealtime, connected } = useChatSocket({
    ticketId: shouldConnectSocket ? state.ticketId : null,
    accountId: accountId || 0,
    onIncoming: (incoming) => {
      chatStore.appendMessage(incoming);

      if (
        incoming.senderAccountId !== accountId &&
        chatStore.getState().chatState === "MINIMIZED"
      ) {
        chatStore.incrementUnread();
      }
    },
  });

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

      if (targetTicketId) {
        void supportApi.getTicketDetail(targetTicketId).then((ticketRes) => {
          if (!ticketRes.success || !ticketRes.data?.ticket) {
            chatStore.goHome();
            return;
          }
          chatStore.setCurrentView("ticket-detail");
          chatStore.goTicketDetail(ticketRes.data.ticket);
        });
        return;
      }

      void openChat();
    };

    window.addEventListener("support-chat-open", onOpenFromEvent as EventListener);
    return () => {
      window.removeEventListener("support-chat-open", onOpenFromEvent as EventListener);
    };
  }, [canUseWidget, openChat]);

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

    if (!state.ticketId) {
      message.error("Vui lòng tạo yêu cầu hỗ trợ trước");
      return;
    }

    setSending(true);

    try {
      if (connected) {
        sendRealtime({
          ticketId: state.ticketId,
          senderAccountId: accountId,
          message: trimmed,
          messageType: "TEXT",
          isInternalNote: false,
        });
        return;
      }

      const sendRes = await supportApi.sendMessage(state.ticketId, {
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

  const handleUploadImage = async (file: File) => {
    if (!state.ticketId || !accountId) {
      message.error("Vui lòng chọn một yêu cầu hỗ trợ trước");
      return;
    }

    setUploadingImage(true);
    try {
      const res = await supportApi.uploadImage(state.ticketId, file);
      if (!res.success) {
        message.error(res.message || "Gửi ảnh thất bại");
        return;
      }
      if (res.data) {
        chatStore.appendMessage(res.data as SupportMessage);
      }
    } catch {
      message.error("Gửi ảnh thất bại");
    } finally {
      setUploadingImage(false);
    }
  };

  const isOpen = state.chatState === "OPEN";

  const modalMessages = useMemo(() => state.messages, [state.messages]);

  if (!canUseWidget || !accountId) {
    return null;
  }

  const renderTicketDetail = () => {
    if (!state.selectedTicket) return null;

    const ticket = state.selectedTicket;
    const isClosed = ticket.status === "CLOSED";
    const isResolved = ticket.status === "RESOLVED";

    const canSend = !isClosed;

    return (
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          height: "100%",
          background: "#f5f7fa",
        }}
      >
        {/* Ticket header info */}
        <div
          style={{
            padding: "8px 12px",
            background: "#fff",
            borderBottom: "1px solid #f0f0f0",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Text strong style={{ fontSize: 14 }}>{ticket.code}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {statusLabel(ticket.status)}
            </Text>
          </div>
          <Button
            type="text"
            size="small"
            icon={<ArrowLeftOutlined />}
            onClick={() => chatStore.goTicketList()}
          >
            Quay lại
          </Button>
        </div>

        {/* Info for resolved ticket */}
        {isResolved && (
          <div style={{ padding: "8px 12px", background: "#e6f7ff", borderBottom: "1px solid #91d5ff" }}>
            <div style={{ display: "flex", alignItems: "flex-start", gap: 6 }}>
              <InfoCircleOutlined style={{ color: "#1890ff", marginTop: 2 }} />
              <Text style={{ fontSize: 12, color: "#595959" }}>
                Yêu cầu đã được đánh dấu giải quyết. Nếu vẫn cần hỗ trợ, bạn có thể nhắn tiếp để mở lại.
              </Text>
            </div>
          </div>
        )}

        {/* Messages */}
        <div style={{ flex: 1, overflow: "hidden" }}>
          <MessageList
            messages={modalMessages}
            currentAccountId={accountId}
            loading={false}
          />
        </div>

        {/* Input */}
        {isClosed ? (
          <div
            style={{
              padding: 12,
              background: "#f5f5f5",
              textAlign: "center",
              borderTop: "1px solid #f0f0f0",
            }}
          >
            <Text type="secondary" style={{ fontSize: 13 }}>
              Yêu cầu này đã được đóng.
            </Text>
          </div>
        ) : (
          <ChatMessageInput
            onSend={sendMessage}
            onUploadImage={handleUploadImage}
            sending={sending || uploadingImage}
            disabled={!canSend}
            placeholder="Nhập tin nhắn..."
          />
        )}
      </div>
    );
  };

  const renderContent = () => {
    switch (state.currentView) {
      case "home":
        return <ChatHome />;
      case "ticket-list":
        return <TicketListView />;
      case "ticket-detail":
        return renderTicketDetail();
      default:
        return <ChatHome />;
    }
  };

  if (!isOpen) {
    return (
      <ChatBubble
        unreadCount={state.unreadCount}
        position={state.position}
        onOpen={() => {
          chatStore.setChatState("OPEN");
        }}
        onPositionChange={onPositionChange}
        draggableEnabled={!isMobile}
      />
    );
  }

  return (
    <div
      style={{
        position: "fixed",
        right: isMobile ? 0 : 20,
        bottom: isMobile ? 0 : 20,
        width: isMobile ? "100vw" : 380,
        height: isMobile ? "100vh" : 560,
        borderRadius: isMobile ? 0 : 14,
        overflow: "hidden",
        background: "#fff",
        boxShadow: "0 18px 46px rgba(0,0,0,0.22)",
        zIndex: 10001,
        display: "flex",
        flexDirection: "column",
        animation: "supportChatEnter 0.16s ease-out",
      }}
    >
      <style>
        {`@keyframes supportChatEnter {
          from { opacity: 0; transform: translateY(12px) scale(0.98); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }`}
      </style>

      {/* Common Header */}
      <div
        style={{
          background: "linear-gradient(135deg, #0284c7, #0369a1)",
          color: "white",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "0 12px",
          height: 56,
          flexShrink: 0,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <CustomerServiceOutlined style={{ color: "white", fontSize: 20 }} />
          <Text style={{ color: "white", fontWeight: 600, fontSize: 15 }}>
            Hỗ trợ khách hàng
          </Text>
        </div>
        <Button
          type="text"
          icon={<CloseOutlined />}
          onClick={minimizeChat}
          style={{ color: "white" }}
        />
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: "hidden", display: "flex", flexDirection: "column" }}>
        {renderContent()}
      </div>
    </div>
  );
};

export default ChatWidget;
