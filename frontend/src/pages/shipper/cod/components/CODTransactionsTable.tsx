import React from "react";
import { Checkbox, Table, Tag, Typography } from "antd";
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
  selectedTransactions: number[];
  loading: boolean;
  pagination: { current: number; pageSize: number; total: number };
  onSelectionChange: (selected: number[]) => void;
  onPageChange: (page: number, pageSize: number) => void;
}

const getSubmissionStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING":
    case "IN_BATCH":
      return "orange";
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
      return "Chờ thu";
    case "IN_BATCH":
      return "Đang nộp";
    case "MATCHED":
      return "Khớp";
    case "ADJUSTED":
      return "Đã điều chỉnh";
    case "MISMATCHED":
      return "Không khớp";
    case "SUCCESS":
      return "Đã thu";
    default:
      return status;
  }
};

const CODTransactionsTable: React.FC<CODTransactionsTableProps> = ({
  transactions,
  selectedTransactions,
  loading,
  pagination,
  onSelectionChange,
  onPageChange,
}) => {
  const columns: ColumnsType<PaymentSubmissionItem> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => (
        <Text strong className="shipper-table-strong">
          {text || "-"}
        </Text>
      ),
    },
    {
      title: "Số tiền",
      dataIndex: "actualAmount",
      key: "actualAmount",
      render: (amount: number) => (
        <Text strong className="shipper-cod-value">{(amount || 0).toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Tag color={getSubmissionStatusColor(status)}>{getSubmissionStatusText(status)}</Tag>
      ),
    },
    {
      title: "Ngày thu",
      dataIndex: "paidAt",
      key: "paidAt",
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: PaymentSubmissionItem) => (
        <Checkbox
          checked={selectedTransactions.includes(record.id)}
          onChange={(e) => {
            if (e.target.checked) {
              onSelectionChange([...selectedTransactions, record.id]);
            } else {
              onSelectionChange(selectedTransactions.filter((id) => id !== record.id));
            }
          }}
          disabled={record.status !== 'PENDING'}
        >
          Chọn
        </Checkbox>
      ),
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
      }}
      scroll={{ x: 900 }}
    />
  );
};

export default CODTransactionsTable;
