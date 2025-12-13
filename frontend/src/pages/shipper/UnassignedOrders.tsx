import React, { useEffect, useState } from "react";
import { Card, Table, Button, Space, Tag, message, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { EyeOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";
import type { ShipperOrder } from "../../api/orderApi";

type OrderItem = ShipperOrder;

const ShipperUnassignedOrders: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<OrderItem[]>([]);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchUnassigned = async (p = page, l = limit) => {
    try {
      setLoading(true);
      const resp = await orderApi.getShipperUnassignedOrders({ page: p, limit: l });
      setData(resp.orders || []);
      setTotal(resp.pagination?.total || 0);
      setPage(resp.pagination?.page || p);
      setLimit(resp.pagination?.limit || l);
    } catch (err) {
      message.error("Lỗi khi tải đơn chưa gán");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnassigned(1, limit);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleClaim = async (orderId: number) => {
    try {
      await orderApi.claimShipperOrder(orderId);
      message.success("Đã nhận đơn");
      fetchUnassigned(page, limit);
    } catch (err: any) {
      message.error(err?.message || "Lỗi khi nhận đơn");
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
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const columns: ColumnsType<OrderItem> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Typography.Text strong style={{ color: "#1f2937" }}>{text}</Typography.Text>,
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (_, record) => {
        const address =
          typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong style={{ color: "#111827" }}>{record.recipientName}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12, color: "#6b7280" }}>
              {record.recipientPhone}
            </Typography.Text>
            <Typography.Text style={{ fontSize: 12, color: "#4b5563" }}>{address}</Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      render: (_, record) => {
        const serviceName =
          typeof record.serviceType === "string" ? record.serviceType : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text style={{ color: "#1f2937" }}>{serviceName || "—"}</Typography.Text>
            <Typography.Text style={{ color: "#ef4444", fontWeight: 600 }}>
              {record.cod ? `${record.cod.toLocaleString()}đ` : "COD: 0đ"}
            </Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (s: string) => (
        <Tag color={getStatusColor(s)} style={{ fontWeight: 600, textTransform: "uppercase" }}>
          {getStatusText(s)}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/orders/${record.id}`)}>
            Chi tiết
          </Button>
          <Button type="primary" onClick={() => handleClaim(record.id!)}>Nhận đơn</Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Card title="Đơn chưa gán" bordered={false}>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data}
          pagination={{
            current: page,
            pageSize: limit,
            total,
            onChange: (p, l) => fetchUnassigned(p, l),
          }}
        />
      </Card>
    </div>
  );
};

export default ShipperUnassignedOrders;
