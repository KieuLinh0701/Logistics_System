import React from "react";
import { Button, Table, Tag, Typography } from "antd";
import { EyeOutlined } from "@ant-design/icons";
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

interface CODSubmissionsTableProps {
  submissions: PaymentSubmissionItem[];
  loading: boolean;
  pagination: { current: number; pageSize: number; total: number };
  onPageChange: (page: number, pageSize: number) => void;
  onViewDetail: (record: PaymentSubmissionItem) => void;
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
      return "Chờ xác nhận";
    case "IN_BATCH":
      return "Đang nộp";
    case "MATCHED":
      return "Khớp";
    case "ADJUSTED":
      return "Đã điều chỉnh";
    case "MISMATCHED":
      return "Không khớp";
    default:
      return status;
  }
};

const CODSubmissionsTable: React.FC<CODSubmissionsTableProps> = ({
  submissions,
  loading,
  pagination,
  onPageChange,
  onViewDetail,
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
      title: "Số tiền hệ thống",
      dataIndex: "systemAmount",
      key: "systemAmount",
      render: (amount: number) => <Text className="shipper-table-strong">{amount.toLocaleString()}đ</Text>,
    },
    {
      title: "Số tiền thực nộp",
      dataIndex: "actualAmount",
      key: "actualAmount",
      render: (amount: number) => (
        <Text strong className="shipper-table-strong">{amount.toLocaleString()}đ</Text>
      ),
    },
    {
      title: "Chênh lệch",
      dataIndex: "discrepancy",
      key: "discrepancy",
      render: (discrepancy: number) => (
        <Text className={discrepancy !== 0 ? "shipper-cod-value" : "shipper-amount-ok"}>
          {discrepancy > 0 ? "+" : ""}
          {discrepancy.toLocaleString()}đ
        </Text>
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
      title: "Ngày nộp",
      dataIndex: "paidAt",
      key: "paidAt",
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: PaymentSubmissionItem) => (
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
      dataSource={submissions}
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

export default CODSubmissionsTable;
