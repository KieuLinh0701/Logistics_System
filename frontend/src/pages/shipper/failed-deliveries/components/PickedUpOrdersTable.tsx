import React from "react";
import { Button, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { InboxOutlined } from "@ant-design/icons";
import type { ShipperOrder } from "../../../../api/orderApi";

const { Text } = Typography;

interface PickedUpOrdersTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  onDeliverToOrigin: (orderId: number, trackingNumber: string) => void;
  isDelivering: boolean;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "PICKED_UP":
      return "orange";
    case "AT_ORIGIN_OFFICE":
      return "green";
    default:
      return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case "PICKED_UP":
      return "Đã lấy từ khách";
    case "AT_ORIGIN_OFFICE":
      return "Đã nộp bưu cục";
    default:
      return status;
  }
};

const PickedUpOrdersTable: React.FC<PickedUpOrdersTableProps> = ({
  orders,
  loading,
  onDeliverToOrigin,
  isDelivering,
}) => {
  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: "Người gửi",
      dataIndex: "senderName",
      key: "senderName",
      render: (text: string) => <Text>{text || "-"}</Text>,
    },
    {
      title: "SĐT người gửi",
      dataIndex: "senderPhone",
      key: "senderPhone",
      render: (text: string) => <Text>{text || "-"}</Text>,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      align: "center" as const,
      width: 150,
      render: (s: string) => (
        <Tag color={getStatusColor(s)} style={{ fontWeight: 600 }}>
          {getStatusText(s)}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "action",
      width: 150,
      render: (_, record) => (
        record.status === "PICKED_UP" && (
          <Button
            type="primary"
            icon={<InboxOutlined />}
            size="small"
            loading={isDelivering}
            onClick={() => onDeliverToOrigin(record.id, record.trackingNumber)}
            style={{ backgroundColor: "#16a34a", borderColor: "#16a34a" }}
          >
            Nộp bưu cục
          </Button>
        )
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

export default PickedUpOrdersTable;
