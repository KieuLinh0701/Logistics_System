import React, { useEffect, useState } from "react";
import {
    Table,
    Typography,
    Button,
    Layout,
    Spin,
    Input,
    Checkbox,
    message,
} from "antd";
import {
    BellOutlined,
    ReloadOutlined,
    SearchOutlined,
    CheckCircleOutlined
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { Notification } from "../../../types/notification";
import notificationApi from "../../../api/notificationApi";
import { getUserRole } from "../../../utils/authUtils";
import "./NotificationList.css";

const { Content } = Layout;
const { Title, Text } = Typography;
const { Search } = Input;

const NotificationList: React.FC = () => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const [unreadCount, setUnreadCount] = useState<number>(0);
    const [searchTerm, setSearchTerm] = useState("");
    const [showUnreadOnly, setShowUnreadOnly] = useState(false);
    const pageSize = 10;
    const navigate = useNavigate();
    const role = getUserRole();

    const fetchNotifications = async (
        pageNumber = 1,
        keyword = "",
        unreadOnly = false
    ) => {
        try {
            setLoading(true);
            const response = await notificationApi.getNotifications({
                page: pageNumber,
                limit: pageSize,
                search: keyword,
                isRead: unreadOnly ? false : undefined,
            });
            if (response.success && response.data) {
                setNotifications(response.data.notifications);
                setTotal(response.data.pagination?.total || response.data.notifications.length);
                setPage(pageNumber);
                setUnreadCount(response.data.unreadCount);
            }
        } catch (error) {
            console.error("Error fetching notifications:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = (value: string) => {
        setSearchTerm(value);
        fetchNotifications(1, value, showUnreadOnly);
    };

    const handleToggleUnread = (e: any) => {
        const checked = e.target.checked;
        setShowUnreadOnly(checked);
        fetchNotifications(1, searchTerm, checked);
    };

    const handleClick = async (notification: Notification) => {
        if (!notification.isRead) {
            await notificationApi.markAsRead(notification.id);

            // Cập nhật trạng thái thông báo cục bộ
            const updatedNotifications = notifications.map(n =>
                n.id === notification.id ? { ...n, isRead: true } : n
            );
            setNotifications(updatedNotifications);

            // Đồng bộ sang Header
            window.dispatchEvent(new CustomEvent('updateNotifications', { detail: updatedNotifications }));
        }
        navigate(`/notifications/${notification.id}`, { state: { notification } });
    };

    // THÊM HÀM ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC
    const handleMarkAllAsRead = async () => {
        try {
            setLoading(true);
            await notificationApi.markAllAsRead();

            // Cập nhật tất cả cục bộ
            const updatedNotifications = notifications.map(n => ({ ...n, isRead: true }));
            setNotifications(updatedNotifications);

            // Đồng bộ sang Header
            window.dispatchEvent(new CustomEvent('updateNotifications', { detail: updatedNotifications }));

            message.success("Đã đánh dấu tất cả thông báo là đã đọc");
        } catch (error) {
            message.error("Đánh dấu tất cả đã đọc thất bại");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    const baseColumns = [
        {
            title: "Tiêu đề",
            dataIndex: "title",
            key: "title",
            width: "20%",
            render: (title: string, record: Notification) => (
                <div className="notification-title">
                    <Text strong={!record.isRead} className={record.isRead ? "read-text" : "unread-text"} ellipsis>
                        {title}
                    </Text>
                </div>
            ),
            ellipsis: true,
        },
        {
            title: "Nội dung",
            dataIndex: "message",
            key: "message",
            width: "45%",
            render: (message: string, record: Notification) => (
                <Text className={record.isRead ? "read-text" : "unread-text"} ellipsis>
                    {message}
                </Text>
            ),
            ellipsis: true,
        },
        {
            title: "Người gửi",
            dataIndex: "creatorName",
            key: "creatorName",
            width: "20%",
            render: (creatorName: string, record: Notification) => (
                <Text className={record.isRead ? "read-text" : "unread-text"} ellipsis>
                    {creatorName ? creatorName : "Hệ thống"}
                </Text>
            ),
            ellipsis: true,
        },
        {
            title: "Thời gian",
            dataIndex: "createdAt",
            key: "createdAt",
            width: "15%",
            render: (createdAt: string, record: Notification) => (
                <Text className={record.isRead ? "read-text" : "unread-text"}>
                    {new Date(createdAt).toLocaleString("vi-VN")}
                </Text>
            ),
        },
    ];

    const columns = role === "user"
        ? baseColumns.filter(col => col.key !== "creatorName")
        : baseColumns;

    return (
        <Layout className="notification-layout">
            <Content className="notification-content">
                <div className="notification-header">
                    <Title level={3} className="notification-title-main">
                        <BellOutlined className="title-icon" />
                        Danh sách thông báo
                    </Title>
                    <Text className="notification-subtitle">
                        Quản lý và theo dõi tất cả thông báo của bạn
                    </Text>
                </div>

                <div className="notification-actions" style={{ display: "flex", gap: 12, marginBottom: 12 }}>
                    <Search
                        placeholder="Tìm kiếm theo tiêu đề hoặc nội dung..."
                        allowClear
                        value={searchTerm}
                        onSearch={handleSearch}
                        onChange={(e) => handleSearch(e.target.value)}
                        size="large"
                        className="notification-search"
                        prefix={<SearchOutlined />}
                        style={{ flex: 1 }}
                    />
                    <Checkbox checked={showUnreadOnly} onChange={handleToggleUnread}>
                        Chỉ hiển thị chưa đọc
                    </Checkbox>

                    {/* THÊM NÚT ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC */}
                    <Button
                        onClick={handleMarkAllAsRead}
                        size="large"
                        className="mark-all-read-button"
                        icon={<CheckCircleOutlined />}
                        disabled={unreadCount === 0}
                    >
                        Đánh dấu tất cả đã đọc
                    </Button>

                    <Button
                        onClick={() => {
                            setSearchTerm("");
                            setShowUnreadOnly(false);
                            fetchNotifications(1, "", false);
                        }}
                        size="large"
                        className="refresh-button"
                        icon={<ReloadOutlined />}
                    >
                        Làm mới
                    </Button>
                </div>

                {loading ? (
                    <div className="loading-container">
                        <Spin size="large" />
                        <Text className="loading-text">Đang tải thông báo...</Text>
                    </div>
                ) : notifications.length === 0 ? (
                    <div className="empty-container">
                        <BellOutlined className="empty-icon" />
                        <Text className="empty-text">Không có thông báo nào</Text>
                        <Text type="secondary" className="empty-subtext">
                            {searchTerm ? "Thử tìm kiếm với từ khóa khác" : "Tất cả thông báo đã được xem"}
                        </Text>
                    </div>
                ) : (
                    <>
                        <span className="notification-tag">
                            {`Kết quả trả về: ${total} thông báo`} &nbsp;|&nbsp;
                            {`Chưa đọc: ${unreadCount}`}
                        </span>

                        <div className="table-container">
                            <Table
                                dataSource={notifications}
                                columns={columns}
                                rowKey="id"
                                pagination={{
                                    current: page,
                                    pageSize,
                                    total,
                                    onChange: (pageNumber) => fetchNotifications(pageNumber, searchTerm, showUnreadOnly),
                                    showSizeChanger: false,
                                    showQuickJumper: true,
                                }}
                                rowClassName={(record) => (record.isRead ? "read-row" : "unread-row")}
                                onRow={(record) => ({
                                    onClick: () => handleClick(record),
                                    style: { cursor: "pointer" },
                                })}
                                tableLayout="fixed"
                                size="middle"
                                className="notification-table"
                            />
                        </div>
                    </>
                )}
            </Content>
        </Layout>
    );
};

export default NotificationList;