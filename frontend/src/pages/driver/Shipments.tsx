import React, { useEffect, useState } from "react";
import { Card, Table, Button, Space, Typography, message, Tag, Modal, Descriptions } from "antd";
import { ReloadOutlined, PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import type { Shipment } from "../../api/shipmentApi";
import { useNavigate } from "react-router-dom";

const { Title, Text } = Typography;

const DriverShipments: React.FC = () => {
  const navigate = useNavigate();
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });

  useEffect(() => {
    loadShipments();
  }, [pagination.page, pagination.limit]);

  const loadShipments = async () => {
    try {
      setLoading(true);
      const res = await shipmentApi.getDriverShipments({
        page: pagination.page,
        limit: pagination.limit,
      });
      setShipments(Array.isArray(res.shipments) ? res.shipments : []);
      setPagination(res.pagination || pagination);
    } catch (e: any) {
      message.error("Không tải được danh sách chuyến hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleStartShipment = async (shipmentId: number) => {
    Modal.confirm({
      title: "Bắt đầu vận chuyển",
      content: "Bạn có chắc chắn muốn bắt đầu vận chuyển chuyến hàng này?",
      onOk: async () => {
        try {
          await shipmentApi.startShipment(shipmentId);
          message.success("Đã bắt đầu vận chuyển");
          loadShipments();
        } catch (e: any) {
          message.error(e?.message || "Lỗi khi bắt đầu vận chuyển");
        }
      },
    });
  };

  const handleFinishShipment = async (shipmentId: number, status: "COMPLETED" | "CANCELLED") => {
    Modal.confirm({
      title: status === "COMPLETED" ? "Hoàn tất chuyến hàng" : "Hủy chuyến hàng",
      content: `Bạn có chắc chắn muốn ${status === "COMPLETED" ? "hoàn tất" : "hủy"} chuyến hàng này?`,
      onOk: async () => {
        try {
          await shipmentApi.finishShipment({ shipmentId, status });
          message.success(status === "COMPLETED" ? "Đã hoàn tất chuyến hàng" : "Đã hủy chuyến hàng");
          loadShipments();
        } catch (e: any) {
          message.error(e?.message || "Lỗi khi hoàn tất chuyến hàng");
        }
      },
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "default";
      case "IN_TRANSIT":
        return "processing";
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
      case "PENDING":
        return "Chờ xử lý";
      case "IN_TRANSIT":
        return "Đang vận chuyển";
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
      render: (_: any, r: Shipment) => (
        <Tag color={getStatusColor(r.status)}>{getStatusText(r.status)}</Tag>
      ),
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, r: Shipment) =>
        r.vehicle ? `${r.vehicle.licensePlate} (${r.vehicle.type})` : "-",
    },
    {
      title: "Từ bưu cục",
      key: "fromOffice",
      render: (_: any, r: Shipment) => r.fromOffice?.name || "-",
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, r: Shipment) => r.toOffice?.name || "-",
    },
    { title: "Số đơn", dataIndex: "orderCount", key: "orderCount" },
    { title: "Thời gian bắt đầu", dataIndex: "startTime", key: "startTime" },
    {
      title: "Thao tác",
      key: "actions",
      render: (_: any, r: Shipment) => (
        <Space direction="vertical" size="small">
          {r.status === "PENDING" && (
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={() => handleStartShipment(r.id)}
              block
            >
              Bắt đầu
            </Button>
          )}
          {r.status === "IN_TRANSIT" && (
            <Space>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => handleFinishShipment(r.id, "COMPLETED")}
              >
                Hoàn tất
              </Button>
              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleFinishShipment(r.id, "CANCELLED")}
              >
                Hủy
              </Button>
            </Space>
          )}
          <Button onClick={() => navigate(`/driver/route`)} block>
            Xem lộ trình
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={<Title level={3}>Quản lý chuyến vận chuyển</Title>}
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadShipments}>
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
            expandedRowRender: (record: Shipment) => (
              <div style={{ margin: 0 }}>
                <Title level={5}>Chi tiết đơn hàng trong chuyến</Title>
                {record.orders && record.orders.length > 0 ? (
                  <Descriptions size="small" column={2}>
                    {record.orders.map((order, index) => (
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
            rowExpandable: (record: Shipment) => !!(record.orders && record.orders.length > 0),
          }}
        />
      </Card>
    </div>
  );
};

export default DriverShipments;
