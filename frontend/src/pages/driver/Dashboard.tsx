import React, { useEffect, useState } from "react";
import { Card, Row, Col, Statistic, Space, Typography, Table, Tag } from "antd";
import { TruckOutlined, EnvironmentOutlined, ClockCircleOutlined, CheckCircleOutlined } from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import { translateShipmentStatus } from "../../utils/shipmentUtils";

function translateVehicleStatus(status: string) {
  if (!status) return '';
  switch (status.toUpperCase()) {
    case 'AVAILABLE':
      return 'Sẵn sàng';
    case 'IN_USE':
      return 'Đang sử dụng';
    case 'MAINTENANCE':
      return 'Bảo trì';
    default:
      return status;
  }
}

function getShipmentTagColor(status: string) {
  switch ((status || "").toUpperCase()) {
    case "READY_FOR_PICKUP":
      return "gold";
    case "PENDING":
      return "default";
    case "CONFIRMED":
    case "AT_DEST_OFFICE":
      return "blue";
    case "PICKED_UP":
      return "orange";
    case "IN_TRANSIT":
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
}

const { Title, Text } = Typography;

interface OfficeInfo {
  id: number;
  name: string;
  address?: string;
  detail?: string;
  cityCode?: number;
  wardCode?: number;
}

interface Vehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
}


interface ShipmentSummary {
  id?: number;
  code?: string;
  status?: string;
  fromOffice?: { name?: string };
  toOffice?: { name?: string };
}

const DriverDashboard: React.FC = () => {
  const [activeShipment, setActiveShipment] = useState<ShipmentSummary | null>(null);
  const [office, setOffice] = useState<OfficeInfo | null>(null);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [stats, setStats] = useState({ pending: 0, inTransit: 0, completed: 0 });
  const [recentShipments, setRecentShipments] = useState<ShipmentSummary[]>([]);

  useEffect(() => {
    (async () => {
      try {
        const res = await shipmentApi.getDriverRoute();
        if (res.routeInfo) setActiveShipment(res.routeInfo);
      } catch {
        // ignore
      }
    })();
    (async () => {
      try {
        const ctx = await shipmentApi.getDriverContext();
        if (ctx.office) setOffice(ctx.office);
        if (Array.isArray(ctx.vehicles)) setVehicles(ctx.vehicles);
      } catch {
        // ignore
      }
    })();

    (async () => {
      try {
        const shipRes = await shipmentApi.getDriverShipments({ page: 1, limit: 10 });
        const historyRes = await shipmentApi.getDriverHistory({ page: 1, limit: 10 });
        const current = (shipRes.shipments || []) as ShipmentSummary[];
        const history = (historyRes.shipments || []) as ShipmentSummary[];
        const pending = current.filter(s => (s.status || '').toString().toUpperCase() === 'PENDING').length;
        const inTransit = current.filter(s => (s.status || '').toString().toUpperCase() === 'IN_TRANSIT').length;
        const completed = history.filter(s => (s.status || '').toString().toUpperCase() === 'COMPLETED').length;
        setStats({ pending, inTransit, completed });
        const combined = [...current, ...history];
        const seen = new Set<number>();
        const deduped: ShipmentSummary[] = [];
        for (const s of combined) {
          if (!s || s.id == null) continue;
          if (!seen.has(s.id)) {
            seen.add(s.id);
            deduped.push(s);
          }
        }
        setRecentShipments(deduped.slice(0, 8));
      } catch {
        // ignore
      }
    })();
  }, []);
  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>Dashboard Tài Xế</Title>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8} lg={8}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic title="Chuyến chờ" value={stats.pending} prefix={<ClockCircleOutlined />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col xs={24} sm={8} lg={8}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic title="Đang vận chuyển" value={stats.inTransit} prefix={<TruckOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col xs={24} sm={8} lg={8}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic title="Hoàn thành" value={stats.completed} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
      </Row>

      {activeShipment && (
        <Card style={{ marginBottom: 16, background: "#e6f7ff", border: "1px solid #91d5ff" }}>
          <Space>
            <TruckOutlined style={{ fontSize: 24, color: "#1890ff" }} />
            <div>
              <Text strong>Chuyến hàng đang hoạt động: {activeShipment.code || `#${activeShipment.id ?? ''}`}</Text>
              <br />
              <Text type="secondary">Trạng thái: <Tag color={getShipmentTagColor(activeShipment.status || '')}>{translateShipmentStatus(activeShipment.status || '')}</Tag></Text>
            </div>
          </Space>
        </Card>
      )}

      <Row gutter={16}>
        <Col span={10}>
          <Card style={{ marginBottom: 12 }} title={(
            <Space>
              <EnvironmentOutlined />
              <span>Văn phòng làm việc</span>
            </Space>
          )}>
            {office ? (
              <div>
                <Text strong style={{ color: '#111' }}>{office.name}</Text>
                <br />
                <Text type="secondary">{office.address || office.detail || ""}</Text>
              </div>
            ) : (
              <Text style={{ color: '#333' }}>Thông tin văn phòng được quản lý bởi bưu cục/quản lý. Vui lòng liên hệ quản lý để nhận hàng.</Text>
            )}
          </Card>

          <Card title={(
            <Space>
              <TruckOutlined />
              <span>Phương tiện</span>
            </Space>
          )}>
            {vehicles.length === 0 ? (
              <Text type="secondary">Không có phương tiện khả dụng</Text>
            ) : (
              <ul style={{ marginTop: 8, paddingLeft: 18 }}>
                {vehicles.map((v, idx) => (
                  <li key={`${v.id}-${idx}`}><Text strong style={{ color: '#111' }}>{v.licensePlate}</Text>{` — ${v.type} `}{v.status ? <Text type="secondary">({translateVehicleStatus(v.status)})</Text> : null}</li>
                ))}
              </ul>
            )}
          </Card>
        </Col>
        <Col span={14}>
          <Card title="Chuyến vận chuyển">
            <Table
              rowKey={(r: ShipmentSummary, index?: number) => `${r.id ?? ''}-${index ?? 0}`}
              dataSource={recentShipments}
              pagination={false}
              columns={[
                { title: 'Mã chuyến', dataIndex: 'code', key: 'code', render: (t: string, r: ShipmentSummary) => t || `#${r.id ?? ''}` },
                { title: 'Từ', dataIndex: ['fromOffice', 'name'], key: 'fromOffice' },
                { title: 'Đến', dataIndex: ['toOffice', 'name'], key: 'toOffice' },
                { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (s?: string) => <Tag>{translateShipmentStatus(s || '')}</Tag> }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DriverDashboard;
