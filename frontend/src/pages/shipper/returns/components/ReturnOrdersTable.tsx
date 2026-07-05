import React from "react";
import { Button, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { EyeOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ShipperOrder } from "../../../../api/orderApi";

const { Text } = Typography;

interface ReturnOrdersTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  onPageChange: (page: number, pageSize: number) => void;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "RETURN_AT_ORIGIN_OFFICE":
      return "warning";
    case "RETURNING":
      return "processing";
    case "RETURN_RETRY":
      return "orange";
    case "RETURNED":
      return "success";
    case "RETURN_FAILED_FINAL":
      return "error";
    default:
      return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case "RETURN_AT_ORIGIN_OFFICE":
      return "Chờ nhận hoàn trả";
    case "RETURNING":
      return "Đang hoàn trả";
    case "RETURN_RETRY":
      return "Hoàn lại";
    case "RETURNED":
      return "Đã hoàn trả";
    case "RETURN_FAILED_FINAL":
      return "Hoàn trả thất bại";
    default:
      return status;
  }
};

const ReturnOrdersTable: React.FC<ReturnOrdersTableProps> = ({
  orders,
  loading,
  pagination,
  onPageChange,
}) => {
  const navigate = useNavigate();

  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => <Text strong className="shipper-table-strong">{text}</Text>,
    },
    {
      title: "Người gửi (Shop)",
      key: "sender",
      render: (record: ShipperOrder) => {
        const senderName = (record as any).senderName || "";
        const senderPhone = (record as any).senderPhone || "";
        const senderAddress = (record as any).senderAddress || "";
        const fullAddress = typeof senderAddress === "string"
          ? senderAddress
          : (senderAddress as any)?.fullAddress || "";
        return (
          <Space direction="vertical" size={2}>
            <Text strong className="shipper-table-strong">{senderName}</Text>
            <Text className="shipper-table-muted">{senderPhone}</Text>
            <Text className="shipper-table-muted">{fullAddress}</Text>
          </Space>
        );
      },
    },
    {
      title: "Người nhận hoàn",
      key: "recipient",
      render: (record: ShipperOrder) => {
        const address =
          record.recipientFullAddress ||
          (typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress) || "";
        return (
          <Space direction="vertical" size={2}>
            <Text strong className="shipper-table-strong">{record.recipientName}</Text>
            <Text className="shipper-table-muted">{record.recipientPhone}</Text>
            <Text className="shipper-table-muted">{address}</Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ",
      key: "service",
      render: (record: ShipperOrder) => {
        const serviceName =
          typeof record.serviceType === "string"
            ? record.serviceType
            : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Text className="shipper-table-strong">{serviceName || "—"}</Text>
            <Tag color="warning">Hoàn trả</Tag>
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
      render: (record: ShipperOrder) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`)}>
            Chi tiết
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
      scroll={{ x: 1100 }}
    />
  );
};

export default ReturnOrdersTable;
