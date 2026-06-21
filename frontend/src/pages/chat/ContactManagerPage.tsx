import { Avatar, Spin, Typography, message, Image } from "antd";
import { UserOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getUserRole } from "../../utils/authUtils";
import internalChatApi from "../../api/internalChatApi";
import type { InternalChatMessage, InternalChatRoom } from "../../api/internalChatApi";
import ChatMessageInput from "../../components/chat/ChatMessageInput";
import "./InternalChat.css";

dayjs.extend(relativeTime);

const { Text, Title } = Typography;

const UTE_BRAND_COLOR = "#1E4DB7";

const getRoleLabel = (role: string): string => {
  switch (role.toLowerCase()) {
    case "shipper":
      return "Shipper";
    case "driver":
      return "Driver";
    case "manager":
      return "Quản lý bưu cục";
    default:
      return role;
  }
};

const ContactManagerPage: React.FC = () => {
  const navigate = useNavigate();
  const userRole = getUserRole() || "";
  const isShipper = userRole.toLowerCase() === "shipper";
  const isDriver = userRole.toLowerCase() === "driver";

  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [room, setRoom] = useState<InternalChatRoom | null>(null);
  const [messages, setMessages] = useState<InternalChatMessage[]>([]);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchRoom = useCallback(async () => {
    try {
      const res = await internalChatApi.getMyRoom();
      if (res.success) {
        setRoom(res.data || null);
        return res.data || null;
      }
      message.error(res.message || "Không thể tải phòng chat");
      return null;
    } catch {
      message.error("Không thể tải phòng chat");
      return null;
    }
  }, []);

  const fetchMessages = useCallback(async (roomId: number) => {
    try {
      const res = await internalChatApi.getMessages(roomId);
      if (res.success) {
        setMessages(res.data || []);
      }
    } catch {
      message.error("Không thể tải tin nhắn");
    }
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    const currentRoom = await fetchRoom();
    if (currentRoom) {
      await fetchMessages(currentRoom.id);
      internalChatApi.markAsRead(currentRoom.id).catch(() => undefined);
    }
    setLoading(false);
  }, [fetchRoom, fetchMessages]);

  useEffect(() => {
    if (!isShipper && !isDriver) {
      message.error("Bạn không có quyền truy cập trang này");
      navigate("/dashboard");
      return;
    }
    void loadData();

    pollingRef.current = setInterval(() => {
      if (room?.id) {
        void fetchMessages(room.id);
      }
    }, 5000);

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
      }
    };
  }, [isShipper, isDriver, loadData, room?.id, fetchMessages, navigate]);

  useEffect(() => {
    const el = messagesContainerRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages]);

  const handleSend = async (text: string) => {
    if (!text.trim() || !room) {
      return;
    }

    setSending(true);
    try {
      const res = await internalChatApi.sendMessage(room.id, { message: text.trim() });
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data!]);
        setRoom((prev) => prev ? {
          ...prev,
          lastMessage: text.trim(),
          lastMessageAt: res.data!.createdAt,
        } : prev);
      } else {
        message.error(res.message || "Gửi tin nhắn thất bại");
      }
    } catch {
      message.error("Gửi tin nhắn thất bại");
    } finally {
      setSending(false);
    }
  };

  const handleUploadImage = async (file: File) => {
    if (!room) return;

    setUploadingImage(true);
    try {
      const res = await internalChatApi.uploadImage(room.id, file);
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data!]);
        setRoom((prev) => prev ? {
          ...prev,
          lastMessage: "[Hình ảnh]",
          lastMessageAt: res.data!.createdAt,
        } : prev);
      } else {
        console.error("[ContactManagerPage] uploadImage failed:", res);
        message.error(res.message || "Gửi ảnh thất bại");
      }
    } catch (err) {
      console.error("[ContactManagerPage] uploadImage error:", err);
      message.error("Gửi ảnh thất bại");
    } finally {
      setUploadingImage(false);
    }
  };

  const getAvatarStyle = (hasAvatar: boolean): React.CSSProperties => {
    if (hasAvatar) {
      return {};
    }
    return {
      backgroundColor: UTE_BRAND_COLOR,
    };
  };

  if (loading) {
    return (
      <div className="internal-chat-page">
        <div className="internal-chat-main">
          <div className="internal-chat-loading">
            <Spin size="large" />
            <Text>Đang tải...</Text>
          </div>
        </div>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="internal-chat-page">
        <div className="internal-chat-main">
          <div className="internal-chat-empty">
            <Title level={4}>Không tìm thấy phòng chat</Title>
            <Text type="secondary">Vui lòng liên hệ quản lý bưu cục của bạn.</Text>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="internal-chat-page">
      <div className="internal-chat-main">
        {/* Header */}
        <div className="internal-chat-header">
          <div className="internal-chat-header-info">
            <Avatar
              size={40}
              src={room.managerAvatar || undefined}
              icon={<UserOutlined />}
              style={getAvatarStyle(!!room.managerAvatar)}
            />
            <div>
              <Title level={5} style={{ margin: 0 }}>
                {room.managerName || "Quản lý"}
              </Title>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {getRoleLabel("manager")} - {room.officeName}
              </Text>
            </div>
          </div>
        </div>

        {/* Messages */}
        <div ref={messagesContainerRef} className="internal-chat-messages">
          {messages.length === 0 ? (
            <div className="internal-chat-no-messages">
              <Text type="secondary">Chưa có tin nhắn nào. Bắt đầu cuộc trò chuyện!</Text>
            </div>
          ) : (
            messages.map((msg) => {
              const isMine = msg.isMine;
              const hasAvatar = !!msg.senderAvatar;

              return (
                <div
                  key={msg.id}
                  className={`internal-chat-bubble-row ${isMine ? "mine" : "their"}`}
                >
                  {!isMine && (
                    <Avatar
                      size={32}
                      src={msg.senderAvatar || undefined}
                      icon={<UserOutlined />}
                      style={{
                        ...getAvatarStyle(hasAvatar),
                        marginRight: 8,
                        flexShrink: 0
                      }}
                    />
                  )}
                  <div className={`internal-chat-bubble ${isMine ? "mine" : "their"}`}>
                    {!isMine && (
                      <div className="internal-chat-sender-name">
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {msg.senderName}
                        </Text>
                      </div>
                    )}
                    {msg.messageType === "IMAGE" && msg.imageUrl ? (
                      <Image
                        src={msg.imageUrl}
                        alt="Hình ảnh"
                        style={{ maxWidth: "100%", maxHeight: 200, borderRadius: 8, cursor: "pointer" }}
                        preview={{ mask: <span style={{ fontSize: 12 }}>Xem</span> }}
                      />
                    ) : (
                      <div style={{ whiteSpace: "pre-wrap", lineHeight: 1.5 }}>
                        {msg.message}
                      </div>
                    )}
                    <div className="chat-bubble-time">
                      <Text style={{ fontSize: 11, color: isMine ? "rgba(255,255,255,0.85)" : undefined }}>
                        {dayjs(msg.createdAt).format("HH:mm")}
                      </Text>
                    </div>
                  </div>
                  {isMine && (
                    <Avatar
                      size={32}
                      src={msg.senderAvatar || undefined}
                      icon={<UserOutlined />}
                      style={{
                        backgroundColor: UTE_BRAND_COLOR,
                        marginLeft: 8,
                        flexShrink: 0
                      }}
                    />
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Input */}
        <div className="internal-chat-input">
          <ChatMessageInput
            onSend={handleSend}
            onUploadImage={handleUploadImage}
            sending={sending}
            placeholder="Nhập tin nhắn cho quản lý..."
          />
        </div>
      </div>
    </div>
  );
};

export default ContactManagerPage;
