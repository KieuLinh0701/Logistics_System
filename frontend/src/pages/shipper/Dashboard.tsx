import React, { useState, useEffect } from "react";
import { Card, Row, Col, Statistic, Table, Tag, Button, Space, Typography, List, Badge, message } from "antd";
import {
  TruckOutlined,
  BoxPlotOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  BellOutlined,
  EnvironmentOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";
import type { ShipperOrder, ShipperStats } from "../../api/orderApi";
import { connectWebSocket, disconnectWebSocket } from "../../socket/socket";
import { getUserId } from "../../utils/authUtils";


const { Title, Text } = Typography;

interface NotificationItem {
  id: number;
  type: "urgent" | "route_change" | "new_order" | "system";
  title: string;
  message: string;
  time: string;
  read: boolean;
}

const ShipperDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<ShipperStats>({
    totalAssigned: 0,
    inProgress: 0,
    delivered: 0,
    failed: 0,
    codCollected: 0,
  });

  const [todayOrders, setTodayOrders] = useState<ShipperOrder[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();

    // Kết nối WebSocket để nhận thông báo real-time
    const userId = getUserId();
    if (userId) {
      connectWebSocket(userId, (notification) => {
        console.log("Received notification:", notification);
        message.info({
          content: (
            <div>
              <strong>{notification.title}</strong>
              <br />
              {notification.message}
            </div>
          ),
          duration: 5,
        });
        // Refresh dashboard data khi có thông báo mới
        fetchDashboardData();
      });
    }

    return () => {
      disconnectWebSocket();
    };
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const res = await orderApi.getShipperDashboard();
      setStats(res.stats);
      const today = Array.isArray((res as any).todayOrders) ? (res as any).todayOrders : [];
      setTodayOrders(today as ShipperOrder[]);
      setNotifications((res as any).notifications || []);
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
      message.error("Lỗi khi tải dashboard");
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "default";
      case "CONFIRMED":
      case "AT_DEST_OFFICE":
        return "blue";
      case "PICKED_UP":
        return "orange";
      case "DELIVERING":
        return "processing";
      case "DELIVERED":
        return "success";
      case "FAILED_DELIVERY":
      case "RETURNED":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "PENDING":
        return "Chờ xử lý";
      case "CONFIRMED":
        return "Đã xác nhận";
      case "AT_DEST_OFFICE":
        return "Đã đến bưu cục";
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "DELIVERING":
        return "Đang giao hàng";
      case "DELIVERED":
        return "Đã giao";
      case "FAILED_DELIVERY":
        return "Giao thất bại";
      case "RETURNED":
        return "Đã hoàn";
      default:
        return status;
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case "urgent":
        return <ExclamationCircleOutlined style={{ color: "#ff4d4f" }} />;
      case "route_change":
        return <EnvironmentOutlined style={{ color: "#1890ff" }} />;
      case "new_order":
        return <BoxPlotOutlined style={{ color: "#52c41a" }} />;
      default:
        return <BellOutlined style={{ color: "#faad14" }} />;
    }
  };

  const orderColumns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string, record: ShipperOrder) => (
        <Space direction="vertical" size={0}>
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: "Người nhận",
      key: "recipient",
      render: (record: ShipperOrder) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.recipientName}</Text>
          <Text type="secondary" style={{ fontSize: "12px" }}>
            {record.recipientPhone}
          </Text>
        </Space>
      ),
    },
    {
      title: "Địa chỉ",
      key: "recipientAddress",
      ellipsis: true,
      render: (record: ShipperOrder) =>
        typeof record.recipientAddress === "string"
          ? record.recipientAddress
          : (record.recipientAddress as any)?.fullAddress ?? "",
    },
    {
      title: "COD",
      dataIndex: "cod",
      key: "cod",
      render: (amount: number, record: ShipperOrder & any) => (
        <div>
          <Text strong style={{ color: amount > 0 ? "#52c41a" : "#8c8c8c" }}>
            {amount > 0 ? `${amount.toLocaleString()}đ` : "Không"}
          </Text>
          {record.codStatus && (
            <div style={{ marginTop: 4 }}>
              <Tag color={record.codStatus === "PENDING" ? "orange" : record.codStatus === "SUBMITTED" ? "blue" : record.codStatus === "RECEIVED" || record.codStatus === "TRANSFERRED" ? "green" : "default"}>
                {record.codStatus}
              </Tag>
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>,
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: ShipperOrder) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/shipper/orders/${record.id}`)}>
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>
          Dashboard Shipper
        </Title>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }} loading={loading}>
            <Statistic
              title="Tổng đơn được phân công"
              value={stats.totalAssigned}
              prefix={<BoxPlotOutlined />}
              valueStyle={{ color: "#1890ff" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }} loading={loading}>
            <Statistic
              title="Đang giao"
              value={stats.inProgress}
              prefix={<TruckOutlined />}
              valueStyle={{ color: "#faad14" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }} loading={loading}>
            <Statistic
              title="Đã giao thành công"
              value={stats.delivered}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: "#52c41a" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }} loading={loading}>
            <Statistic
              title="COD đã thu"
              value={stats.codCollected}
              prefix={<DollarOutlined />}
              formatter={(value) => `${(value as number)?.toLocaleString()}đ`}
              valueStyle={{ color: "#52c41a" }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card
            title="Đơn hàng trong ngày"
            extra={
              <Button type="link" onClick={() => navigate("/shipper/orders")}>
                Xem tất cả
              </Button>
            }
            style={{ borderRadius: 12 }}
            loading={loading}
          >
            <Table
              rowKey="id"
              dataSource={todayOrders}
              columns={orderColumns}
              pagination={false}
              locale={{ emptyText: "Không có đơn hàng nào hôm nay" }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card
            title={
              <Space>
                <BellOutlined />
                <span>Thông báo gần đây</span>
              </Space>
            }
            style={{ borderRadius: 12 }}
          >
            <List
              dataSource={notifications}
              locale={{ emptyText: "Không có thông báo" }}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <Badge dot={!item.read}>
                        <span>{getNotificationIcon(item.type)}</span>
                      </Badge>
                    }
                    title={<Text strong>{item.title}</Text>}
                    description={
                      <>
                        <Text>{item.message}</Text>
                        <br />
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {item.time}
                        </Text>
                      </>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ShipperDashboard;
