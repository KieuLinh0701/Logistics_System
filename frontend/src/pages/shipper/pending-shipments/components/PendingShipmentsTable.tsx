import React from "react";
import {Button, Popover, Progress, Table, Tag, Typography} from "antd";
import {FileExcelOutlined, PlayCircleOutlined, ScanOutlined} from "@ant-design/icons";

const { Text } = Typography;

interface PendingShipmentsTableProps {
  shipments: any[];
  loading: boolean;
  actionLoading: boolean;
  onStartShipment: (shipmentId: number) => void;
  onFinishShipment: (shipmentId: number) => void;
}

interface StartValidation {
  ready: boolean;
  message?: string;
  scannedCount: number;
  totalCount: number;
}

const isShipmentReadyToStart = (shipment: any): StartValidation => {
  const scannedCount = shipment.scannedCount ?? 0;
  const totalCount = shipment.totalCount ?? shipment.orderCount ?? 0;
  const isReady = shipment.isReadyToStart ?? false;

  if (isReady) {
    return { ready: true, scannedCount, totalCount };
  }

  if (totalCount > 0) {
    const remaining = totalCount - scannedCount;
    if (remaining > 0) {
      return {
        ready: false,
        message: `Còn ${remaining}/${totalCount} đơn giao chưa lên xe. Vui lòng quét QR trước khi bắt đầu.`,
        scannedCount,
        totalCount,
      };
    }
  }

  return { ready: true, scannedCount, totalCount };
};

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
      const validation = isShipmentReadyToStart(record);
      const percent = validation.totalCount > 0
        ? Math.round((validation.scannedCount / validation.totalCount) * 100)
        : 0;

      const button = (
        <Button
          className="primary-button"
          icon={<PlayCircleOutlined />}
          onClick={() => onStartShipment(record.id)}
          loading={actionLoading}
          disabled={!validation.ready}
        >
          Bắt đầu chuyến
        </Button>
      );

      // Show progress popover when not ready
      if (!validation.ready) {
        return (
          <Popover
            content={
              <div style={{ minWidth: 200 }}>
                <div style={{ marginBottom: 8 }}>
                  <ScanOutlined style={{ marginRight: 6 }} />
                  <Text type="secondary">Tiến độ quét QR</Text>
                </div>
                <Progress
                  percent={percent}
                  size="small"
                  status="exception"
                  format={() => `${validation.scannedCount}/${validation.totalCount}`}
                />
                <Text type="warning" style={{ fontSize: 12, display: "block", marginTop: 4 }}>
                  {validation.message}
                </Text>
              </div>
            }
            title="Chưa thể bắt đầu chuyến"
            trigger="hover"
          >
            {button}
          </Popover>
        );
      }

      return button;
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
      key: "totalOrders",
      render: (_: any, record: any) => {
        const value = record.totalOrders ?? record.orderCount ?? 0;
        return <Text>{value}</Text>;
      },
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
