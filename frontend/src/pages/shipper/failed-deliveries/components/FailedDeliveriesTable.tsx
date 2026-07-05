import React from "react";
import { Button, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { InboxOutlined } from "@ant-design/icons";
import { translateOrderStatus } from "../../../../utils/orderUtils";
import type { ShipperOrder } from "../../../../api/orderApi";

const { Text } = Typography;

interface FailedDeliveriesTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  onReturnToOffice: (orderId: number, trackingNumber: string) => void;
  isReturning: boolean;
}

const FailedDeliveriesTable: React.FC<FailedDeliveriesTableProps> = ({
  orders,
  loading,
  onReturnToOffice,
  isReturning,
}) => {
  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Text strong>{text}</Text>,
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
      align: "center" as const,
      width: 150,
      render: (status: string) => (
        <Tag color={status === "DELIVERY_FAILED_FINAL" ? "red" : "orange"} style={{ fontWeight: 600 }}>
          {translateOrderStatus(status)}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "action",
      width: 150,
      render: (_, record) => (
        <Button
          type="primary"
          icon={<InboxOutlined />}
          onClick={() => onReturnToOffice(record.id, record.trackingNumber)}
          loading={isReturning}
          size="small"
          style={{ backgroundColor: "#16a34a", borderColor: "#16a34a" }}
        >
          Nộp bưu cục
        </Button>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      className="failed-deliveries-table"
      columns={columns}
      dataSource={orders}
      pagination={false}
      loading={loading}
      scroll={{ x: 900 }}
    />
  );
};

export default FailedDeliveriesTable;
