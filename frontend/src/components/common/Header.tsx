import React, { useState, useEffect } from "react";
import {
  Layout,
  Button,
  Space,
  Typography,
  Avatar,
  Dropdown,
  Badge,
  List,
  Divider,
  Modal
} from "antd";
import {
  UserOutlined,
  LogoutOutlined,
  ProfileOutlined,
  BellOutlined,
  ShoppingCartOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  BellFilled,
  PlayCircleOutlined
} from "@ant-design/icons";
import { Link, useNavigate } from "react-router-dom";
import authApi from "../../api/authApi";
import { getCurrentUser, getUserRole } from "../../utils/authUtils";
import notificationApi from "../../api/notificationApi";
import type { Notification } from "../../types/notification";
import { connectWebSocket, disconnectWebSocket } from "../../socket/socket";
import "./Header.css";
import logo from "../../assets/images/home/logo_white.png";
import { getFullName, type User } from "../../types/auth";
import { translateRoleNameHeader } from "../../utils/roleUtils";

const { Header: AntHeader } = Layout;
const { Text, Title } = Typography;

const Header: React.FC = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(getCurrentUser());
  const [unreadCount, setUnreadCount] = useState<number>(0);

  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState<Notification | null>(null);

  const avatarSrc = currentUser?.images || undefined;

  const role = getUserRole();

  useEffect(() => {
    const handleUserUpdate = (e: any) => {
      setCurrentUser(e.detail);
    };

    window.addEventListener('userUpdated', handleUserUpdate);

    return () => {
      window.removeEventListener('userUpdated', handleUserUpdate);
    };
  }, []);

  useEffect(() => {
    const handleNotificationUpdate = (e: any) => {
      setNotifications(e.detail);
    };

    window.addEventListener('updateNotifications', handleNotificationUpdate);

    return () => {
      window.removeEventListener('updateNotifications', handleNotificationUpdate);
    };
  }, []);

  const fetchNotifications = async () => {
    try {
      const res = await notificationApi.getNotifications({ page: 1, limit: 7 });
      if (res.success && res.data) {
        setNotifications(res.data.notifications);
        setUnreadCount(res.data.unreadCount);
      }
    } catch (error) {
      console.error("Failed to fetch notifications:", error);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    if (!notification.isRead) {
      await notificationApi.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notification.id ? { ...n, isRead: true } : n))
      );
    }
    setSelectedNotification(notification);
    setIsModalOpen(true);
  };

  const formatTime = (createdAt: string) => {
    const now = new Date();
    const notifTime = new Date(createdAt);
    const diffMinutes = Math.floor((now.getTime() - notifTime.getTime()) / 60000);

    if (diffMinutes < 1) return "Vừa xong";
    if (diffMinutes < 60) return `${diffMinutes} phút trước`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours} giờ trước`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} ngày trước`;
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case "new_order":
        return <ShoppingCartOutlined style={{ color: "#1890ff" }} />;
      case "delivery_assigned":
        return <ClockCircleOutlined style={{ color: "#52c41a" }} />;
      case "delivery_started":
        return <PlayCircleOutlined style={{ color: "#1890ff" }} />;
      case "route_change":
        return <ExclamationCircleOutlined style={{ color: "#faad14" }} />;
      case "cod_reminder":
        return <BellOutlined style={{ color: "#f5222d" }} />;
      default:
        return <BellFilled style={{ color: "#1C3D90" }} />;
    }
  };

  const getNotificationLink = (type?: string) => {
    switch (type) {
      case "new_order":
        return `/orders/list`;
      case "delivery_assigned":
        return `/deliveries`;
      case "cod_reminder":
        return `/payments`;
      case "ShippingRequest":
        return `/orders/requests`;
      case "order":
        return `/orders/list`;
      default:
        return null;
    }
  };

  useEffect(() => {
    if (!currentUser?.id) return;

    fetchNotifications();

    const handleRealtimeNotification = (payload: Notification) => {
      setNotifications((prev) => [payload, ...prev].slice(0, 7));
      setUnreadCount((prev) => prev + 1);
    };

    connectWebSocket(currentUser.id, handleRealtimeNotification);

    return () => {
      disconnectWebSocket();
    };
  }, [currentUser?.id]);

  const profileMenuItems = [
    {
      key: "account/settings",
      label: "Cài đặt tài khoản",
      icon: <ProfileOutlined />,
      onClick: () => navigate(`/account/settings?tab=profile`)
    },
    {
      key: "logout",
      label: "Logout",
      icon: <LogoutOutlined />,
      onClick: () => { authApi.logout(); navigate("/login"); }
    }
  ];

  const notificationMenuItems = [
    {
      key: "notifications",
      label: (
        <div className="notification-dropdown">
          <div className="notification-header">
            <Text strong className="notification-title">Thông báo</Text>
            {unreadCount > 0 && <Badge count={unreadCount} color="#d32029" />}
          </div>
          <Divider className="notification-divider" />
          {notifications.length === 0 ? (
            <div className="notification-empty">
              <BellOutlined className="empty-icon" />
              <div>Chưa có thông báo nào</div>
            </div>
          ) : (
            <List
              dataSource={notifications}
              renderItem={(notification) => (
                <List.Item
                  className={`notification-item ${notification.isRead ? '' : 'unread'}`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <List.Item.Meta
                    avatar={getNotificationIcon(notification.type)}
                    title={
                      <div className="notification-item-title">
                        <Text strong={!notification.isRead} ellipsis>{notification.title}</Text>
                        <Text type="secondary" className="notification-time">{formatTime(notification.createdAt)}</Text>
                      </div>
                    }
                    description={<Text type="secondary" ellipsis>{notification.message}</Text>}
                  />
                </List.Item>
              )}
            />
          )}
          <Divider className="notification-divider-bottom" />
          <div className="notification-footer">
            <Button type="text" onClick={() => navigate(`/notifications`)}>Xem thêm</Button>
          </div>
        </div>
      )
    }
  ];

  return (
    <>
      <AntHeader className="main-header">
        <Link to="/home" className="header-logo-link">
          <div className="logo-container">
            <img src={logo} alt="UTE Logistics" className="header-logo-image" />
            <Title level={3} className="header-logo-text">
              UTE Logistics
            </Title>
          </div>
        </Link>
        <Space size="middle" className="header-actions">
          <Dropdown
            menu={{ items: notificationMenuItems }}
            placement="bottomRight"
            trigger={["click"]}
            overlayClassName="notification-dropdown-wrapper"
          >
            <Badge count={unreadCount} offset={[-10, 8]}>
              <Button type="text" className="notification-button" icon={<BellOutlined />} />
            </Badge>
          </Dropdown>

          <Dropdown menu={{ items: profileMenuItems }} placement="bottomRight" trigger={["click"]}>
            <Space className="user-profile">
              <Avatar src={avatarSrc} icon={<UserOutlined />} className="user-avatar" />
              <Text className="user-name">{getFullName(currentUser!)}</Text>
              <Text className="role-name">({translateRoleNameHeader(role!)})</Text>
            </Space>
          </Dropdown>
        </Space>
      </AntHeader>

      <Modal
        title={selectedNotification?.title || "Chi tiết thông báo"}
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setIsModalOpen(false)}>Đóng</Button>
        ]}
        className="notification-modal"
      >
        <p>{selectedNotification?.message}</p>
        {getNotificationLink(selectedNotification?.type) && (
          <div className="notification-modal-link">
            <a onClick={() => { setIsModalOpen(false); navigate(getNotificationLink(selectedNotification?.type)!); }}>Xem chi tiết →</a>
          </div>
        )}
      </Modal>
    </>
  );
};

export default Header;