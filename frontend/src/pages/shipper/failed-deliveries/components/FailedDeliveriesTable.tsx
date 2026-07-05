import React from "react";
import { Button, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { InboxOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { translateOrderStatus } from "../../../../utils/orderUtils";
import type { ShipperOrder } from "../../../../api/orderApi";

const { Text } = Typography;

interface FailedDeliveriesTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  onReturnToOffice: (orderId: number) => void;
  isReturning: boolean;
}

const FailedDeliveriesTable: React.FC<FailedDeliveriesTableProps> = ({
  orders,
  loading,
  onReturnToOffice,
  isReturning,
}) => {
  const navigate = useNavigate();

  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string, record) => (
        <Button
          type="link"
          onClick={() => navigate(`/shipper/orders/${record.id}`)}
          style={{ padding: 0, height: "auto" }}
        >
          <Text strong style={{ color: "#111111" }}>{text}</Text>
        </Button>
      ),
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (_, record) => (
        <Typography.Text strong>{record.recipientName}</Typography.Text>
      ),
    },
    {
      title: "SĐT",
      dataIndex: "recipientPhone",
      key: "recipientPhone",
      render: (text: string) => <Text style={{ color: "#111111" }}>{text}</Text>,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Tag color={status === "DELIVERY_FAILED_FINAL" ? "red" : "orange"} style={{ fontWeight: 600 }}>
          {translateOrderStatus(status)}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (_, record) => (
        <Button
          icon={<InboxOutlined />}
          onClick={() => onReturnToOffice(record.id)}
          loading={isReturning}
        >
          Nộp bưu cục
        </Button>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={orders}
      pagination={false}
      loading={loading}
      scroll={{ x: 900 }}
    />
  );
};

export default FailedDeliveriesTable;
