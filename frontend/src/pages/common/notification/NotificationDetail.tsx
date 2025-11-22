import React from "react";
import { Layout, Typography, Button, Card } from "antd";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { BellOutlined } from "@ant-design/icons";
import "./NotificationDetail.css";

const { Content } = Layout;
const { Title, Text } = Typography;

const NotificationDetail: React.FC = () => {
    const { state } = useLocation() as any;
    const { id } = useParams();
    const navigate = useNavigate();
    const notification = state?.notification;
    const user = JSON.parse(localStorage.getItem("user") || "{}");

    const formatTime = (createdAt: string) => {
        const now = new Date();
        const notificationTime = new Date(createdAt);
        const diffInMinutes = Math.floor((now.getTime() - notificationTime.getTime()) / (1000 * 60));
        if (diffInMinutes < 1) return "Vừa xong";
        if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
        const diffInHours = Math.floor(diffInMinutes / 60);
        if (diffInHours < 24) return `${diffInHours} giờ trước`;
        const diffInDays = Math.floor(diffInHours / 24);
        return `${diffInDays} ngày trước`;
    };

    const getNotificationLink = (type?: string) => {
        switch (type) {
            case "new_order":
                return `/${user.role}/orders`;
            case "delivery_assigned":
                return `/${user.role}/deliveries`;
            case "cod_reminder":
                return `/${user.role}/payments`;
            case "ShippingRequest":
                return `/${user.role}/orders/requests`;
            case "order":
                return `/${user.role}/orders`;
            default:
                return null;
        }
    };

    if (!notification) {
        return (
            <Layout className="notification-detail-layout">
                <Content className="notification-detail-content">
                    <div className="notification-not-found">
                        <BellOutlined className="not-found-icon" />
                        <p>Không tìm thấy thông báo (ID: {id})</p>
                        <Button type="primary" onClick={() => navigate(-1)}>
                            Quay lại
                        </Button>
                    </div>
                </Content>
            </Layout>
        );
    }

    return (
        <Layout className="notification-detail-layout">
            <Content className="notification-detail-content">
                <div className="notification-detail-header">
                    <Title level={3} className="notification-detail-title-main">
                        <BellOutlined className="title-icon" />
                        Danh sách thông báo
                    </Title>
                    <Text className="notification-detail-subtitle">
                        Quản lý và theo dõi tất cả thông báo của bạn
                    </Text>
                </div>

                <Card className="notification-detail-card">
                    <Title level={4} className="notification-detail-card-title">
                        {notification.title}
                    </Title>
                    <Text className="notification-time">
                        {formatTime(notification.createdAt)}
                    </Text>

                    <div className="notification-message">
                        {notification.message}
                    </div>

                    {/* Link điều hướng */}
                    {getNotificationLink(notification?.type) && (
                        <div className="notification-link-container">
                            <a
                                onClick={() => {
                                    navigate(getNotificationLink(notification?.type)!);
                                }}
                                className="notification-link"
                            >
                                Xem chi tiết →
                            </a>
                        </div>
                    )}

                    <Button onClick={() => navigate(-1)} className="back-button">
                        Quay lại
                    </Button>
                </Card>
            </Content>
        </Layout>
    );
};

export default NotificationDetail;