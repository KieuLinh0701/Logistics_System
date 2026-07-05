import React from "react";
import { Button, Table, Tag, Typography } from "antd";
import { FileExcelOutlined, PlayCircleOutlined } from "@ant-design/icons";

const { Text } = Typography;

interface PendingShipmentsTableProps {
  shipments: any[];
  loading: boolean;
  actionLoading: boolean;
  onStartShipment: (shipmentId: number) => void;
  onFinishShipment: (shipmentId: number) => void;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "PENDING": return "gold";
    case "IN_TRANSIT": return "processing";
    case "COMPLETED": return "success";
    case "CANCELLED": return "error";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status) {
    case "PENDING": return "Chờ bắt đầu";
    case "IN_TRANSIT": return "Đang giao";
    case "COMPLETED": return "Hoàn thành";
    case "CANCELLED": return "Đã hủy";
    default: return status;
  }
};

const PendingShipmentsTable: React.FC<PendingShipmentsTableProps> = ({
  shipments,
  loading,
  actionLoading,
  onStartShipment,
  onFinishShipment,
}) => {
  const getActionButton = (record: any) => {
    const status = record.status || record.shipmentStatus || record.state;

    if (status === "PENDING") {
      return (
        <Button
          className="primary-button"
          icon={<PlayCircleOutlined />}
          onClick={() => onStartShipment(record.id)}
          loading={actionLoading}
        >
          Bắt đầu chuyến
        </Button>
      );
    }

    if (status === "IN_TRANSIT") {
      return (
        <Button
          className="success-button"
          icon={<FileExcelOutlined />}
          onClick={() => onFinishShipment(record.id)}
          loading={actionLoading}
        >
          Kết thúc chuyến
        </Button>
      );
    }

    return null;
  };

  const columns = [
    {
      title: "Mã chuyến",
      dataIndex: "code",
      key: "code",
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, record: any) => (
        <Text>{record.vehicle?.licensePlate || "—"}</Text>
      ),
    },
    {
      title: "Từ bưu cục",
      key: "fromOffice",
      render: (_: any, record: any) => <Text>{record.fromOffice?.name || "—"}</Text>,
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, record: any) => <Text>{record.toOffice?.name || "—"}</Text>,
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
      title: "Số đơn",
      dataIndex: "orderCount",
      key: "orderCount",
      render: (value: number) => <Text>{value ?? 0}</Text>,
    },
    {
      title: "Thời gian tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (text: string) => <Text>{text ? new Date(text).toLocaleString("vi-VN") : "—"}</Text>,
    },
    {
      title: "Thao tác",
      key: "action",
      width: 150,
      render: (_: any, record: any) => getActionButton(record),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={shipments}
      pagination={false}
      locale={{ emptyText: "Chưa có chuyến hàng" }}
      scroll={{ x: 960 }}
    />
  );
};

export default PendingShipmentsTable;
