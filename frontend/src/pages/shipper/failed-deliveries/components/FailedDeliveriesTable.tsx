import React from "react";
import {Table, Tag, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {translateOrderStatus} from "../../../../utils/orderUtils";
import type {ShipperOrder} from "../../../../api/orderApi";

const { Text } = Typography;

interface FailedDeliveriesTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  selectedRowKeys: React.Key[];
  onSelectionChange: (keys: React.Key[]) => void;
}

const FailedDeliveriesTable: React.FC<FailedDeliveriesTableProps> = ({
  orders,
  loading,
  selectedRowKeys,
  onSelectionChange,
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
      rowSelection={{
        selectedRowKeys,
        onChange: (keys) => onSelectionChange(keys),
      }}
    />
  );
};

export default FailedDeliveriesTable;