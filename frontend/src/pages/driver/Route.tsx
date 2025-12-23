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
  Modal,
} from "antd";
import {
  EnvironmentOutlined,
  TruckOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import type { DriverRouteInfo, DriverDeliveryStop } from "../../types/shipment";

const { Title, Text } = Typography;

const DriverRoute: React.FC = () => {
  type StopOrderItem = NonNullable<DriverDeliveryStop['orders']>[number];
  const [routeInfo, setRouteInfo] = useState<DriverRouteInfo | null>(null);
  const [deliveryStops, setDeliveryStops] = useState<DriverDeliveryStop[]>([]);
  const [loading, setLoading] = useState(false);
  const [mapModalOpen, setMapModalOpen] = useState(false);
  const [mapModalAddress, setMapModalAddress] = useState<string | null>(null);

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

  const handleNavigateToStop = (stop: DriverDeliveryStop) => {
    if (stop.officeAddress) {
      const address = encodeURIComponent(stop.officeAddress);
      const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${address}`;
      window.open(mapsUrl, "_blank");
      message.success(`Đã mở bản đồ đến ${stop.officeName}`);
    }
  };

  const openMapModal = (address: string) => {
    setMapModalAddress(address);
    setMapModalOpen(true);
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

  const translateStatus = (status?: string, scope: "route" | "stop" = "stop") => {
    if (!status) return "";
    if (scope === "route") {
      const s = status.toString().toUpperCase();
      switch (s) {
        case "PENDING":
          return "Chưa bắt đầu";
        case "IN_TRANSIT":
          return "Đang vận chuyển";
        case "COMPLETED":
          return "Hoàn tất";
        case "CANCELLED":
          return "Đã hủy";
        default:
          return status;
      }
    }

    const sLow = status.toString().toLowerCase();
    switch (sLow) {
      case "pending":
        return "Chờ giao";
      case "in_progress":
        return "Đang giao";
      case "completed":
        return "Hoàn tất";
      default:
        return status;
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
              value={translateStatus(routeInfo.status, "route")}
              valueStyle={{ textTransform: "uppercase" }}
            />
          </Col>
          <Col span={6}>
            {routeInfo.fromOffice && (
              <Statistic
                title="Từ bưu cục"
                value={routeInfo.fromOffice?.name}
              />
            )}
          </Col>
        </Row>
        <div style={{ marginTop: 12 }}>
          <div style={{ width: "100%", height: 360 }}>
            {(() => {
              const first = deliveryStops && deliveryStops.length > 0 ? deliveryStops[0] : null;
              let src = `https://www.google.com/maps?q=Vietnam&output=embed`;
              if (first) {
                const anyFirst = first as any;
                const lat = anyFirst.latitude ?? anyFirst.lat ?? anyFirst.officeLatitude ?? null;
                const lng = anyFirst.longitude ?? anyFirst.lng ?? anyFirst.officeLongitude ?? null;
                if (lat != null && lng != null) {
                  src = `https://www.google.com/maps?q=${lat},${lng}&z=15&output=embed`;
                } else if (first.officeAddress) {
                  src = `https://www.google.com/maps?q=${encodeURIComponent(first.officeAddress)}&z=15&output=embed`;
                }
              }

              return (
                <iframe
                  title="route-inline-map"
                  src={src}
                  style={{ width: "100%", height: "100%", border: 0 }}
                  allowFullScreen
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              );
            })()}
          </div>
        </div>
      </Card>

      <Card title="Danh sách điểm dừng">
          <List
          dataSource={deliveryStops}
          renderItem={(stop: DriverDeliveryStop, index: number) => (
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
                    <Tag style={{ background: 'transparent', color: '#000', border: '1px solid #d9d9d9' }}>{translateStatus(stop.status, "stop")}</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size="small">
                    {stop.officeAddress && <Text type="secondary">{stop.officeAddress}</Text>}
                    <Text>Số đơn: {stop.orderCount}</Text>
                        {stop.orders && stop.orders.length > 0 && (
                      <div>
                        <Text type="secondary">Mã đơn: </Text>
                        {stop.orders.map((o: StopOrderItem, i: number) => (
                          <Tag key={o.id ?? i}>{o.trackingNumber}</Tag>
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
      <Modal
        title={mapModalAddress ? "Bản đồ điểm dừng" : "Bản đồ"}
        open={mapModalOpen}
        onCancel={() => {
          setMapModalOpen(false);
          setMapModalAddress(null);
        }}
        footer={null}
        width={800}
      >
        {mapModalAddress && (
          <div style={{ width: "100%", height: 500 }}>
            <iframe
              title="route-map"
              src={`https://www.google.com/maps?q=${encodeURIComponent(mapModalAddress)}&z=15&output=embed`}
              style={{ width: "100%", height: "100%", border: 0 }}
              allowFullScreen
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
            />
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DriverRoute;


