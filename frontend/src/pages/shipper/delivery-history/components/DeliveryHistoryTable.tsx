import React from "react";
import {Button, Space, Table, Tag, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {EyeOutlined, PhoneOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import type {ShipperOrder} from "../../../../api/orderApi";
import {translateOrderCodStatus} from "../../../../utils/orderUtils";

const { Text } = Typography;

interface DeliveryHistoryTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  onPageChange: (page: number, pageSize: number) => void;
  onViewDetail: (record: ShipperOrder) => void;
}

const getStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
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
    case "DELIVERY_RETRY":
    case "DELIVERY_FAILED_FINAL":
    case "FAILED_DELIVERY":
    case "CANCELLED":
      return "error";
    case "RETURNING":
      return "warning";
    case "RETURNED":
      return "gold";
    default:
      return "default";
  }
};

const getStatusText = (status: string) => {
  switch (status.toUpperCase()) {
    case "DELIVERED": return "Đã giao";
    case "DELIVERY_RETRY": return "Chờ giao lại";
    case "AT_DEST_OFFICE": return "Tại bưu cục đích";
    case "DELIVERY_FAILED_FINAL": return "Giao thất bại";
    case "FAILED_DELIVERY": return "Giao thất bại";
    case "RETURNING": return "Đang hoàn trả";
    case "RETURN_AT_ORIGIN_OFFICE": return "Đã hoàn về bưu cục gốc";
    case "RETURN_RETRY": return "Hoàn lại";
    case "RETURN_FAILED_FINAL": return "Hoàn thất bại";
    case "RETURNED": return "Đã hoàn";
    case "PENDING": return "Chờ xử lý";
    case "CONFIRMED": return "Đã xác nhận";
    case "PICKED_UP": return "Đã lấy hàng";
    case "DELIVERING": return "Đang giao";
    case "CANCELLED": return "Đã hủy";
    default: return status;
  }
};

const DeliveryHistoryTable: React.FC<DeliveryHistoryTableProps> = ({
  orders,
  loading,
  pagination,
  onPageChange,
  onViewDetail,
}) => {
  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => (
        <Text strong className="shipper-table-strong" style={{ fontSize: "13px" }}>
          {text}
        </Text>
      ),
    },
    {
      title: "Người nhận",
      key: "recipient",
      width: 200,
      render: (record: ShipperOrder) => (
        <Space direction="vertical" size={0}>
          <Text strong className="shipper-table-strong">
            {record.recipientName}
          </Text>
          <Space size={4}>
            <PhoneOutlined className="shipper-table-muted" style={{ fontSize: "12px" }} />
            <Text className="shipper-table-muted" style={{ fontSize: "12px" }}>
              {record.recipientPhone}
            </Text>
          </Space>
          <Text className="shipper-table-muted" style={{ fontSize: "11px" }} ellipsis>
            {record.recipientFullAddress ||
              (typeof record.recipientAddress === "string"
                ? record.recipientAddress
                : (record.recipientAddress as any)?.fullAddress) ||
              ""}
          </Text>
        </Space>
      ),
    },
    {
      title: "COD",
      dataIndex: "cod",
      key: "cod",
      width: 120,
      render: (amount: number, record: ShipperOrder & any) => (
        <div>
          {amount > 0 ? (
            <Text strong className="shipper-cod-value">
              {amount.toLocaleString()}đ
            </Text>
          ) : (
            <Text className="shipper-table-muted">—</Text>
          )}
          {record.codStatus && (
            <div style={{ marginTop: 6 }}>
              <Tag
                color={
                  record.codStatus === "PENDING"
                    ? "orange"
                    : record.codStatus === "SUBMITTED"
                      ? "blue"
                      : record.codStatus === "RECEIVED" || record.codStatus === "TRANSFERRED"
                        ? "green"
                        : "default"
                }
              >
                {translateOrderCodStatus(record.codStatus)}
              </Tag>
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
      ),
    },
    {
      title: "Ngày giao",
      dataIndex: "displayDate",
      key: "displayDate",
      width: 150,
      render: (_: string, record: ShipperOrder & { historyDate?: string; displayDate?: string; deliveredAt?: string }) => {
        const date = record.historyDate || record.displayDate || record.deliveredAt;
        return date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—";
      },
    },
    {
      title: "Thao tác",
      key: "action",
      width: 100,
      render: (record: ShipperOrder) => (
        <Button icon={<EyeOutlined />} onClick={() => onViewDetail(record)}>
          Chi tiết
        </Button>
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

export default DeliveryHistoryTable;
