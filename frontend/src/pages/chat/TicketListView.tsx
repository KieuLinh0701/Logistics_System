import { useEffect, useState } from "react";
import { Badge, Button, Empty, List, Spin, Tag, Typography } from "antd";
import { ArrowLeftOutlined, PlusOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import supportApi from "../../api/supportApi";
import type { SupportMessage, SupportTicket, SupportTicketStatus } from "../../types/support";
import { chatStore } from "../../hooks/chatStore";

dayjs.extend(relativeTime);

const { Text } = Typography;

const statusConfig: Record<string, { color: string; label: string }> = {
  OPEN: { color: "blue", label: "Mới" },
  PENDING: { color: "orange", label: "Chờ xử lý" },
  ASSIGNED: { color: "cyan", label: "Đã phân công" },
  RESOLVED: { color: "green", label: "Đã giải quyết" },
  CLOSED: { color: "default", label: "Đã đóng" },
};

const TicketListView: React.FC = () => {
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    void fetchTickets();

    const interval = setInterval(() => {
      void fetchTickets();
    }, 15000);

    return () => clearInterval(interval);
  }, []);

  const fetchTickets = async () => {
    setLoading(true);
    try {
      const res = await supportApi.getMyTickets();
      if (res.success && res.data) {
        setTickets(res.data);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  const handleSelectTicket = async (ticket: SupportTicket) => {
    setSelectedId(ticket.id);
    chatStore.selectTicket(ticket);

    // Fetch messages for this ticket
    try {
      const res = await supportApi.getMessages(ticket.id);
      if (res.success && res.data) {
        chatStore.setMessages(res.data as SupportMessage[]);
      }
    } catch {
      // ignore
    }
  };

  const handleBack = () => {
    chatStore.goHome();
  };

  const handleNewTicket = () => {
    chatStore.goHome();
  };

  const getStatusBadge = (status?: SupportTicketStatus) => {
    if (!status) return null;
    const config = statusConfig[status] || { color: "default", label: status };
    return <Tag color={config.color}>{config.label}</Tag>;
  };

  const formatTime = (dateStr?: string | null) => {
    if (!dateStr) return "";
    return dayjs(dateStr).fromNow();
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        background: "#fafafa",
      }}
    >
      {/* Header with back button */}
      <div
        style={{
          padding: "10px 16px",
          background: "#fff",
          borderBottom: "1px solid #f0f0f0",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={handleBack}
          style={{ marginLeft: -8 }}
        >
          Quay lại
        </Button>
        <Text strong style={{ fontSize: 14 }}>Yêu cầu của tôi</Text>
        <Button
          type="text"
          icon={<PlusOutlined />}
          onClick={handleNewTicket}
          style={{ marginRight: -8 }}
        >
          Mới
        </Button>
      </div>

      {/* List */}
      <div style={{ flex: 1, overflowY: "auto" }}>
        <Spin spinning={loading}>
          {tickets.length === 0 ? (
            <div style={{ padding: 40, textAlign: "center" }}>
              <Empty description="Chưa có yêu cầu hỗ trợ nào" />
            </div>
          ) : (
            <List
              dataSource={tickets}
              renderItem={(ticket) => {
                const isActive = ticket.id === selectedId;
                const unread = ticket.unreadCount || 0;

                return (
                  <List.Item
                    onClick={() => handleSelectTicket(ticket)}
                    style={{
                      padding: "12px 16px",
                      cursor: "pointer",
                      background: isActive ? "#e6f4ff" : "#fff",
                      borderLeft: isActive ? "3px solid #0284c7" : "3px solid transparent",
                      transition: "all 0.2s",
                    }}
                  >
                    <div style={{ width: "100%" }}>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 4 }}>
                        <Text strong style={{ fontSize: 14 }}>
                          {ticket.code}
                        </Text>
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          {getStatusBadge(ticket.status)}
                          {unread > 0 && <Badge count={unread} size="small" />}
                        </div>
                      </div>

                      <div style={{ marginBottom: 4 }}>
                        <Text
                          ellipsis
                          style={{ fontSize: 13, color: "#595959" }}
                        >
                          {ticket.subject || ticket.latestMessage || "(Không có nội dung)"}
                        </Text>
                      </div>

                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {formatTime(ticket.latestMessageAt || ticket.updatedAt)}
                        </Text>
                        {ticket.isAssigned && ticket.assignedToName && (
                          <Text type="secondary" style={{ fontSize: 11 }}>
                            → {ticket.assignedToName}
                          </Text>
                        )}
                      </div>
                    </div>
                  </List.Item>
                );
              }}
            />
          )}
        </Spin>
      </div>
    </div>
  );
};

export default TicketListView;
