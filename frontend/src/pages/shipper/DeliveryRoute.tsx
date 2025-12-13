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
  Progress,
  Statistic,
  Modal,
  message,
  Spin,
  Alert,
  Divider,
} from "antd";
import {
  EnvironmentOutlined,
  PhoneOutlined,
  DollarOutlined,
  CompassOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  NodeIndexOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";

const { Title, Text} = Typography;

interface RouteInfo {
  id: number;
  name: string;
  startLocation: string;
  totalStops: number;
  completedStops: number;
  totalDistance: number;
  estimatedDuration: number;
  totalCOD: number;
  status: string;
}

interface DeliveryStop {
  id: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  codAmount: number;
  priority: string;
  serviceType: string;
  status: string;
}

const ShipperDeliveryRoute: React.FC = () => {
  const navigate = useNavigate();
  const [routeInfo, setRouteInfo] = useState<RouteInfo | null>(null);
  const [deliveryStops, setDeliveryStops] = useState<DeliveryStop[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedStop, setSelectedStop] = useState<DeliveryStop | null>(null);
  const [detailModal, setDetailModal] = useState(false);

  useEffect(() => {
    fetchRouteData();
  }, []);

  const fetchRouteData = async () => {
    try {
      setLoading(true);
      const routeData = await orderApi.getShipperRoute();
      console.log("API DATA:", routeData);
      setRouteInfo(routeData.routeInfo);
      setDeliveryStops(routeData.deliveryStops || []);
    } catch (error) {
      console.error("Error fetching route data:", error);
      message.error("Lỗi khi tải dữ liệu lộ trình");
    } finally {
      setLoading(false);
    }
  };

  const handleStartRoute = async () => {
    if (!routeInfo) return;

    const openDirections = () => {
      if (!deliveryStops || deliveryStops.length === 0) {
        message.warning("Không có điểm dừng nào trong tuyến");
        return;
      }

      const stops = deliveryStops.map((s) => s.recipientAddress);
      let destination = encodeURIComponent(stops[stops.length - 1]);
      let waypoints = "";

      if (stops.length > 1) {
        const mid = stops.slice(0, -1).map((a) => encodeURIComponent(a)).join("|");
        waypoints = `&waypoints=${mid}`;
      }

      const url = `https://www.google.com/maps/dir/?api=1&origin=Current+Location&destination=${destination}${waypoints}&travelmode=driving`;
      window.open(url, "_blank");
    };

    Modal.confirm({
      title: "Bắt đầu tuyến giao hàng",
      content: "Bạn có chắc chắn muốn bắt đầu tuyến giao hàng này?",
      onOk: async () => {
        try {
          await orderApi.startShipperRoute(routeInfo.id);
          setRouteInfo((prev) => (prev ? { ...prev, status: "in_progress" } : null));
          message.success("Đã bắt đầu tuyến giao hàng");
          openDirections();
        } catch (error) {
          openDirections();
        }
      },
    });
  };

  const handleNavigateToStop = (stop: DeliveryStop) => {
    const address = encodeURIComponent(stop.recipientAddress);
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${address}`;
    window.open(mapsUrl, "_blank");
    message.success(`Đã mở bản đồ đến ${stop.recipientName}`);
  };

  const handleViewStopDetail = (stop: DeliveryStop) => {
    setSelectedStop(stop);
    setDetailModal(true);
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return "default";
      case "in_progress":
        return "processing";
      case "completed":
        return "success";
      case "failed":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status.toLowerCase()) {
      case "pending":
        return "Chờ giao";
      case "in_progress":
        return "Đang giao";
      case "completed":
        return "Hoàn thành";
      case "failed":
        return "Thất bại";
      default:
        return status;
    }
  };

  const getPriorityColor = (priority: string) => {
    return priority === "urgent" ? "red" : "default";
  };

  const getPriorityText = (priority: string) => {
    return priority === "urgent" ? "Ưu tiên" : "Bình thường";
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "50px" }}>
        <Spin size="large" />
        <div style={{ marginTop: "16px" }}>Đang tải dữ liệu lộ trình...</div>
      </div>
    );
  }

  if (!routeInfo || deliveryStops.length === 0) {
    return (
      <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
        <Alert message="Không có lộ trình giao hàng hôm nay" type="info" showIcon />
      </div>
    );
  }

  const completionRate = routeInfo.totalStops > 0 ? (routeInfo.completedStops / routeInfo.totalStops) * 100 : 0;

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Title level={2} style={{ color: "#1C3D90", marginBottom: 24 }}>
        Lộ trình giao hàng
      </Title>

      <Card style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col xs={24} sm={12} lg={6}>
            <Statistic title="Tổng điểm dừng" value={routeInfo.totalStops} prefix={<NodeIndexOutlined />} />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic title="Đã hoàn thành" value={routeInfo.completedStops} prefix={<CheckCircleOutlined />} />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic
              title="Tổng COD"
              value={routeInfo.totalCOD}
              prefix={<DollarOutlined />}
              formatter={(value) => `${value?.toLocaleString()}đ`}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Statistic title="Thời gian ước tính" value={routeInfo.estimatedDuration} suffix="phút" />
          </Col>
        </Row>

        <Divider />

        <Progress percent={Math.round(completionRate)} status="active" />

        <Space style={{ marginTop: 16 }}>
          {routeInfo.status === "not_started" && (
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={handleStartRoute}>
              Bắt đầu tuyến
            </Button>
          )}
          {routeInfo.status === "in_progress" && (
            <>
              <Button icon={<PauseCircleOutlined />}>Tạm dừng</Button>
              <Button type="primary" icon={<CompassOutlined />} onClick={() => navigate("/shipper/orders")}>
                Xem đơn hàng
              </Button>
            </>
          )}
        </Space>
      </Card>

      <Card title="Danh sách điểm giao hàng">
        <List
          dataSource={deliveryStops}
          renderItem={(stop, index) => (
            <List.Item
              actions={[
                <Button icon={<EnvironmentOutlined />} onClick={() => handleNavigateToStop(stop)}>
                  Chỉ đường
                </Button>,
                <Button icon={<EyeOutlined />} onClick={() => handleViewStopDetail(stop)}>
                  Chi tiết
                </Button>,
                <Button type="link" onClick={() => navigate(`/orders/${stop.id}`)}>
                  Xem đơn
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
                      background: stop.status === "completed" ? "#52c41a" : "#1890ff",
                      color: "#fff",
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
                    <Text strong>{stop.trackingNumber}</Text>
                    <Tag color={getPriorityColor(stop.priority)}>{getPriorityText(stop.priority)}</Tag>
                    <Tag color={getStatusColor(stop.status)}>{getStatusText(stop.status)}</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size={4}>
                    <Text>
                      <PhoneOutlined /> {stop.recipientPhone} - {stop.recipientName}
                    </Text>
                    <Text type="secondary">
                      <EnvironmentOutlined /> {stop.recipientAddress}
                    </Text>
                    {stop.codAmount > 0 && (
                      <Text>
                        <DollarOutlined /> COD: {stop.codAmount.toLocaleString()}đ
                      </Text>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      <Modal
        title="Chi tiết điểm giao hàng"
        open={detailModal}
        onCancel={() => {
          setDetailModal(false);
          setSelectedStop(null);
        }}
        footer={null}
        width={600}
      >
        {selectedStop && (
          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            <div>
              <Text strong>Mã đơn hàng: </Text>
              <Text>{selectedStop.trackingNumber}</Text>
            </div>
            <div>
              <Text strong>Người nhận: </Text>
              <Text>{selectedStop.recipientName}</Text>
            </div>
            <div>
              <Text strong>SĐT: </Text>
              <Text>{selectedStop.recipientPhone}</Text>
            </div>
            <div>
              <Text strong>Địa chỉ: </Text>
              <Text>{selectedStop.recipientAddress}</Text>
            </div>
            <div>
              <Text strong>COD: </Text>
              <Text style={{ color: "#f50" }}>{selectedStop.codAmount.toLocaleString()}đ</Text>
            </div>
            <div>
              <Text strong>Dịch vụ: </Text>
              <Text>{selectedStop.serviceType}</Text>
            </div>
            <div>
              <Text strong>Trạng thái: </Text>
              <Tag color={getStatusColor(selectedStop.status)}>{getStatusText(selectedStop.status)}</Tag>
            </div>
            <Button type="primary" block icon={<EnvironmentOutlined />} onClick={() => handleNavigateToStop(selectedStop)}>
              Mở Google Maps
            </Button>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default ShipperDeliveryRoute;
