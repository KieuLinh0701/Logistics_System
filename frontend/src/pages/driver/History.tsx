import React, { useEffect, useState } from "react";
import { Card, Table, Button, Space, Typography, message, Tag, Descriptions } from "antd";
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

  const getStatusColor = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "success";
      case "CANCELLED":
        return "error";
      default:
        return "default";
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

  const columns = [
    { title: "Mã chuyến", dataIndex: "code", key: "code" },
    {
      title: "Trạng thái",
      key: "status",
      render: (_: any, r: DriverShipment) => (
        <Tag color={getStatusColor(r.status)}>{getStatusText(r.status)}</Tag>
      ),
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, r: DriverShipment) =>
        r.vehicle ? `${r.vehicle.licensePlate} (${r.vehicle.type})` : "-",
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
    { title: "Thời gian bắt đầu", dataIndex: "startTime", key: "startTime" },
    { title: "Thời gian kết thúc", dataIndex: "endTime", key: "endTime" },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={<Title level={3}>Lịch sử vận chuyển</Title>}
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadHistory}>
            Tải lại
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={shipments}
          loading={loading}
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
            rowExpandable: (record: DriverShipment) => !!(record.orders && record.orders.length > 0),
          }}
        />
      </Card>
    </div>
  );
};

export default DriverHistory;
