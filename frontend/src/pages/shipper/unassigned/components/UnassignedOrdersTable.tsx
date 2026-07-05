import React from "react";
import { Button, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { EyeOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ShipperOrder } from "../../../../api/orderApi";

const { Text } = Typography;

interface UnassignedOrdersTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  onPageChange: (page: number, limit: number) => void;
  onClaim: (orderId: number) => void;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "PENDING": return "default";
    case "CONFIRMED": return "blue";
    case "AT_DEST_OFFICE":
    case "RETURN_AT_ORIGIN_OFFICE": return "orange";
    case "READY_FOR_PICKUP": return "blue";
    case "PICKED_UP": return "orange";
    case "DELIVERING": return "processing";
    case "DELIVERED": return "success";
    case "CANCELLED": return "error";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case "PENDING": return "Chờ xử lý";
    case "CONFIRMED": return "Đã xác nhận";
    case "AT_DEST_OFFICE": return "Đã đến bưu cục";
    case "RETURN_AT_ORIGIN_OFFICE": return "Đã hoàn về bưu cục gốc";
    case "READY_FOR_PICKUP": return "Sẵn sàng lấy hàng";
    case "PICKED_UP": return "Đã lấy hàng";
    case "DELIVERING": return "Đang giao hàng";
    case "DELIVERED": return "Đã giao";
    case "CANCELLED": return "Đã hủy";
    default: return status;
  }
};

const UnassignedOrdersTable: React.FC<UnassignedOrdersTableProps> = ({
  orders,
  loading,
  pagination,
  onPageChange,
  onClaim,
}) => {
  const navigate = useNavigate();

  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Typography.Text strong className="shipper-table-strong">{text}</Typography.Text>,
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (_, record) => {
        const address =
          record.recipientFullAddress ||
          (typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress) || "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong className="shipper-table-strong">
              {record.recipientName}
            </Typography.Text>
            <Typography.Text className="shipper-table-muted">{record.recipientPhone}</Typography.Text>
            <Typography.Text className="shipper-table-muted">{address}</Typography.Text>
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
            <Typography.Text className="shipper-table-strong">{serviceName || "—"}</Typography.Text>
            <Typography.Text className="shipper-cod-value">
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
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`)}>
            Chi tiết
          </Button>
          <Button type="primary" className="primary-button" onClick={() => onClaim(record.id!)}>
            Nhận đơn
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={orders}
      pagination={{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        onChange: onPageChange,
      }}
      scroll={{ x: 960 }}
    />
  );
};

export default UnassignedOrdersTable;
