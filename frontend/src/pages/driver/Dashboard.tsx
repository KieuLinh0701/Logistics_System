import React, { useEffect, useState } from "react";
import { Card, Row, Col, List, Tag, Button, Space, Typography, message, Table, Select } from "antd";
import { TruckOutlined, EnvironmentOutlined } from "@ant-design/icons";
import orderApi from "../../api/orderApi";
import shipmentApi from "../../api/shipmentApi";
import { translateOrderStatus } from "../../utils/orderUtils";

const { Title, Text } = Typography;
const { Option } = Select;

interface OfficeInfo {
  id: number;
  name: string;
  address?: string;
}

interface Vehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
}

interface OrderItem {
  id: number;
  trackingNumber: string;
  toOffice?: { id: number; name: string };
  serviceType?: { id: number; name: string };
}

const DriverDashboard: React.FC = () => {
  const [office, setOffice] = useState<OfficeInfo | null>(null);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [vehicleId, setVehicleId] = useState<number | undefined>(undefined);
  const [activeShipment, setActiveShipment] = useState<any>(null);

  useEffect(() => {
    loadContext();
    loadOrders();
    loadActiveShipment();
  }, []);

  const loadContext = async () => {
    try {
      const res = await orderApi.getDriverContext();
      setOffice(res.office || null);
      setVehicles(Array.isArray(res.vehicles) ? res.vehicles : []);
    } catch (e: any) {
      message.error("Không tải được thông tin driver");
    }
  };

  const loadOrders = async () => {
    try {
      setLoading(true);
      const res = await orderApi.getDriverPendingOrders({ page: 1, limit: 50 });
      setOrders(Array.isArray(res.orders) ? res.orders : []);
    } catch (e: any) {
      message.error("Không tải được danh sách đơn");
    } finally {
      setLoading(false);
    }
  };

  const loadActiveShipment = async () => {
    try {
      const res = await shipmentApi.getDriverRoute();
      if (res.routeInfo) {
        setActiveShipment(res.routeInfo);
      }
    } catch (e: any) {
      // Không có chuyến đang hoạt động
    }
  };

  const handlePickup = async () => {
    try {
      if (!orders.length) return message.info("Không có đơn để nhận");
      const orderIds = orders.map((o) => o.id);
      await orderApi.driverPickUp({ vehicleId, orderIds });
      message.success("Đã nhận hàng, tạo chuyến Pending");
      loadOrders();
      loadActiveShipment();
    } catch (e: any) {
      message.error(e?.message || "Lỗi nhận hàng");
    }
  };

  const columns = [
    { title: "Mã đơn", dataIndex: "trackingNumber", key: "trackingNumber" },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, r: OrderItem) => r.toOffice?.name || "-",
    },
    {
      title: "Dịch vụ",
      key: "serviceType",
      render: (_: any, r: OrderItem) => r.serviceType?.name || "-",
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Dashboard Tài Xế</Title>

      {activeShipment && (
        <Card
          style={{ marginBottom: 16, background: "#e6f7ff", border: "1px solid #91d5ff" }}
        >
          <Space>
            <TruckOutlined style={{ fontSize: 24, color: "#1890ff" }} />
            <div>
              <Text strong>Chuyến hàng đang hoạt động: {activeShipment.code || `#${activeShipment.id}`}</Text>
              <br />
              <Text type="secondary">Trạng thái: {activeShipment.status}</Text>
            </div>
          </Space>
        </Card>
      )}

      <Row gutter={16}>
        <Col span={10}>
          <Card title="Văn phòng làm việc" icon={<EnvironmentOutlined />}>
            {office ? (
              <Space direction="vertical">
                <Text strong>{office.name}</Text>
                {office.address && <Text type="secondary">{office.address}</Text>}
              </Space>
            ) : (
              <Text>—</Text>
            )}
          </Card>

          <Card title="Phương tiện tại văn phòng" style={{ marginTop: 16 }}>
            <Space direction="vertical" style={{ width: "100%" }}>
              <Select
                allowClear
                placeholder="Chọn phương tiện cho chuyến đi"
                style={{ width: "100%" }}
                value={vehicleId}
                onChange={(v) => setVehicleId(v)}
              >
                {vehicles.map((v) => (
                  <Option key={v.id} value={v.id}>
                    {v.licensePlate} - {v.type} - {v.capacity}kg
                  </Option>
                ))}
              </Select>
              <List
                dataSource={vehicles}
                renderItem={(v) => (
                  <List.Item>
                    <Space>
                      <Text strong>{v.licensePlate}</Text>
                      <Tag>{v.type}</Tag>
                      <Tag color="blue">{v.capacity}kg</Tag>
                      <Tag color={v.status === "AVAILABLE" ? "green" : "orange"}>
                        {v.status}
                      </Tag>
                    </Space>
                  </List.Item>
                )}
              />
            </Space>
          </Card>
        </Col>
        <Col span={14}>
          <Card
            title="Đơn cần nhận (AT_ORIGIN_OFFICE)"
            extra={<Button onClick={loadOrders}>Tải lại</Button>}
          >
            <Table
              rowKey="id"
              columns={columns}
              dataSource={orders}
              loading={loading}
              pagination={false}
            />
            <div style={{ marginTop: 12 }}>
              <Button
                type="primary"
                onClick={handlePickup}
                disabled={!orders.length}
              >
                Xác nhận nhận hàng
              </Button>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DriverDashboard;
