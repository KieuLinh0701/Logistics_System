import {Avatar, Badge, Empty, Image, Input, List, message, Select, Spin, Typography,} from "antd";
import {PictureOutlined, SearchOutlined, UserOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useNavigate} from "react-router-dom";
import {getUserRole} from "../../utils/authUtils";
import type {InternalChatMessage, InternalChatRoom} from "../../api/internalChatApi";
import internalChatApi from "../../api/internalChatApi";
import ChatMessageInput from "../../components/chat/ChatMessageInput";
import "./InternalChat.css";

dayjs.extend(relativeTime);

const { Text, Title } = Typography;
const { TextArea } = Input;

const UTE_BRAND_COLOR = "#1E4DB7";
const DRIVER_COLOR = "#fa8c16";

const getRoleLabel = (role: string): string => {
  switch (role.toLowerCase()) {
    case "shipper":
      return "Shipper";
    case "driver":
      return "Driver";
    default:
      return role;
  }
};

const filterRoleOptions = [
  { value: "ALL", label: "Tất cả" },
  { value: "SHIPPER", label: "Nhân viên giao hàng" },
  { value: "DRIVER", label: "Tài xế" },
];

const InternalEmployeeChatPage: React.FC = () => {
  const navigate = useNavigate();
  const userRole = getUserRole() || "";
  const isManager = userRole.toLowerCase() === "manager";
  const isAdmin = userRole.toLowerCase() === "admin";

  const [loadingRooms, setLoadingRooms] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [rooms, setRooms] = useState<InternalChatRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<InternalChatRoom | null>(null);
  const [messages, setMessages] = useState<InternalChatMessage[]>([]);
  const [filterRole, setFilterRole] = useState("ALL");
  const [searchText, setSearchText] = useState("");
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const roomsPollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const fetchRooms = useCallback(async () => {
    try {
      const res = await internalChatApi.getRooms();
      if (res.success) {
        setRooms(res.data || []);
      }
    } catch {
      // 
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

  const loadRooms = useCallback(async () => {
    setLoadingRooms(true);
    await fetchRooms();
    setLoadingRooms(false);
  }, [fetchRooms]);

  useEffect(() => {
    if (!isManager && !isAdmin) {
      message.error("Bạn không có quyền truy cập trang này");
      navigate("/dashboard");
      return;
    }
    void loadRooms();

    roomsPollingRef.current = setInterval(() => {
      void fetchRooms();
    }, 5000);

    return () => {
      if (roomsPollingRef.current) {
        clearInterval(roomsPollingRef.current);
      }
    };
  }, [isManager, isAdmin, loadRooms, fetchRooms, navigate]);

  useEffect(() => {
    if (selectedRoom?.id) {
      setLoadingMessages(true);
      void fetchMessages(selectedRoom.id).then(() => setLoadingMessages(false));

      pollingRef.current = setInterval(() => {
        if (selectedRoom?.id) {
          void fetchMessages(selectedRoom.id);
        }
      }, 5000);

      internalChatApi.markAsRead(selectedRoom.id).catch(() => undefined);
    }

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
      }
    };
  }, [selectedRoom?.id, fetchMessages]);

  useEffect(() => {
    const el = messagesContainerRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages]);

  const handleSend = async (text?: string) => {
    const content = (text ?? "").trim();
    if (!content || !selectedRoom) {
      return;
    }

    setSending(true);
    try {
      const res = await internalChatApi.sendMessage(selectedRoom.id, { message: content });
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data!]);
        void fetchRooms();
      } else {
        message.error(res.message || "Gửi tin nhắn thất bại");
      }
    } catch {
      message.error("Gửi tin nhắn thất bại");
    } finally {
      setSending(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  };

  const selectRoom = (room: InternalChatRoom) => {
    setSelectedRoom(room);
    setMessages([]);
  };

  const handleUploadImage = async (file: File) => {
    if (!selectedRoom) return;

    setUploadingImage(true);
    try {
      const res = await internalChatApi.uploadImage(selectedRoom.id, file);
      if (res.success && res.data) {
        setMessages((prev) => [...prev, res.data!]);
        void fetchRooms();
      } else {
        message.error(res.message || "Gửi ảnh thất bại");
      }
    } catch {
      message.error("Gửi ảnh thất bại");
    } finally {
      setUploadingImage(false);
    }
  };

  const filteredRooms = useMemo(() => {
    let filtered = rooms;

    if (filterRole !== "ALL") {
      filtered = filtered.filter(
        (r) => r.employeeRole.toUpperCase() === filterRole
      );
    }

    if (searchText.trim()) {
      const search = searchText.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          r.employeeName.toLowerCase().includes(search)
      );
    }

    return [...filtered].sort((a, b) => {
      const aTime = new Date(a.lastMessageAt || a.createdAt).getTime();
      const bTime = new Date(b.lastMessageAt || b.createdAt).getTime();
      return bTime - aTime;
    });
  }, [rooms, filterRole, searchText]);

  const getAvatarColor = (role: string): string => {
    return role.toLowerCase() === "driver" ? DRIVER_COLOR : UTE_BRAND_COLOR;
  };

  const getAvatarStyle = (role: string, hasAvatar: boolean): React.CSSProperties => {
    if (hasAvatar) {
      return {};
    }
    return {
      backgroundColor: getAvatarColor(role),
    };
  };

  return (
    <div className="manager-internal-chat-page">
      {/* Sidebar */}
      <div className="manager-internal-chat-sidebar">
        <div className="manager-internal-chat-sidebar-header">
          <Title level={5} style={{ margin: 0 }}>Trao đổi nhân viên</Title>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {filteredRooms.length} cuộc trò chuyện
          </Text>
        </div>

        {/* Filter & Search */}
        <div className="chat-filter-section">
          <Select
            value={filterRole}
            onChange={(val) => setFilterRole(val)}
            options={filterRoleOptions}
            style={{ width: "100%" }}
            size="small"
          />
          <Input
            placeholder="Tìm kiếm nhân viên..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            size="small"
            prefix={<SearchOutlined style={{ color: "#aaa" }} />}
          />
        </div>

        <Spin spinning={loadingRooms}>
          {filteredRooms.length === 0 ? (
            <div className="manager-internal-chat-empty">
              <Empty description="Chưa có cuộc trò chuyện nào" />
            </div>
          ) : (
            <List
              dataSource={filteredRooms}
              renderItem={(room) => {
                const isActive = selectedRoom?.id === room.id;
                const unread = room.unreadCount || 0;
                const hasAvatar = !!room.employeeAvatar;

                return (
                  <List.Item
                    className={`manager-internal-chat-conversation ${isActive ? "active" : ""}`}
                    onClick={() => selectRoom(room)}
                  >
                    <div className="chat-conversation-item">
                      <div className="chat-conversation-left">
                        <Badge dot={unread > 0} offset={[-4, 4]}>
                          <Avatar
                            size={40}
                            src={room.employeeAvatar || undefined}
                            icon={<UserOutlined />}
                            style={getAvatarStyle(room.employeeRole, hasAvatar)}
                          />
                        </Badge>
                        <div className="chat-conversation-info">
                          <div className="chat-conversation-header">
                            <Text strong style={{ fontSize: 13 }}>
                              {room.employeeName}
                            </Text>
                            <Text type="secondary" style={{ fontSize: 11 }}>
                              {getRoleLabel(room.employeeRole)}
                            </Text>
                          </div>
                          {room.lastMessage && (
                            <Text ellipsis className="chat-conversation-preview" style={{ fontSize: 12 }}>
                              {room.lastMessage}
                            </Text>
                          )}
                        </div>
                      </div>
                      <div className="chat-conversation-right">
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          {room.lastMessageAt ? dayjs(room.lastMessageAt).format("HH:mm") : ""}
                        </Text>
                      </div>
                    </div>
                  </List.Item>
                );
              }}
            />
          )}
        </Spin>
      </div>

      {/* Main Chat Area */}
      <div className="manager-internal-chat-main">
        {!selectedRoom ? (
          <div className="manager-internal-chat-empty-main">
            <Empty description="Chọn một cuộc trò chuyện" />
            <Text type="secondary">Chọn nhân viên từ danh sách bên trái để bắt đầu trò chuyện</Text>
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="manager-internal-chat-header">
              <div className="manager-internal-chat-header-info">
                <Avatar
                  size={40}
                  src={selectedRoom.employeeAvatar || undefined}
                  icon={<UserOutlined />}
                  style={getAvatarStyle(selectedRoom.employeeRole, !!selectedRoom.employeeAvatar)}
                />
                <div>
                  <Title level={5} style={{ margin: 0 }}>
                    {selectedRoom.employeeName}
                  </Title>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {getRoleLabel(selectedRoom.employeeRole)} - {selectedRoom.officeName}
                  </Text>
                </div>
              </div>
            </div>

            {/* Messages */}
            <div ref={messagesContainerRef} className="manager-internal-chat-messages">
              <Spin spinning={loadingMessages}>
                {messages.length === 0 ? (
                  <div className="manager-internal-chat-empty-main">
                    <Empty description="Chưa có tin nhắn nào" />
                  </div>
                ) : (
                  messages.map((msg) => {
                    const isMine = msg.isMine;
                    const hasAvatar = !!msg.senderAvatar;

                    return (
                      <div
                        key={msg.id}
                        className={`manager-internal-chat-bubble-row ${isMine ? "mine" : "their"}`}
                      >
                        {!isMine && (
                          <Avatar
                            size={32}
                            src={msg.senderAvatar || undefined}
                            icon={<UserOutlined />}
                            style={{
                              ...getAvatarStyle(msg.senderRole, hasAvatar),
                              marginRight: 8,
                              flexShrink: 0
                            }}
                          />
                        )}
                        <div className={`manager-internal-chat-bubble ${isMine ? "mine" : "their"}`}>
                          {!isMine && (
                            <div className="manager-internal-chat-sender-name">
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {msg.senderName} ({getRoleLabel(msg.senderRole)})
                              </Text>
                            </div>
                          )}
                          {msg.messageType === "IMAGE" && msg.imageUrl ? (
                            <Image
                              src={msg.imageUrl}
                              alt="Hình ảnh"
                              style={{ maxWidth: "100%", maxHeight: 200, borderRadius: 8, cursor: "pointer" }}
                              preview={{ mask: <PictureOutlined style={{ fontSize: 24 }} /> }}
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
                              ...getAvatarStyle(msg.senderRole, hasAvatar),
                              marginLeft: 8,
                              flexShrink: 0
                            }}
                          />
                        )}
                      </div>
                    );
                  })
                )}
              </Spin>
            </div>

            {/* Input */}
            <div className="manager-internal-chat-input">
              <ChatMessageInput
                onSend={handleSend}
                onUploadImage={handleUploadImage}
                sending={sending}
                placeholder="Nhập tin nhắn..."
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default InternalEmployeeChatPage;
