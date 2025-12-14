import React, { useState, useEffect } from "react";
import {
  Card,
  Row,
  Col,
  Typography,
  Button,
  Space,
  List,
  Tag,
  Statistic,
  message,
  Spin,
  Alert,
} from "antd";
import {
  EnvironmentOutlined,
  TruckOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import type { RouteInfo, DeliveryStop } from "../../api/shipmentApi";

const { Title, Text } = Typography;

const DriverRoute: React.FC = () => {
  const [routeInfo, setRouteInfo] = useState<RouteInfo | null>(null);
  const [deliveryStops, setDeliveryStops] = useState<DeliveryStop[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchRouteData();
  }, []);

  const fetchRouteData = async () => {
    try {
      setLoading(true);
      const routeData = await shipmentApi.getDriverRoute();
      setRouteInfo(routeData.routeInfo);
      setDeliveryStops(routeData.deliveryStops || []);
    } catch (error) {
      console.error("Error fetching route data:", error);
      message.error("Lỗi khi tải dữ liệu lộ trình");
    } finally {
      setLoading(false);
    }
  };

  const handleNavigateToStop = (stop: DeliveryStop) => {
    if (stop.officeAddress) {
      const address = encodeURIComponent(stop.officeAddress);
      const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${address}`;
      window.open(mapsUrl, "_blank");
      message.success(`Đã mở bản đồ đến ${stop.officeName}`);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return "default";
      case "in_progress":
        return "processing";
      case "completed":
        return "success";
      default:
        return "default";
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!routeInfo) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          message="Không có lộ trình vận chuyển"
          description="Hiện tại bạn chưa có chuyến hàng đang hoạt động."
          type="info"
          showIcon
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Lộ trình vận chuyển</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={6}>
            <Statistic title="Tổng số điểm dừng" value={routeInfo.totalStops} />
          </Col>
          <Col span={6}>
            <Statistic title="Tổng số đơn" value={routeInfo.totalOrders} />
          </Col>
          <Col span={6}>
            <Statistic
              title="Trạng thái"
              value={routeInfo.status}
              valueStyle={{ textTransform: "uppercase" }}
            />
          </Col>
          <Col span={6}>
            {routeInfo.fromOffice && (
              <div>
                <Text type="secondary">Từ bưu cục:</Text>
                <br />
                <Text strong>{routeInfo.fromOffice.name}</Text>
              </div>
            )}
          </Col>
        </Row>
      </Card>

      <Card title="Danh sách điểm dừng">
        <List
          dataSource={deliveryStops}
          renderItem={(stop, index) => (
            <List.Item
              actions={[
                <Button
                  type="link"
                  icon={<EnvironmentOutlined />}
                  onClick={() => handleNavigateToStop(stop)}
                >
                  Xem bản đồ
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={
                  <div
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: "50%",
                      background: "#1890ff",
                      color: "white",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontWeight: "bold",
                    }}
                  >
                    {index + 1}
                  </div>
                }
                title={
                  <Space>
                    <Text strong>{stop.officeName}</Text>
                    <Tag color={getStatusColor(stop.status)}>{stop.status}</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size="small">
                    {stop.officeAddress && <Text type="secondary">{stop.officeAddress}</Text>}
                    <Text>Số đơn: {stop.orderCount}</Text>
                    {stop.orders && stop.orders.length > 0 && (
                      <div>
                        <Text type="secondary">Mã đơn: </Text>
                        {stop.orders.map((o, i) => (
                          <Tag key={i}>{o.trackingNumber}</Tag>
                        ))}
                      </div>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};

export default DriverRoute;
