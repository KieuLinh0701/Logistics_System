import { connectSupportSocket, type SupportSocketClient } from "../../socket/supportSocket";
import { Avatar, Badge, Empty, Input, List, Spin, Tag, Typography, message } from "antd";
import { UserOutlined, SendOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getUserId } from "../../utils/authUtils";
import supportApi from "../../api/supportApi";
import type { SupportMessage, SupportTicket } from "../../types/support";
import "./SupportChatPage.css";

const { Text, Title } = Typography;

type MessageMap = Record<number, SupportMessage[]>;
type UnreadMap = Record<number, number>;

const SupportChatPage: React.FC = () => {
  const navigate = useNavigate();
  const params = useParams<{ id?: string }>();
  const accountId = getUserId() || 0;

  const [loadingTickets, setLoadingTickets] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(false);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [messagesByTicket, setMessagesByTicket] = useState<MessageMap>({});
  const [unreadByTicket, setUnreadByTicket] = useState<UnreadMap>({});
  const [selectedTicketId, setSelectedTicketId] = useState<number | null>(null);
  const [inputValue, setInputValue] = useState("");

  const selectedTicketIdRef = useRef<number | null>(null);
  const socketClientsRef = useRef<Map<number, SupportSocketClient>>(new Map());
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

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

  const fetchTickets = useCallback(async () => {
    setLoadingTickets(true);
    try {
      const res = await supportApi.getMyTickets();
      if (!res.success) {
        message.error(res.message || "Không thể tải conversation");
        return;
      }
      setTickets(sortTickets(res.data || []));
    } catch {
      message.error("Không thể tải conversation");
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

  // Các subscription realtime được xử lý bởi `connectSupportSocket` trong lớp socket

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      fetchTickets();
    }, 15000);

    return () => window.clearInterval(timer);
  }, [fetchTickets]);

  useEffect(() => {
    if (!accountId) return;

    const clients = socketClientsRef.current;

    const ticketIds = new Set(tickets.map((t) => t.id));

    // ngắt kết nối các client cho những ticket không còn tồn tại
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

    // đảm bảo có một client cho mỗi ticket
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
      // không làm gì ở đây; việc dọn dẹp cuối cùng sẽ xảy ra khi component unmount
    };
  }, [accountId, tickets, handleIncomingMessage]);

  // Đồng bộ trạng thái `connected` với bất kỳ client socket nào
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

  // Các subscription được quản lý bởi lớp socket

  useEffect(() => {
    if (!tickets.length) {
      return;
    }

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

  // Ngắt kết nối tất cả các client socket khi component unmount
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

  useEffect(() => {
    const el = messagesContainerRef.current;
    if (!el) return;

    el.scrollTop = el.scrollHeight;
  }, [selectedTicketId, messagesByTicket]);

  

  const selectedTicket = useMemo(
    () => tickets.find((ticket) => ticket.id === selectedTicketId) || null,
    [selectedTicketId, tickets],
  );

  const selectedMessages = selectedTicketId ? messagesByTicket[selectedTicketId] || [] : [];

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

  return (
    <div className="support-chat-page">
      <div className="support-chat-sidebar">
        <div className="support-chat-sidebar-header">
          <Title level={4} style={{ margin: 0 }}>CSKH Chat</Title>
          <Tag color={connected ? "green" : "default"}>{connected ? "Trực tuyến" : "Ngoại tuyến"}</Tag>
        </div>

        <Spin spinning={loadingTickets}>
          {tickets.length === 0 ? (
            <div className="support-chat-empty-wrap">
              <Empty description="Chưa có cuộc trò chuyện" />
            </div>
          ) : (
            <List
              dataSource={tickets}
              renderItem={(ticket) => {
                const isActive = ticket.id === selectedTicketId;
                const unread = unreadByTicket[ticket.id] || 0;
                return (
                  <List.Item
                    className={`support-chat-conversation ${isActive ? "active" : ""}`}
                    onClick={() => selectTicket(ticket.id)}
                  >
                    <List.Item.Meta
                      avatar={<Avatar src={ticket.createdByImage || undefined} icon={<UserOutlined />} />}
                      title={
                        <div className="support-chat-conversation-title">
                          <Text strong>{ticket.createdByName ? ticket.createdByName : (ticket.code ? ticket.code : `User #${ticket.createdByAccountId}`)}</Text>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {ticket.latestMessageAt ? dayjs(ticket.latestMessageAt).format("DD/MM HH:mm") : ""}
                          </Text>
                        </div>
                      }
                      description={
                        <div className="support-chat-conversation-desc">
                          <Text ellipsis className="support-chat-preview">
                            {ticket.latestMessage || "(Chưa có tin nhắn)"}
                          </Text>
                          {unread > 0 ? <Badge count={unread} /> : null}
                        </div>
                      }
                    />
                  </List.Item>
                );
              }}
            />
          )}
        </Spin>
      </div>

      <div ref={messagesContainerRef} className="support-chat-main">
        {!selectedTicket ? (
          <div className="support-chat-empty-wrap">
            <Empty description="Chọn cuộc trò chuyện để bắt đầu" />
          </div>
        ) : (
          <>
            <div className="support-chat-main-header">
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Avatar src={selectedTicket.createdByImage || undefined} icon={<UserOutlined />} />
                <div>
                  <Title level={5} style={{ margin: 0 }}>
                    {selectedTicket.createdByName ? selectedTicket.createdByName : (selectedTicket.code ? selectedTicket.code : `User #${selectedTicket.createdByAccountId}`)}
                    <span style={{ marginLeft: 8, color: "#666", fontSize: 12 }}>(USER)</span>
                  </Title>
                </div>
              </div>
            </div>
            <div className="support-chat-message-wrap">
              <Spin spinning={loadingMessages}>
                <div className="support-chat-messages">
                  {selectedMessages.map((msg) => {
                    const isMine = msg.senderAccountId === accountId;
                    return (
                      <div key={msg.id} className={`support-chat-bubble-row ${isMine ? "mine" : "their"}`}>
                        {!isMine ? (
                          <div style={{ marginRight: 8 }}>
                            <Avatar src={msg.senderImage || undefined} icon={<UserOutlined />} />
                          </div>
                        ) : null}

                        <div className={`support-chat-bubble ${isMine ? "mine" : "their"}`}>
                          <div>{msg.message}</div>
                          <Text type="secondary" style={{ fontSize: 11 }}>
                            {dayjs(msg.createdAt).format("DD/MM HH:mm")}
                          </Text>
                        </div>

                        {isMine ? (
                          <div style={{ marginLeft: 8 }}>
                            <Avatar src={msg.senderImage || undefined} icon={<UserOutlined />} />
                          </div>
                        ) : null}
                      </div>
                    );
                  })}
                 
                </div>
              </Spin>
            </div>

            <div className="support-chat-input-wrap">
              <Input.TextArea
                rows={2}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder="Nhập tin nhắn..."
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault();
                    onSend();
                  }
                }}
                style={{ flex: 1, minWidth: 0 }}
              />

              <button
                type="button"
                className="support-chat-send-btn"
                aria-label="Gửi"
                disabled={sending || !inputValue.trim()}
                onClick={onSend}
              >
                <SendOutlined />
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SupportChatPage;
