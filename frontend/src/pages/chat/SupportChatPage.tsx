import { connectSupportSocket, type SupportSocketClient } from "../../socket/supportSocket";
import {
  Avatar,
  Badge,
  Empty,
  Input,
  List,
  Spin,
  Tag,
  Typography,
  message,
  Select,
  Button,
  Popconfirm,
  Alert,
} from "antd";
import {
  UserOutlined,
  SendOutlined,
  UserSwitchOutlined,
  CheckCircleOutlined,
  RedoOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getUserId, getUserRole } from "../../utils/authUtils";
import supportApi from "../../api/supportApi";
import type { SupportMessage, SupportTicket, SupportTicketStatus } from "../../types/support";
import TicketAssignModal from "../../components/chat/TicketAssignModal";
import CloseTicketModal from "../../components/chat/CloseTicketModal";
import "./SupportChatPage.css";

dayjs.extend(relativeTime);

const { Text, Title } = Typography;
const { TextArea } = Input;

const UTE_BRAND_COLOR = "#1E4DB7";
const UTE_BRAND_LIGHT = "#2d5fd6";
const UTE_BRAND_MUTED = "#8baae0";

type MessageMap = Record<number, SupportMessage[]>;

const statusConfig: Record<string, { bg: string; color: string; label: string }> = {
  OPEN: { bg: "#e6f4ff", color: UTE_BRAND_COLOR, label: "Mới" },
  PENDING: { bg: "#fff7e6", color: "#d46b00", label: "Chờ xử lý" },
  ASSIGNED: { bg: "#f9f0ff", color: "#722ed1", label: "Đã phân công" },
  RESOLVED: { bg: "#f6ffed", color: "#389e0d", label: "Đã giải quyết" },
  CLOSED: { bg: "#f5f5f5", color: "#666", label: "Đã đóng" },
};

const filterOptions = [
  { value: "ALL", label: "Tất cả" },
  { value: "OPEN", label: "Đang mở" },
  { value: "ASSIGNED", label: "Đã phân công" },
  { value: "RESOLVED", label: "Đã giải quyết" },
  { value: "CLOSED", label: "Đã đóng" },
  { value: "MY", label: "Của tôi" },
];

const getSenderDisplayLabel = (msg: SupportMessage): string => {
  if (msg.senderType === "ADMIN") {
    return `${msg.senderName || "Admin"} (Admin)`;
  }
  if (msg.senderType === "MANAGER") {
    return `${msg.senderName || "Manager"} (Manager)`;
  }
  if (msg.senderType === "BOT") {
    return "Trợ lý Logistics";
  }
  if (msg.senderType === "SYSTEM") {
    return "Hệ thống";
  }
  return `${msg.senderName || "Shop"} (Shop)`;
};

const SupportChatPage: React.FC = () => {
  const navigate = useNavigate();
  const params = useParams<{ id?: string }>();
  const accountId = getUserId() || 0;
  const userRole = getUserRole() || "";
  const isAdmin = userRole.toLowerCase() === "admin";
  const isManager = userRole.toLowerCase() === "manager";

  const [loadingTickets, setLoadingTickets] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(false);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [messagesByTicket, setMessagesByTicket] = useState<MessageMap>({});
  const [unreadByTicket, setUnreadByTicket] = useState<Record<number, number>>({});
  const [selectedTicketId, setSelectedTicketId] = useState<number | null>(null);
  const [inputValue, setInputValue] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [searchText, setSearchText] = useState("");

  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [closeModalOpen, setCloseModalOpen] = useState(false);
  const [reopenLoading, setReopenLoading] = useState(false);

  const selectedTicketIdRef = useRef<number | null>(null);
  const socketClientsRef = useRef<Map<number, SupportSocketClient>>(new Map());
  const messageListRef = useRef<HTMLDivElement | null>(null);

  const scrollToBottom = () => {
    requestAnimationFrame(() => {
      const el = messageListRef.current;
      if (!el) return;
      el.scrollTop = el.scrollHeight;
    });
  };

  useEffect(() => {
    selectedTicketIdRef.current = selectedTicketId;
  }, [selectedTicketId]);

  const sortTickets = useCallback((list: SupportTicket[]) => {
    return [...list].sort((a, b) => {
      const aTime = new Date(a.latestMessageAt || a.updatedAt).getTime();
      const bTime = new Date(b.latestMessageAt || b.updatedAt).getTime();
      return bTime - aTime;
    });
  }, []);

  const filterTickets = useCallback((list: SupportTicket[]) => {
    let filtered = list;

    if (filterStatus === "MY") {
      filtered = filtered.filter((t) => t.assignedToAccountId === accountId);
    } else if (filterStatus !== "ALL") {
      filtered = filtered.filter((t) => t.status === filterStatus);
    }

    if (searchText.trim()) {
      const search = searchText.toLowerCase();
      filtered = filtered.filter(
        (t) =>
          t.code?.toLowerCase().includes(search) ||
          t.subject?.toLowerCase().includes(search) ||
          t.latestMessage?.toLowerCase().includes(search) ||
          t.createdByName?.toLowerCase().includes(search),
      );
    }

    return filtered;
  }, [filterStatus, searchText, accountId]);

  const fetchTickets = useCallback(async () => {
    setLoadingTickets(true);
    try {
      const res = await supportApi.getMyTickets();
      if (!res.success) {
        message.error(res.message || "Không thể tải danh sách ticket");
        return;
      }
      setTickets(sortTickets(res.data || []));
    } catch {
      message.error("Không thể tải danh sách ticket");
    } finally {
      setLoadingTickets(false);
    }
  }, [sortTickets]);

  const fetchMessages = useCallback(async (ticketId: number) => {
    setLoadingMessages(true);
    try {
      const res = await supportApi.getMessages(ticketId);
      if (!res.success) {
        message.error(res.message || "Không thể tải tin nhắn");
        return;
      }
      setMessagesByTicket((prev) => ({ ...prev, [ticketId]: res.data || [] }));
      setUnreadByTicket((prev) => ({ ...prev, [ticketId]: 0 }));
      supportApi.markMessagesRead(ticketId).catch(() => undefined);
    } catch {
      message.error("Không thể tải tin nhắn");
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  const selectTicket = useCallback(
    (ticketId: number, updateUrl = true) => {
      setSelectedTicketId(ticketId);
      setUnreadByTicket((prev) => ({ ...prev, [ticketId]: 0 }));
      if (!messagesByTicket[ticketId]) {
        fetchMessages(ticketId);
      } else {
        supportApi.markMessagesRead(ticketId).catch(() => undefined);
      }

      if (updateUrl) {
        navigate(`/support/tickets/${ticketId}`);
      }
    },
    [fetchMessages, messagesByTicket, navigate],
  );

  const upsertTicket = useCallback((updatedTicket: SupportTicket) => {
    setTickets((prev) => {
      const idx = prev.findIndex((t) => t.id === updatedTicket.id);
      if (idx === -1) {
        return sortTickets([...prev, updatedTicket]);
      }
      const next = [...prev];
      next[idx] = updatedTicket;
      return sortTickets(next);
    });
  }, [sortTickets]);

  const upsertTicketPreview = useCallback((incoming: SupportMessage) => {
    setTickets((prev) => {
      const idx = prev.findIndex((t) => t.id === incoming.ticketId);
      if (idx === -1) {
        return prev;
      }

      const next = [...prev];
      const current = next[idx];
      next[idx] = {
        ...current,
        latestMessage: incoming.message,
        latestMessageAt: incoming.createdAt,
        updatedAt: incoming.createdAt,
        latestMessageSenderAccountId: incoming.senderAccountId,
        totalMessages: (current.totalMessages || 0) + 1,
      };

      return sortTickets(next);
    });
  }, [sortTickets]);

  const handleIncomingMessage = useCallback(
    (incoming: SupportMessage) => {
      setMessagesByTicket((prev) => {
        const oldList = prev[incoming.ticketId] || [];
        if (oldList.some((item) => item.id === incoming.id)) {
          return prev;
        }
        return {
          ...prev,
          [incoming.ticketId]: [...oldList, incoming],
        };
      });

      upsertTicketPreview(incoming);

      if (selectedTicketIdRef.current === incoming.ticketId) {
        setUnreadByTicket((prev) => ({ ...prev, [incoming.ticketId]: 0 }));
        supportApi.markMessagesRead(incoming.ticketId).catch(() => undefined);
        return;
      }

      if (incoming.senderAccountId !== accountId) {
        setUnreadByTicket((prev) => ({
          ...prev,
          [incoming.ticketId]: (prev[incoming.ticketId] || 0) + 1,
        }));
      }
    },
    [accountId, upsertTicketPreview],
  );

  useEffect(() => {
    void fetchTickets();
  }, [fetchTickets]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      void fetchTickets();
    }, 15000);

    return () => window.clearInterval(timer);
  }, [fetchTickets]);

  useEffect(() => {
    if (!accountId) return;

    const clients = socketClientsRef.current;
    const ticketIds = new Set(tickets.map((t) => t.id));

    for (const [id, client] of clients.entries()) {
      if (!ticketIds.has(id)) {
        try {
          client.disconnect();
        } catch {
          /* ignore */
        }
        clients.delete(id);
      }
    }

    tickets.forEach((ticket) => {
      if (clients.has(ticket.id)) return;
      const client = connectSupportSocket({
        accountId,
        ticketId: ticket.id,
        onMessage: handleIncomingMessage,
        onError: (err) => message.warning(err),
      });
      clients.set(ticket.id, client);
    });

    return () => {
      // cleanup handled in unmount
    };
  }, [accountId, tickets, handleIncomingMessage]);

  useEffect(() => {
    const sync = () => {
      const clients = Array.from(socketClientsRef.current.values());
      const anyConnected = clients.some((c) => !!(c && c.isConnected && c.isConnected()));
      setConnected(anyConnected);
    };

    sync();
    const id = window.setInterval(sync, 1000);
    return () => window.clearInterval(id);
  }, [tickets]);

  useEffect(() => {
    if (!tickets.length) return;

    const routeTicketId = Number(params.id);
    if (params.id && Number.isFinite(routeTicketId) && tickets.some((t) => t.id === routeTicketId)) {
      if (selectedTicketId !== routeTicketId) {
        selectTicket(routeTicketId, false);
      }
      return;
    }

    if (!selectedTicketId || !tickets.some((t) => t.id === selectedTicketId)) {
      selectTicket(tickets[0].id);
    }
  }, [params.id, selectTicket, selectedTicketId, tickets]);

  useEffect(() => {
    return () => {
      socketClientsRef.current.forEach((c) => {
        try {
          c.disconnect();
        } catch {
          /* ignore */
        }
      });
      socketClientsRef.current.clear();
    };
  }, []);

  const selectedTicket = useMemo(
    () => tickets.find((ticket) => ticket.id === selectedTicketId) || null,
    [selectedTicketId, tickets],
  );

  const selectedMessages = selectedTicketId ? messagesByTicket[selectedTicketId] || [] : [];

  const filteredTickets = useMemo(() => filterTickets(tickets), [tickets, filterTickets]);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    scrollToBottom();
  }, [selectedTicketId, selectedMessages.length]);

  const isClosed = selectedTicket?.status === "CLOSED";
  const isResolved = selectedTicket?.status === "RESOLVED";

  const onSend = async () => {
    const text = inputValue.trim();
    if (!text || !selectedTicketId) {
      return;
    }

    setSending(true);

    try {
      const socketClient = selectedTicketId ? socketClientsRef.current.get(selectedTicketId) : undefined;
      if (socketClient && socketClient.isConnected && socketClient.isConnected()) {
        socketClient.send({
          ticketId: selectedTicketId,
          senderAccountId: accountId,
          message: text,
          messageType: "TEXT",
          isInternalNote: false,
        });
      } else {
        const res = await supportApi.sendMessage(selectedTicketId, {
          message: text,
          messageType: "TEXT",
          isInternalNote: false,
        });
        if (!res.success) {
          message.error(res.message || "Gửi tin nhắn thất bại");
          return;
        }
        if (res.data) {
          handleIncomingMessage(res.data);
        }
      }

      setInputValue("");
    } catch {
      message.error("Gửi tin nhắn thất bại");
    } finally {
      setSending(false);
    }
  };

  const handleAssignSuccess = (updatedTicket: SupportTicket) => {
    setAssignModalOpen(false);
    upsertTicket(updatedTicket);
    if (selectedTicketId === updatedTicket.id) {
      void fetchMessages(updatedTicket.id);
    }
  };

  const handleCloseSuccess = (updatedTicket: SupportTicket) => {
    setCloseModalOpen(false);
    upsertTicket(updatedTicket);
    if (selectedTicketId === updatedTicket.id) {
      void fetchMessages(updatedTicket.id);
    }
  };

  const handleReopen = async () => {
    if (!selectedTicket) return;

    setReopenLoading(true);
    try {
      const res = await supportApi.reopenTicket(selectedTicket.id);
      if (!res.success || !res.data) {
        message.error(res.message || "Không thể mở lại ticket");
        return;
      }
      message.success("Đã mở lại ticket");
      upsertTicket(res.data);
      void fetchMessages(selectedTicket.id);
    } catch {
      message.error("Không thể mở lại ticket");
    } finally {
      setReopenLoading(false);
    }
  };

  const getStatusBadge = (status?: SupportTicketStatus) => {
    if (!status) return null;
    const config = statusConfig[status] || { bg: "#f5f5f5", color: "#666", label: status };
    return (
      <Tag
        style={{
          background: config.bg,
          color: config.color,
          border: `1px solid ${config.color}33`,
          fontSize: 11,
          padding: "2px 8px",
          borderRadius: 4,
        }}
      >
        {config.label}
      </Tag>
    );
  };

  const getOnlineBadge = (isOnline: boolean) => {
    return (
      <span className={`support-online-badge ${isOnline ? "online" : "offline"}`}>
        <span className="support-online-dot" />
        {isOnline ? "Trực tuyến" : "Ngoại tuyến"}
      </span>
    );
  };

  const canAssign = isAdmin;
  const canClose = (isAdmin || (isManager && selectedTicket?.assignedToAccountId === accountId)) && !isClosed;
  const canReopen = isAdmin || selectedTicket?.createdByAccountId === accountId;
  const canSendMessage = !isClosed;

  return (
    <div className="support-chat-page">
      {/* Sidebar */}
      <div className="support-chat-sidebar">
        <div className="support-chat-sidebar-header">
          <Title level={4} style={{ margin: 0 }}>CSKH Chat</Title>
          {getOnlineBadge(connected)}
        </div>

        {/* Filter & Search */}
        <div className="support-chat-filter-section">
          <Select
            value={filterStatus}
            onChange={setFilterStatus}
            options={filterOptions}
            style={{ width: "100%" }}
            size="small"
          />
          <Input
            placeholder="Tìm kiếm..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            size="small"
          />
        </div>

        {/* Ticket List */}
        <Spin spinning={loadingTickets}>
          {filteredTickets.length === 0 ? (
            <div className="support-chat-empty-wrap">
              <Empty description="Không có ticket" />
            </div>
          ) : (
            <div className="support-chat-sidebar-list">
            <List
              dataSource={filteredTickets}
              renderItem={(ticket) => {
                const isActive = ticket.id === selectedTicketId;
                const unread = unreadByTicket[ticket.id] || 0;

                return (
                  <List.Item
                    className={`support-chat-conversation ${isActive ? "active" : ""}`}
                    onClick={() => selectTicket(ticket.id)}
                  >
                    <List.Item.Meta
                      avatar={
                        <Badge dot={unread > 0} offset={[-4, 4]}>
                          <Avatar
                            src={ticket.createdByImage || undefined}
                            icon={<UserOutlined />}
                            style={{ backgroundColor: ticket.createdByImage ? undefined : UTE_BRAND_COLOR }}
                          />
                        </Badge>
                      }
                      title={
                        <div className="support-chat-conversation-title">
                          <Text strong style={{ fontSize: 13 }}>
                            {ticket.code}
                          </Text>
                          {getStatusBadge(ticket.status)}
                        </div>
                      }
                      description={
                        <div>
                          <Text ellipsis className="support-chat-preview" style={{ fontSize: 12 }}>
                            {ticket.subject || ticket.latestMessage || "(Không có nội dung)"}
                          </Text>
                        </div>
                      }
                    />
                  </List.Item>
                );
              }}
            />
            </div>
          )}
        </Spin>
      </div>

      {/* Main Chat Area */}
      <div className="support-chat-main">
        {!selectedTicket ? (
          <div className="support-chat-empty-wrap">
            <Empty description="Chọn ticket để bắt đầu" />
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="support-chat-main-header">
              <div style={{ display: "flex", alignItems: "center", gap: 12, flex: 1 }}>
                <Avatar
                  src={selectedTicket.createdByImage || undefined}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: selectedTicket.createdByImage ? undefined : UTE_BRAND_COLOR }}
                />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                    <Title level={5} style={{ margin: 0 }}>
                      {selectedTicket.createdByName || selectedTicket.code}
                    </Title>
                    {getStatusBadge(selectedTicket.status)}
                  </div>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {selectedTicket.subject || "Không có tiêu đề"}
                    {selectedTicket.isAssigned && selectedTicket.assignedToName
                      ? ` → ${selectedTicket.assignedToName} phụ trách`
                      : " → Chưa phân công"}
                  </Text>
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
                {canAssign && (
                  <Button
                    icon={<UserSwitchOutlined />}
                    onClick={() => setAssignModalOpen(true)}
                  >
                    {selectedTicket.isAssigned ? "Chuyển" : "Phân công"}
                  </Button>
                )}

                {canClose && !isResolved && (
                  <Button
                    type="primary"
                    icon={<CheckCircleOutlined />}
                    style={{ background: UTE_BRAND_COLOR, borderColor: UTE_BRAND_COLOR }}
                    onClick={() => setCloseModalOpen(true)}
                  >
                    Đánh dấu đã giải quyết
                  </Button>
                )}

                {canReopen && (isClosed || isResolved) && (
                  <Popconfirm
                    title="Mở lại ticket này?"
                    onConfirm={() => void handleReopen()}
                    okText="Mở lại"
                    cancelText="Hủy"
                  >
                    <Button icon={<RedoOutlined />} loading={reopenLoading}>
                      Mở lại
                    </Button>
                  </Popconfirm>
                )}
              </div>
            </div>

            {/* Messages */}
            <div className="support-chat-message-wrap">
              <Spin spinning={loadingMessages}>
                <div
                  ref={messageListRef}
                  className="support-chat-messages"
                >
                  {selectedMessages.map((msg) => {
                    const isBot = msg.isBotMessage === true || msg.senderType === "BOT";
                    const isSystem =
                      (msg.messageType === "SYSTEM" && !isBot) || msg.senderType === "SYSTEM";

                    if (isSystem) {
                      return (
                        <div key={msg.id} className="support-chat-system-message">
                          <div className="support-chat-system-bubble">
                            <Text type="secondary" style={{ fontSize: 12, fontStyle: "italic" }}>
                              {msg.message}
                            </Text>
                            <Text type="secondary" style={{ fontSize: 10, display: "block", textAlign: "center", marginTop: 2 }}>
                              {dayjs(msg.createdAt).format("DD/MM HH:mm")}
                            </Text>
                          </div>
                        </div>
                      );
                    }

                    const isMine = msg.senderAccountId === accountId;
                    const alignRight = isMine && !isBot;

                    return (
                      <div key={msg.id} className={`support-chat-bubble-row ${alignRight ? "mine" : "their"}`}>
                        {!alignRight || isBot ? (
                          <Avatar
                            src={isBot ? undefined : msg.senderImage || undefined}
                            icon={isBot ? <span style={{ fontSize: 14 }}>🤖</span> : <UserOutlined />}
                            style={{
                              backgroundColor: isBot ? UTE_BRAND_COLOR : UTE_BRAND_COLOR,
                              marginRight: 8,
                              flexShrink: 0,
                            }}
                          />
                        ) : null}

                        <div className={`support-chat-bubble ${alignRight ? "mine" : "their"} ${isBot ? "bot" : ""}`}>
                          {isBot ? (
                            <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
                              <Tag color="processing" style={{ marginInlineEnd: 0 }}>
                                Trợ lý logistics
                              </Tag>
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                Bot
                              </Text>
                            </div>
                          ) : null}

                          {!isBot && !isMine && (
                            <div style={{ marginBottom: 4 }}>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {getSenderDisplayLabel(msg)}
                              </Text>
                            </div>
                          )}

                          <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word", lineHeight: 1.5 }}>
                            {msg.message}
                          </div>
                          <div className={`support-chat-bubble-time ${!alignRight ? "their" : ""}`}>
                            <Text style={{ fontSize: 11, color: alignRight ? "rgba(255,255,255,0.8)" : "#999" }}>
                              {dayjs(msg.createdAt).format("DD/MM HH:mm")}
                            </Text>
                          </div>
                        </div>

                        {alignRight && !isBot ? (
                          <Avatar
                            src={msg.senderImage || undefined}
                            icon={<UserOutlined />}
                            style={{
                              backgroundColor: UTE_BRAND_COLOR,
                              marginLeft: 8,
                              flexShrink: 0,
                            }}
                          />
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              </Spin>
            </div>

            {/* Input */}
            <div className="support-chat-input-wrap">
              <div className="support-chat-input-content">
                {isResolved && (
                  <Alert
                    message="Yêu cầu đã được đánh dấu giải quyết. Nếu vẫn cần hỗ trợ, bạn có thể nhắn tiếp để mở lại."
                    type="info"
                    showIcon
                    style={{ marginBottom: 8, fontSize: 12 }}
                  />
                )}

                {isClosed && (
                  <Alert
                    message="Ticket đã đóng, không thể gửi tin nhắn."
                    type="warning"
                    showIcon
                    style={{ marginBottom: 8 }}
                  />
                )}

                <div className="support-chat-input-row">
                  <TextArea
                    rows={2}
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder={isClosed ? "Ticket đã đóng" : "Nhập tin nhắn..."}
                    disabled={!canSendMessage}
                    onPressEnter={(e) => {
                      if (!e.shiftKey) {
                        e.preventDefault();
                        if (canSendMessage) {
                          void onSend();
                        }
                      }
                    }}
                    style={{ flex: 1, minWidth: 0, resize: "none", borderRadius: 8 }}
                  />

                  <button
                    type="button"
                    className="support-chat-send-btn"
                    aria-label="Gửi"
                    disabled={sending || !inputValue.trim() || !canSendMessage}
                    onClick={() => {
                      if (canSendMessage) {
                        void onSend();
                      }
                    }}
                  >
                    <SendOutlined />
                  </button>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Modals */}
      <TicketAssignModal
        open={assignModalOpen}
        ticket={selectedTicket}
        onCancel={() => setAssignModalOpen(false)}
        onSuccess={handleAssignSuccess}
      />

      <CloseTicketModal
        open={closeModalOpen}
        ticket={selectedTicket}
        onCancel={() => setCloseModalOpen(false)}
        onSuccess={handleCloseSuccess}
      />
    </div>
  );
};

export default SupportChatPage;
