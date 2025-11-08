import React, { useState, useEffect } from "react";
import { Layout, Button, Space, Typography, Avatar, Dropdown, Menu, Badge, List, Divider, Modal } from "antd";
import { UserOutlined, LogoutOutlined, ProfileOutlined, PlusOutlined, BellOutlined, ShoppingCartOutlined, ClockCircleOutlined, ExclamationCircleOutlined, BellFilled, PlayCircleOutlined } from "@ant-design/icons";
import { Link, useNavigate } from "react-router-dom"; import authApi from "../../api/authApi";
import { getCurrentUser, getUserRole } from "../../utils/authUtils";

// import { getSocket } from "../../services/socket";
// import { notificationService, NotificationItem } from "../../services/notificationService";

const { Header: AntHeader } = Layout;
const { Text } = Typography;

interface HeaderProps {
}

const Header: React.FC<HeaderProps> = () => {
  const navigate = useNavigate();
  // const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);

  const role = getUserRole();
  const user = getCurrentUser();

  const fileName = user?.images ? String(user.images).split('/').pop() : undefined;
  const avatarSrc = fileName ? `/uploads/${fileName}` : undefined;

  const [isModalOpen, setIsModalOpen] = useState(false);
  // const [selectedNotification, setSelectedNotification] = useState<NotificationItem | null>(null);

  const handleLogout = () => {
    authApi.logout();
    navigate("/login");
  };

  const handleProfile = () => {
    if (role) {
      navigate(`/${role}/profile`);
    } else {
      navigate("/profile");
    }
  };

  // Fetch notifications từ API
  // const fetchNotifications = async () => {
  //   // if (user?.role !== 'shipper') return;

  //   try {
  //     console.log('Fetching notifications from API...');
  //     setLoading(true);
  //     const response = await notificationService.getNotifications({
  //       page: 1,
  //       limit: 10
  //     });

  //     console.log('API response:', response);

  //     if (response.success && response.data) {
  //       console.log('Notifications loaded:', response.data.notifications.length);
  //       setNotifications(response.data.notifications);
  //     } else {
  //       console.log('Failed to load notifications:', response.message);
  //     }
  //   } catch (error) {
  //     console.error('Error fetching notifications:', error);
  //   } finally {
  //     setLoading(false);
  //   }
  // };

  // Subscribe to WebSocket notifications
  // useEffect(() => {
  //   console.log('Setting up notifications for user:', user.id);

  //   // Fetch initial notifications from database
  //   fetchNotifications();

  //   // Connect to WebSocket
  //   const socket = getSocket();
  //   console.log('Socket connected:', socket.connected);

  //   // Re-register user with WebSocket when user changes
  //   if (user?.id) {
  //     console.log('Re-registering user with WebSocket:', user.id);
  //     socket.emit('register', user.id);
  //   }

  //   const onServerNotification = (payload: any) => {
  //     console.log('Received WebSocket notification:', payload);

  //     // Add new notification to the list (notification đã được lưu vào DB từ backend)
  //     const newNotification: NotificationItem = {
  //       id: payload.id || Date.now(),
  //       title: payload.title || 'Thông báo',
  //       message: payload.message || '',
  //       type: payload.type || 'system',
  //       isRead: payload.isRead || false,
  //       userId: user.id,
  //       createdAt: payload.createdAt || new Date().toISOString(),
  //       updatedAt: new Date().toISOString(),
  //     };

  //     console.log('Adding notification to state:', newNotification);

  //     // Thêm vào đầu danh sách và giới hạn 20 thông báo
  //     setNotifications(prev => [newNotification, ...prev.slice(0, 19)]);
  //   };

  //   socket.on('notification', onServerNotification);
  //   console.log('Listening for notification events');

  //   return () => {
  //     console.log('Cleaning up notification listeners');
  //     socket.off('notification', onServerNotification);
  //   };
  // }, [user?.role, user?.id]);

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'new_order':
        return <ShoppingCartOutlined style={{ color: '#1890ff' }} />;
      case 'delivery_assigned':
        return <ClockCircleOutlined style={{ color: '#52c41a' }} />;
      case 'delivery_started':
        return <PlayCircleOutlined style={{ color: '#1890ff' }} />;
      case 'route_change':
        return <ExclamationCircleOutlined style={{ color: '#faad14' }} />;
      case 'cod_reminder':
        return <BellOutlined style={{ color: '#f5222d' }} />;
      default:
        return <BellFilled style={{ color: '#1C3D90' }} />;
    }
  };

  // const handleNotificationClick = async (notification: NotificationItem) => {
  //   try {
  //     if (!notification.isRead) {
  //       await notificationService.markAsRead(notification.id);
  //       setNotifications(prev =>
  //         prev.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
  //       );
  //     }

  //     setSelectedNotification(notification);
  //     setIsModalOpen(true);
  //   } catch (error) {
  //     console.error('Error handling notification click:', error);
  //   }
  // };

  // const unreadCount = notifications.filter(n => !n.isRead).length;

  // Format thời gian từ createdAt
  // const formatTime = (createdAt: string) => {
  //   const now = new Date();
  //   const notificationTime = new Date(createdAt);
  //   const diffInMinutes = Math.floor((now.getTime() - notificationTime.getTime()) / (1000 * 60));

  //   if (diffInMinutes < 1) return 'Vừa xong';
  //   if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;

  //   const diffInHours = Math.floor(diffInMinutes / 60);
  //   if (diffInHours < 24) return `${diffInHours} giờ trước`;

  //   const diffInDays = Math.floor(diffInHours / 24);
  //   return `${diffInDays} ngày trước`;
  // };

  const menu = (
    <Menu>
      <Menu.Item key="profile" icon={<ProfileOutlined />} onClick={handleProfile}>
        Profile
      </Menu.Item>
      <Menu.Item key="logout" icon={<LogoutOutlined />} onClick={handleLogout}>
        Logout
      </Menu.Item>
    </Menu>
  );

  // Menu dropdown cho thông báo
  // const notificationMenu = (
  //   <div
  //     style={{
  //       width: 400,
  //       maxHeight: 500,
  //       overflowY: 'auto',
  //       backgroundColor: '#fff',
  //       borderRadius: 12,
  //       boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)',
  //     }}
  //   >
  //     {/* Header */}
  //     <div
  //       style={{
  //         padding: '14px 18px',
  //         borderBottom: '1px solid #f0f0f0',
  //         display: 'flex',
  //         alignItems: 'center',
  //         justifyContent: 'space-between',
  //       }}
  //     >
  //       <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
  //         <Text strong style={{ fontSize: 16 }}>
  //           Thông báo
  //         </Text>
  //         {unreadCount > 0 && (
  //           <Badge
  //             count={unreadCount}
  //             color="#d32029"
  //             style={{
  //               boxShadow: '0 0 0 2px #fff',
  //               fontSize: 13,
  //             }}
  //           />
  //         )}
  //       </div>
  //     </div>

  //     {/* Content */}
  //     {notifications.length === 0 ? (
  //       <div
  //         style={{
  //           padding: '32px 20px',
  //           textAlign: 'center',
  //           color: '#999',
  //         }}
  //       >
  //         <BellOutlined style={{ fontSize: 28, marginBottom: 8, color: '#d9d9d9' }} />
  //         <div>Chưa có thông báo nào</div>
  //       </div>
  //     ) : (
  //       <List
  //         dataSource={notifications}
  //         renderItem={(notification) => (
  //           <List.Item
  //             style={{
  //               padding: '14px 18px',
  //               cursor: 'pointer',
  //               backgroundColor: notification.isRead ? '#fff' : '#f6ffed',
  //               transition: 'background-color 0.2s ease',
  //             }}
  //             onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = notification.isRead ? '#fafafa' : '#e9fbe5')}
  //             onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = notification.isRead ? '#fff' : '#f6ffed')}
  //             onClick={() => handleNotificationClick(notification)}
  //           >
  //             <List.Item.Meta
  //               avatar={getNotificationIcon(notification.type)}
  //               title={
  //                 <div
  //                   style={{
  //                     display: 'flex',
  //                     justifyContent: 'space-between',
  //                     alignItems: 'center',
  //                     gap: 8,
  //                   }}
  //                 >
  //                   <Text
  //                     strong={!notification.isRead}
  //                     style={{
  //                       flex: 1,
  //                       whiteSpace: 'nowrap',
  //                       overflow: 'hidden',
  //                       textOverflow: 'ellipsis',
  //                     }}
  //                     title={notification.title}
  //                   >
  //                     {notification.title}
  //                   </Text>

  //                   <Text
  //                     type="secondary"
  //                     style={{
  //                       fontSize: 12,
  //                       flexShrink: 0,
  //                       whiteSpace: 'nowrap',
  //                       marginLeft: 8,
  //                     }}
  //                   >
  //                     {formatTime(notification.createdAt)}
  //                   </Text>
  //                 </div>
  //               }
  //               description={
  //                 <Text
  //                   type="secondary"
  //                   style={{
  //                     fontSize: 13,
  //                     lineHeight: '1.4',
  //                     display: '-webkit-box',
  //                     WebkitLineClamp: 1,
  //                     WebkitBoxOrient: 'vertical',
  //                     overflow: 'hidden',
  //                     textOverflow: 'ellipsis',
  //                     maxHeight: '1.4em',
  //                   }}
  //                 >
  //                   {notification.message}
  //                 </Text>
  //               }
  //             />
  //           </List.Item>
  //         )}
  //       />
  //     )}

  //     {/* Footer */}
  //     <Divider style={{ margin: '0 0 4px 0' }} />
  //     <div style={{ padding: '10px 16px', textAlign: 'center' }}>
  //       <Button
  //         type="text"
  //         size="small"
  //         onClick={() => navigate(`/${user.role}/notifications`)}
  //         style={{
  //           fontWeight: 500,
  //           color: '#1C3D90',
  //           transition: 'color 0.2s ease',
  //         }}
  //       >
  //         Xem thêm
  //       </Button>
  //     </div>
  //   </div>
  // );

  const getNotificationLink = (type?: string) => {
    switch (type) {
      case "new_order":
        return `/${role}/orders`;
      case "delivery_assigned":
        return `/${role}/deliveries`;
      case "cod_reminder":
        return `/${role}/payments`;
      case "ShippingRequest":
        return `/${role}/orders/requests`;
      case "order":
        return `/${role}/orders`;
      default:
        return null;
    }
  };

  return (
    <>
      <AntHeader
        style={{
          background: "#1C3D90",
          padding: "0 24px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
        }}
      >
        {/* Góc trái - Tên website */}
        <Link to="/home">
          <Text strong style={{ fontSize: "18px", color: "#fff" }}>
            UTELogistics
          </Text>
        </Link>
        {/* Góc phải */}
        <Space size="middle">
          {/* <Dropdown
            overlay={notificationMenu}
            placement="bottomRight"
            trigger={["click"]}
            getPopupContainer={(trigger) => trigger.parentElement!}
            overlayStyle={{
              marginTop: 0,
              right: 30,
            }}
          >
            <Badge count={unreadCount} size="small" offset={[0, 2]}>
              <Button
                type="text"
                icon={<BellOutlined />}
                style={{ color: "#fff", fontSize: 18 }}
              />
            </Badge>
          </Dropdown> */}

          <Dropdown overlay={menu} placement="bottomRight" trigger={["click"]}>
            <Space style={{ cursor: "pointer", color: "#fff" }}>
              <Avatar src={avatarSrc} icon={<UserOutlined />}
                style={{
                  background: "linear-gradient(135deg, #3a7bd5, #00d2ff)",
                  color: "#fff",
                  fontWeight: "bold",
                }} />
              <Text style={{ color: "#fff" }}>
                {user?.fullName}
              </Text>
            </Space>
          </Dropdown>
        </Space>
      </AntHeader>

      {/* 
<Modal
  centered
  title={
    <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 2 }}>
      <span style={{ color: "#1C3D90", fontWeight: 600, fontSize: 18 }}>
        {selectedNotification?.title || "Chi tiết thông báo"}
      </span>
      <span style={{ fontStyle: "italic", color: "#999", fontSize: 13, marginTop: 2 }}>
        {formatTime(selectedNotification?.createdAt || "")}
      </span>
    </div>
  }
  open={isModalOpen}
  onCancel={() => setIsModalOpen(false)}
  footer={[
    <Button
      key="close"
      style={{ borderColor: "#1C3D90", color: "#1C3D90" }}
      onClick={() => setIsModalOpen(false)}
    >
      Đóng
    </Button>,
  ]}
>
  <div style={{ marginTop: 10, marginBottom: 20 }}>
    <p style={{ fontSize: 15, lineHeight: 1.7, color: "#333", marginBottom: 0 }}>
      {selectedNotification?.message}
    </p>
  </div>
  {getNotificationLink(selectedNotification?.type) && (
    <div style={{ textAlign: "right", marginTop: 12 }}>
      <a
        onClick={() => {
          setIsModalOpen(false);
          navigate(getNotificationLink(selectedNotification?.type)!);
        }}
        style={{ color: "#1C3D90", fontWeight: 500, cursor: "pointer", textDecoration: "none", fontSize: 15 }}
        onMouseEnter={(e) => (e.currentTarget.style.textDecoration = "underline")}
        onMouseLeave={(e) => (e.currentTarget.style.textDecoration = "none")}
      >
        Xem chi tiết →
      </a>
    </div>
  )}
</Modal>
*/}
    </>
  );
};

export default Header;