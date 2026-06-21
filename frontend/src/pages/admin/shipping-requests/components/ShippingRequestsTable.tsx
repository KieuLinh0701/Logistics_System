import React from "react";
import {Button, Table, Tooltip} from "antd";
import type {ColumnsType} from "antd/es/table";
import type {ShippingRequestRow} from "../../../../types/shippingRequest";

const CONTENT_PREVIEW_MAX_LENGTH = 90;

interface ShippingRequestsTableProps {
  loading: boolean;
  data: ShippingRequestRow[];
  getStatusColor: (status: string) => string;
  getTypeColor: (type: string) => string;
  formatStatus: (status?: string) => string;
  formatType: (type?: string) => string;
  onView: (request: ShippingRequestRow) => void;
  onAssign: (request: ShippingRequestRow) => void;
}

const ShippingRequestsTable: React.FC<ShippingRequestsTableProps> = ({
  loading,
  data,
  formatStatus,
  formatType,
  onView,
  onAssign,
}) => {
  const columns: ColumnsType<ShippingRequestRow> = [
    {
      title: "Mã yêu cầu",
      dataIndex: "code",
      key: "code",
      render: (text: string) => <span>{text}</span>,
    },
    {
      title: "Loại yêu cầu",
      dataIndex: "requestType",
      key: "requestType",
      render: (value: string) => <span>{formatType(value)}</span>,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (value: string) => <span>{formatStatus(value)}</span>,
    },
    {
      title: "Bưu cục",
      key: "office",
      render: (_, row) => row.office?.name || "-",
    },
    {
      title: "Người gửi",
      dataIndex: "userName",
      key: "userName",
      render: (value: string | undefined) => value || "Khách",
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (value: string) => {
        if (!value) return "-";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return (
          <div>
            <div>{date.toLocaleDateString("vi-VN")}</div>
            <div style={{ color: "#6b7280" }}>{date.toLocaleTimeString("vi-VN")}</div>
          </div>
        );
      },
    },
    {
      title: "Nội dung",
      dataIndex: "content",
      key: "content",   
      width: 200,
      ellipsis: true,
      render: (value: string) => {
        if (!value) return "-";
        const normalized = value.trim();
        const preview =
          normalized.length > CONTENT_PREVIEW_MAX_LENGTH
            ? `${normalized.slice(0, CONTENT_PREVIEW_MAX_LENGTH)}...`
            : normalized;

        return (
          <Tooltip title={<div style={{ whiteSpace: "pre-wrap", maxWidth: 520 }}>{normalized}</div>} placement="topLeft">
            <div style={{ maxWidth: 420, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
              {preview}
            </div>
          </Tooltip>
        );
      },
    },
    {
      title: "",
      key: "actions",
      width: 180,
      render: (_, row) => (
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span
            onClick={() => onView(row)}
            style={{ color: "#52c41a", cursor: "pointer", display: "inline-flex", alignItems: "center" }}
            title="Xem"
          >
            Xem
          </span>
          <Button
            size="small"
            onClick={() => onAssign(row)}
            disabled={Boolean(row.office?.id)}
          >
            Phân công
          </Button>
        </div>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      dataSource={data}
      columns={columns}
      pagination={{ pageSize: 10, showSizeChanger: false }}
      scroll={{ x: 1200 }}
    />
  );
};

export default ShippingRequestsTable;
