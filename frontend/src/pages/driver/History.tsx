import React, { useEffect, useState } from "react";
import { Table, Button, Space, Typography, message, Descriptions, Row } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import type { DriverShipment } from "../../types/shipment";

const { Title, Text } = Typography;

const DriverHistory: React.FC = () => {
  type DriverOrderItem = NonNullable<DriverShipment['orders']>[number];
  const [shipments, setShipments] = useState<DriverShipment[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });

  useEffect(() => {
    loadHistory();
  }, [pagination.page, pagination.limit]);

  const loadHistory = async () => {
    try {
      setLoading(true);
      const res = await shipmentApi.getDriverHistory({
        page: pagination.page,
        limit: pagination.limit,
      });
      setShipments(Array.isArray(res.shipments) ? res.shipments : []);
      setPagination(res.pagination || pagination);
    } catch (e: any) {
      message.error("Không tải được lịch sử vận chuyển");
    } finally {
      setLoading(false);
    }
  };

  

  const getStatusText = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "Hoàn thành";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const translateVehicleType = (type?: string) => {
    if (!type) return "";
    switch (type.toString().toUpperCase()) {
      case "TRUCK":
        return "Xe tải";
      case "VAN":
        return "Xe van";
      case "CONTAINER":
        return "Xe container";
      default:
        return type;
    }
  };

  const formatDateTime = (iso?: string) => {
    if (!iso) return "-";
    try {
      const d = new Date(iso);
      const date = d.toLocaleDateString("vi-VN");
      const time = d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
      return (
        <div>
          <div>{date}</div>
          <div>{time}</div>
        </div>
      );
    } catch (e) {
      return iso;
    }
  };

  const columns = [
    { title: "Mã chuyến", dataIndex: "code", key: "code" },
    {
      title: "Trạng thái",
      key: "status",
      render: (_: any, r: DriverShipment) => (
        <div className="list-table-status-text">{getStatusText(r.status)}</div>
      ),
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, r: DriverShipment) => {
        if (!r.vehicle) return "-";
        return (
          <div>
            <div style={{ fontWeight: 700, color: "#111827" }}>{r.vehicle.licensePlate}</div>
            <div style={{ fontSize: 12, color: "#6b7280" }}>({translateVehicleType(r.vehicle.type)})</div>
          </div>
        );
      },
    },
    {
      title: "Từ bưu cục",
      key: "fromOffice",
      render: (_: any, r: DriverShipment) => r.fromOffice?.name || "-",
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, r: DriverShipment) => r.toOffice?.name || "-",
    },
    { title: "Số đơn", dataIndex: "orderCount", key: "orderCount" },
    { title: "Thời gian bắt đầu", dataIndex: "startTime", key: "startTime", render: (_: any, r: DriverShipment) => formatDateTime(r.startTime) },
    { title: "Thời gian kết thúc", dataIndex: "endTime", key: "endTime", render: (_: any, r: DriverShipment) => formatDateTime(r.endTime) },
  ];

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row className="list-page-header" justify="space-between" align="middle">
          <Title level={3} className="list-page-title-main">Lịch sử vận chuyển</Title>
          <div className="list-page-actions">
            <Button icon={<ReloadOutlined />} onClick={loadHistory}>Tải lại</Button>
          </div>
        </Row>

        <div className="list-page-table">
          <Table
            rowKey="id"
            className="list-page-table"
            columns={columns}
            dataSource={shipments}
            loading={loading}
            bordered
            pagination={{
              current: pagination.page,
              pageSize: pagination.limit,
              total: pagination.total,
              onChange: (page, pageSize) => {
                setPagination({ ...pagination, page, limit: pageSize });
              },
            }}
            expandable={{
              expandedRowRender: (record: DriverShipment) => (
                <div style={{ margin: 0 }}>
                  <Title level={5}>Chi tiết đơn hàng trong chuyến</Title>
                  {record.orders && record.orders.length > 0 ? (
                    <Descriptions size="small" column={2}>
                      {record.orders.map((order: DriverOrderItem, index: number) => (
                        <Descriptions.Item key={order.id} label={`Đơn ${index + 1}`}>
                          <Space direction="vertical" size={0}>
                            <Text strong>{order.trackingNumber}</Text>
                            <Text type="secondary">{order.toOffice?.name || "—"}</Text>
                          </Space>
                        </Descriptions.Item>
                      ))}
                    </Descriptions>
                  ) : (
                    <Text type="secondary">Không có đơn hàng</Text>
                  )}
                </div>
              ),
              rowExpandable: (record: DriverShipment) => !!((record.orders && record.orders.length > 0) || (record.orderCount && record.orderCount > 0)),
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default DriverHistory;
