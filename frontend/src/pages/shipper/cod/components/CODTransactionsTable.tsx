import React from "react";
import { Table, Tag, Typography, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";

const { Text } = Typography;

interface PaymentSubmissionItem {
  id: number;
  code?: string;
  orderId?: number;
  trackingNumber?: string;
  systemAmount: number;
  actualAmount: number;
  discrepancy?: number;
  status: string;
  notes?: string;
  paidAt?: string;
  checkedAt?: string;
}

interface CODTransactionsTableProps {
  transactions: PaymentSubmissionItem[];
  loading: boolean;
  pagination: { current: number; pageSize: number; total: number };
  onPageChange: (page: number, pageSize: number) => void;
}

const getSubmissionStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING":
      return "orange";
    case "IN_BATCH":
      return "cyan";
    case "MATCHED":
      return "success";
    case "ADJUSTED":
      return "blue";
    case "MISMATCHED":
      return "red";
    default:
      return "default";
  }
};

const getSubmissionStatusText = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING":
      return "Chờ nộp";
    case "IN_BATCH":
      return "Đang nộp";
    case "MATCHED":
      return "Khớp";
    case "ADJUSTED":
      return "Đã điều chỉnh";
    case "MISMATCHED":
      return "Không khớp";
    case "PROCESSING":
      return "Đang xử lý";
    default:
      return status;
  }
};

const CODTransactionsTable: React.FC<CODTransactionsTableProps> = ({
  transactions,
  loading,
  pagination,
  onPageChange,
}) => {
  const columns: ColumnsType<PaymentSubmissionItem> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 160,
      render: (text: string, record: PaymentSubmissionItem) => (
        <Tooltip title={record.code || ""}>
          <Text strong className="shipper-table-strong">
            {text || "-"}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: "Số tiền hệ thống",
      dataIndex: "systemAmount",
      key: "systemAmount",
      width: 150,
      align: "right",
      render: (amount: number) => (
        <Text>{(amount || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Số tiền thực thu",
      dataIndex: "actualAmount",
      key: "actualAmount",
      width: 150,
      align: "right",
      render: (amount: number) => (
        <Text strong className="shipper-cod-value">{(amount || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Chênh lệch",
      dataIndex: "discrepancy",
      key: "discrepancy",
      width: 120,
      align: "right",
      render: (val: number) => {
        if (!val || val === 0) return <Text type="secondary">—</Text>;
        return (
          <Text type={val > 0 ? "success" : "danger"}>
            {val > 0 ? "+" : ""}{val.toLocaleString()}đ
          </Text>
        );
      },
    },
    {
      title: "Ghi chú",
      dataIndex: "notes",
      key: "notes",
      ellipsis: true,
      render: (notes: string) => (
        <Tooltip title={notes || ""}>
          <Text type="secondary" style={{ maxWidth: 200 }} ellipsis>
            {notes || "—"}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: "Ngày thu",
      dataIndex: "paidAt",
      key: "paidAt",
      width: 150,
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={transactions}
      pagination={{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        onChange: onPageChange,
        showSizeChanger: true,
        showTotal: (total, range) => `${range[0]}-${range[1]} / ${total} đơn`,
      }}
      scroll={{ x: 900 }}
      size="middle"
    />
  );
};

export default CODTransactionsTable;
